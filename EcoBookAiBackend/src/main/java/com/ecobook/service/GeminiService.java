package com.ecobook.service;

import com.ecobook.dto.GeminiResponseDTO;
import com.ecobook.dto.PredictionFieldDTO;
import com.ecobook.dto.PreviewErrorDetailsDTO;
import com.ecobook.model.enums.StatusIA;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    private static final Pattern SCHOOL_YEAR_PATTERN = Pattern.compile("\\d{1,2}");
    private static final Pattern PUBLICATION_YEAR_PATTERN = Pattern.compile("\\d{4}");

    private static final List<String> EXPECTED_FIELDS = List.of(
            "titulo",
            "autor",
            "editora",
            "disciplina",
            "nivel_ensino",
            "ano",
            "sistema_ensino",
            "data_publicacao"
    );

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final int[] RATE_LIMIT_BACKOFF_SECONDS = {1, 2, 4};
    private static final int[] SERVER_ERROR_BACKOFF_SECONDS = {2, 4, 8};
    private static final int[] TIMEOUT_BACKOFF_SECONDS = {1, 1};
    private static final int CIRCUIT_BREAKER_FAILURE_THRESHOLD = 10;
    private static final Duration CIRCUIT_BREAKER_WINDOW = Duration.ofMinutes(5);
    private static final Duration CIRCUIT_BREAKER_OPEN_DURATION = Duration.ofSeconds(30);

    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String model;

    @Value("${gemini.max-retries:3}")
    private int maxRetries;

    @Value("${gemini.timeout-seconds:10}")
    private int timeoutSeconds;

    @Value("${gemini.mock-enabled:false}")
    private boolean mockEnabled;

    @Value("${gemini.mock-force:false}")
    private boolean mockForce;

    @Value("${gemini.google-search-enabled:true}")
    private boolean googleSearchEnabled;

    private final Object circuitBreakerMonitor = new Object();
    private final Deque<Instant> recentFailures = new ArrayDeque<>();
    private Instant circuitOpenUntil;

    /**
     * Classifies an uploaded material image with the Gemini integration.
     * @param originalFilename original file name received for the upload
     * @param imageBytes image bytes to validate or classify
     * @param mimeType MIME type associated with the upload
     * @return classification response
     */
    public GeminiResponseDTO classifyMaterial(String originalFilename, byte[] imageBytes, String mimeType) {
        return classifyMaterial(originalFilename, imageBytes, mimeType, null, null);
    }

    public GeminiResponseDTO classifyMaterial(String originalFilename,
                                              byte[] imageBytes,
                                              String mimeType,
                                              byte[] secondaryImageBytes,
                                              String secondaryMimeType) {
        if (shouldUseMockMode()) {
            return mockResponse(originalFilename);
        }

        if (!StringUtils.hasText(apiKey)) {
            log.warn("Gemini API key is not configured in the backend process; returning manual fallback");
            return failureResponse(
                    "Integração Gemini indisponível: GEMINI_API_KEY ausente no processo do backend",
                    false,
                    false,
                    List.of(),
                    List.of()
            );
        }

        List<GeminiImageInput> images = new ArrayList<>();
        images.add(new GeminiImageInput(imageBytes, mimeType));
        if (secondaryImageBytes != null
                && secondaryImageBytes.length > 0
                && StringUtils.hasText(secondaryMimeType)) {
            images.add(new GeminiImageInput(secondaryImageBytes, secondaryMimeType));
        }

        return callGeminiWithRetry(images);
    }

    private boolean shouldUseMockMode() {
        if (!mockEnabled) {
            return false;
        }

        if (mockForce) {
            log.info("Gemini mock mode is being forced even though a live API key may be present.");
            return true;
        }

        if (!StringUtils.hasText(apiKey)) {
            return true;
        }

        log.info("Gemini mock mode was configured, but a GEMINI_API_KEY is present; using the live Gemini integration instead.");
        return false;
    }

    GeminiResponseDTO parseGeminiResponse(String rawGeminiText) {
        try {
            String cleaned = stripCodeFence(rawGeminiText);
            JsonNode root = objectMapper.readTree(cleaned);
            JsonNode predictionNode = root.has("best_prediction") ? root.path("best_prediction") : root;

            Map<String, PredictionFieldDTO> predictions = new LinkedHashMap<>();
            List<String> missingFields = new ArrayList<>();
            List<String> invalidEnums = new ArrayList<>();

            for (String field : EXPECTED_FIELDS) {
                JsonNode fieldNode = predictionNode.path(field);
                if (fieldNode.isMissingNode() || fieldNode.isNull()) {
                    missingFields.add(field);
                    continue;
                }

                Object value = parseFieldValue(field, fieldNode.path("value"));
                Double confidence = parseConfidence(fieldNode.path("confidence"));
                if (value == null) {
                    missingFields.add(field);
                    confidence = null;
                }

                if (isEnumField(field) && value != null) {
                    String normalized = normalizeEnumValue(value.toString());
                    if (!allowedValues(field).contains(normalized)) {
                        invalidEnums.add(field);
                        value = null;
                        confidence = null;
                    } else {
                        value = normalized;
                    }
                }

                if (value != null || confidence != null) {
                    predictions.put(field, PredictionFieldDTO.builder()
                            .value(value)
                            .confidence(confidence)
                            .build());
                }
            }

            StatusIA statusIa = determineFallbackStatus(predictions);
            if (statusIa == StatusIA.SUCCESS && (!missingFields.isEmpty() || !invalidEnums.isEmpty())) {
                statusIa = StatusIA.LOW_CONFIDENCE;
            }

            return GeminiResponseDTO.builder()
                    .statusIa(statusIa.name())
                    .bestPrediction(statusIa == StatusIA.FAILURE ? new LinkedHashMap<>() : predictions)
                    .errorDetails(PreviewErrorDetailsDTO.builder()
                            .timeout(false)
                            .malformedResponse(false)
                            .missingFields(new ArrayList<>(new LinkedHashSet<>(missingFields)))
                            .invalidEnums(invalidEnums)
                            .build())
                    .build();
        } catch (JsonProcessingException ex) {
            log.warn("Gemini returned malformed JSON: {}", rawGeminiText, ex);
            return failureResponse("Resposta do Gemini em formato inválido", false, true, List.of(), List.of());
        }
    }

    StatusIA determineFallbackStatus(Map<String, PredictionFieldDTO> predictions) {
        if (predictions.isEmpty()) {
            return StatusIA.FAILURE;
        }

        boolean allHighConfidence = predictions.values().stream()
                .map(PredictionFieldDTO::getConfidence)
                .filter(java.util.Objects::nonNull)
                .allMatch(confidence -> confidence >= 0.75d);

        long populatedFields = predictions.values().stream()
                .filter(prediction -> prediction.getValue() != null)
                .count();

        if (allHighConfidence && populatedFields >= 4) {
            return StatusIA.SUCCESS;
        }

        return populatedFields > 0 ? StatusIA.LOW_CONFIDENCE : StatusIA.FAILURE;
    }

    private GeminiResponseDTO callGeminiWithRetry(List<GeminiImageInput> images) {
        if (isCircuitOpen()) {
            log.warn("Gemini circuit breaker is OPEN; returning manual fallback without external call");
            return failureResponse(
                    "Gemini temporariamente pausado por excesso de falhas recentes; siga com preenchimento manual.",
                    false,
                    false,
                    List.of(),
                    List.of()
            );
        }

        int retryCount = 0;

        while (true) {
            try {
                GeminiResponseDTO response = doGeminiCall(images);
                recordOutcome(response);
                return response;
            } catch (RetryableGeminiException ex) {
                int[] backoffSchedule = backoffScheduleFor(ex.category());
                int maxAllowedRetries = maxRetriesFor(ex.category(), backoffSchedule);
                if (retryCount >= maxAllowedRetries) {
                    recordFailure();
                    log.warn("Gemini retries exhausted after {} attempts for category {}", retryCount, ex.category(), ex);
                    return failureResponse(ex.getMessage(), ex.timeout(), false, List.of(), List.of());
                }
                sleepQuietly(Duration.ofSeconds(backoffSchedule[retryCount]));
                retryCount++;
            } catch (IOException ex) {
                recordFailure();
                log.warn("Gemini call failed, returning manual fallback", ex);
                return failureResponse("Falha ao consultar o Gemini", false, false, List.of(), List.of());
            }
        }
    }

    private GeminiResponseDTO doGeminiCall(List<GeminiImageInput> images) throws IOException {
        boolean includeGoogleSearchTool = shouldUseGoogleSearchTool();
        return executeGeminiCall(images, includeGoogleSearchTool);
    }

    private GeminiResponseDTO executeGeminiCall(List<GeminiImageInput> images,
                                                boolean includeGoogleSearchTool) throws IOException {
        OkHttpClient client = createHttpClient();

        String body = objectMapper.writeValueAsString(buildRequestBody(images, includeGoogleSearchTool));
        Request request = new Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent")
                .addHeader("x-goog-api-key", apiKey)
                .post(RequestBody.create(body, JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 429) {
                throw new RetryableGeminiException(
                        RetryCategory.RATE_LIMIT,
                        "Gemini atingiu limite temporário de requisições",
                        false,
                        null
                );
            }
            if (response.code() >= 500) {
                throw new RetryableGeminiException(
                        RetryCategory.SERVER_ERROR,
                        "Gemini retornou erro temporario do servidor",
                        false,
                        null
                );
            }
            if (!response.isSuccessful()) {
                String errorPayload = response.body() != null ? response.body().string() : "";
                if (response.code() == 400
                        && includeGoogleSearchTool
                        && isStructuredOutputToolConflict(errorPayload)) {
                    log.warn("Gemini rejeitou o uso de google_search com JSON mode no modelo {}; tentando novamente sem grounding", model);
                    return executeGeminiCall(images, false);
                }

                return failureResponse(
                        buildGeminiHttpErrorMessage(response.code(), errorPayload),
                        false,
                        false,
                        List.of(),
                        List.of()
                );
            }

            String payload = response.body() != null ? response.body().string() : "";
            JsonNode root;
            try {
                root = objectMapper.readTree(payload);
            } catch (JsonProcessingException ex) {
                log.warn("Gemini returned malformed payload: {}", payload, ex);
                return failureResponse("Gemini retornou payload inválido", false, true, List.of(), List.of());
            }
            String rawText = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
            if (!StringUtils.hasText(rawText)) {
                return failureResponse("Gemini não retornou conteúdo utilizável", false, true, List.of(), List.of());
            }

            return parseGeminiResponse(rawText);
        } catch (java.net.SocketTimeoutException ex) {
            throw new RetryableGeminiException(
                    RetryCategory.TIMEOUT_OR_CONNECTION,
                    "Timeout do Gemini após 10s",
                    true,
                    ex
            );
        } catch (IOException ex) {
            throw new RetryableGeminiException(
                    RetryCategory.TIMEOUT_OR_CONNECTION,
                    "Falha temporária de conexão com o Gemini",
                    false,
                    ex
            );
        }
    }

    OkHttpClient createHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .readTimeout(Duration.ofSeconds(timeoutSeconds))
                .writeTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    private Map<String, Object> buildRequestBody(byte[] imageBytes,
                                                 String mimeType,
                                                 boolean includeGoogleSearchTool) {
        return buildRequestBody(List.of(new GeminiImageInput(imageBytes, mimeType)), includeGoogleSearchTool);
    }

    Map<String, Object> buildRequestBody(List<GeminiImageInput> images,
                                         boolean includeGoogleSearchTool) {
        Map<String, Object> request = new LinkedHashMap<>();
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", prompt(includeGoogleSearchTool)));
        for (GeminiImageInput image : images) {
            parts.add(Map.of(
                    "inlineData", Map.of(
                            "mimeType", image.mimeType(),
                            "data", Base64.getEncoder().encodeToString(image.imageBytes())
                    )
            ));
        }
        request.put("contents", List.of(Map.of("parts", parts)));
        if (includeGoogleSearchTool) {
            request.put("tools", List.of(Map.of("google_search", Map.of())));
        }
        request.put("generationConfig", Map.of(
                "temperature", 0.2,
                "responseMimeType", "application/json"
        ));
        return request;
    }

    private String prompt(boolean includeGoogleSearchTool) {
        List<String> lines = new ArrayList<>(List.of(
                "Analise as imagens de um material educacional brasileiro.",
                "Considere todas as imagens enviadas em conjunto; frente e verso podem trazer informações complementares.",
                "Retorne sua resposta SOMENTE em formato JSON válido com a chave best_prediction.",
                "Cada campo deve seguir o formato {\"value\": \"...\", \"confidence\": 0.0}.",
                "Campos aceitos: titulo, autor, editora, disciplina, nivel_ensino, ano, sistema_ensino, data_publicacao.",
                "Valores de disciplina: TODAS, MATEMATICA, PORTUGUES, HISTORIA, GEOGRAFIA, CIENCIAS, LITERATURA.",
                "Use disciplina TODAS quando o material reunir múltiplas matérias, for uma coleção multidisciplinar ou as imagens indicarem mais de uma disciplina no mesmo volume.",
                "Valores de nivel_ensino: FUNDAMENTAL, MEDIO, SUPERIOR.",
                "Valores de sistema_ensino: ANGLO, OBJETIVO, COC, POSITIVO, POLIEDRO, ETAPA, BERNOULLI, SAS, FTD, OUTRO.",
                "Para campos não identificados, use value null e confidence null.",
                "Nunca invente descrição. Nunca retorne texto fora do JSON.",
                "Autor e editora devem ser extraídos da capa, lombada, contracapa ou outras partes visíveis das imagens sempre que possível.",
                "Só preencha autor ou editora quando houver evidência forte; se houver dúvida, use value null e confidence null.",
                "Não retorne estado_conservacao. Esse campo é sempre preenchido manualmente pelo usuário.",
                "Seja conservador nas predições e priorize a precisão da informação."
        ));

        if (includeGoogleSearchTool) {
            lines.add(12, "Se autor ou editora não estiverem claramente visíveis, você pode usar Google Search grounding para tentar confirmar essas informações com base no título e no contexto visual do livro.");
        }

        return String.join("\n", lines);
    }

    private boolean shouldUseGoogleSearchTool() {
        if (!googleSearchEnabled) {
            return false;
        }

        if (supportsStructuredOutputsWithTools(model)) {
            return true;
        }

        log.info("Google Search grounding desativado para o modelo {} porque esta integracao exige JSON mode e essa combinacao so e habilitada aqui para modelos Gemini 3", model);
        return false;
    }

    private boolean supportsStructuredOutputsWithTools(String modelName) {
        if (!StringUtils.hasText(modelName)) {
            return false;
        }

        return modelName.trim().toLowerCase(Locale.ROOT).startsWith("gemini-3");
    }

    private boolean isStructuredOutputToolConflict(String payload) {
        String errorMessage = extractGeminiErrorMessage(payload);
        return errorMessage.toLowerCase(Locale.ROOT).contains("tool use with a response mime type");
    }

    private String buildGeminiHttpErrorMessage(int statusCode, String payload) {
        String apiMessage = extractGeminiErrorMessage(payload);
        if (!StringUtils.hasText(apiMessage)) {
            return "Gemini retornou erro HTTP " + statusCode;
        }

        return "Gemini retornou erro HTTP " + statusCode + ": " + apiMessage;
    }

    private String extractGeminiErrorMessage(String payload) {
        if (!StringUtils.hasText(payload)) {
            return "";
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            return root.path("error").path("message").asText("");
        } catch (JsonProcessingException ex) {
            log.debug("Não foi possível extrair a mensagem de erro do Gemini a partir do payload: {}", payload, ex);
            return "";
        }
    }

    private Object parseFieldValue(String field, JsonNode valueNode) {
        if (valueNode.isMissingNode() || valueNode.isNull()) {
            return null;
        }

        if ("ano".equals(field) || "data_publicacao".equals(field)) {
            return parseNumericField(field, valueNode);
        }

        return valueNode.asText(null);
    }

    private Integer parseNumericField(String field, JsonNode valueNode) {
        if (valueNode.isInt() || valueNode.isLong()) {
            return valueNode.asInt();
        }

        if (!valueNode.isTextual()) {
            return null;
        }

        String rawValue = valueNode.asText().trim();
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }

        Pattern pattern = "data_publicacao".equals(field) ? PUBLICATION_YEAR_PATTERN : SCHOOL_YEAR_PATTERN;
        Matcher matcher = pattern.matcher(rawValue);
        if (!matcher.find()) {
            return null;
        }

        try {
            return Integer.parseInt(matcher.group());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Double parseConfidence(JsonNode confidenceNode) {
        if (confidenceNode.isMissingNode() || confidenceNode.isNull()) {
            return null;
        }
        if (!confidenceNode.isNumber()) {
            return null;
        }

        double value = confidenceNode.asDouble();
        if (value < 0d || value > 1d) {
            return null;
        }
        return value;
    }

    private boolean isEnumField(String field) {
        return List.of("disciplina", "nivel_ensino", "sistema_ensino").contains(field);
    }

    private List<String> allowedValues(String field) {
        return switch (field) {
            case "disciplina" -> List.of("TODAS", "MATEMATICA", "PORTUGUES", "HISTORIA", "GEOGRAFIA", "CIENCIAS", "LITERATURA");
            case "nivel_ensino" -> List.of("FUNDAMENTAL", "MEDIO", "SUPERIOR");
            case "sistema_ensino" -> List.of("ANGLO", "OBJETIVO", "COC", "POSITIVO", "POLIEDRO", "ETAPA", "BERNOULLI", "SAS", "FTD", "OUTRO");
            default -> List.of();
        };
    }

    private String normalizeEnumValue(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .replaceAll("[^A-Za-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "")
                .toUpperCase(Locale.ROOT);
    }

    private String stripCodeFence(String rawText) {
        return rawText
                .replace("```json", "")
                .replace("```", "")
                .trim();
    }

    private GeminiResponseDTO mockResponse(String originalFilename) {
        String inferredTitle = inferTitle(originalFilename);
        Map<String, PredictionFieldDTO> predictions = new LinkedHashMap<>();
        predictions.put("titulo", PredictionFieldDTO.builder().value(inferredTitle).confidence(0.68).build());
        predictions.put("autor", PredictionFieldDTO.builder().value("Autor de Exemplo").confidence(0.57).build());
        predictions.put("editora", PredictionFieldDTO.builder().value("Editora Exemplo").confidence(0.54).build());
        predictions.put("disciplina", PredictionFieldDTO.builder().value("MATEMATICA").confidence(0.66).build());
        predictions.put("nivel_ensino", PredictionFieldDTO.builder().value("FUNDAMENTAL").confidence(0.61).build());
        predictions.put("ano", PredictionFieldDTO.builder().value(7).confidence(0.58).build());
        predictions.put("sistema_ensino", PredictionFieldDTO.builder().value("OUTRO").confidence(0.55).build());
        predictions.put("data_publicacao", PredictionFieldDTO.builder().value(2020).confidence(0.51).build());

        return GeminiResponseDTO.builder()
                .statusIa(StatusIA.LOW_CONFIDENCE.name())
                .bestPrediction(predictions)
                .errorDetails(PreviewErrorDetailsDTO.builder()
                        .message("Modo local de preview ativo; revise os campos antes de publicar.")
                        .missingFields(List.of())
                        .invalidEnums(List.of())
                        .build())
                .build();
    }

    private String inferTitle(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return "Material para revisão manual";
        }

        String baseName = originalFilename.contains(".")
                ? originalFilename.substring(0, originalFilename.lastIndexOf('.'))
                : originalFilename;
        String normalized = baseName.replace('_', ' ').replace('-', ' ').trim();
        return normalized.isBlank() ? "Material para revisão manual" : normalized;
    }

    /**
     * Executes the failure response operation.
     *
     * @param message the message value
     * @param timeout the timeout value
     * @param malformedResponse the malformedResponse value
     * @param missingFields the missingFields value
     * @param invalidEnums the invalidEnums value
     * @return the operation result
     */
    public GeminiResponseDTO failureResponse(String message,
                                             boolean timeout,
                                             boolean malformedResponse,
                                             List<String> missingFields,
                                             List<String> invalidEnums) {
        return GeminiResponseDTO.builder()
                .statusIa(StatusIA.FAILURE.name())
                .bestPrediction(new LinkedHashMap<>())
                .errorDetails(PreviewErrorDetailsDTO.builder()
                        .timeout(timeout)
                        .malformedResponse(malformedResponse)
                        .missingFields(new ArrayList<>(missingFields))
                        .invalidEnums(new ArrayList<>(invalidEnums))
                        .message(message)
                        .build())
                .build();
    }

    void sleepQuietly(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private int[] backoffScheduleFor(RetryCategory category) {
        return switch (category) {
            case RATE_LIMIT -> RATE_LIMIT_BACKOFF_SECONDS;
            case SERVER_ERROR -> SERVER_ERROR_BACKOFF_SECONDS;
            case TIMEOUT_OR_CONNECTION -> TIMEOUT_BACKOFF_SECONDS;
        };
    }

    private int maxRetriesFor(RetryCategory category, int[] backoffSchedule) {
        return switch (category) {
            case RATE_LIMIT, SERVER_ERROR -> Math.min(maxRetries, backoffSchedule.length);
            case TIMEOUT_OR_CONNECTION -> backoffSchedule.length;
        };
    }

    private void recordOutcome(GeminiResponseDTO response) {
        if (response == null) {
            recordFailure();
            return;
        }

        if (StatusIA.FAILURE.name().equals(response.getStatusIa())) {
            recordFailure();
        } else {
            resetCircuitBreaker();
        }
    }

    private boolean isCircuitOpen() {
        synchronized (circuitBreakerMonitor) {
            Instant now = Instant.now();
            trimFailures(now);

            if (circuitOpenUntil == null) {
                return false;
            }
            if (circuitOpenUntil.isAfter(now)) {
                return true;
            }

            circuitOpenUntil = null;
            return false;
        }
    }

    private void recordFailure() {
        synchronized (circuitBreakerMonitor) {
            Instant now = Instant.now();
            trimFailures(now);
            recentFailures.addLast(now);
            if (recentFailures.size() > CIRCUIT_BREAKER_FAILURE_THRESHOLD) {
                circuitOpenUntil = now.plus(CIRCUIT_BREAKER_OPEN_DURATION);
            }
        }
    }

    private void resetCircuitBreaker() {
        synchronized (circuitBreakerMonitor) {
            recentFailures.clear();
            circuitOpenUntil = null;
        }
    }

    private void trimFailures(Instant now) {
        Instant threshold = now.minus(CIRCUIT_BREAKER_WINDOW);
        while (!recentFailures.isEmpty() && recentFailures.peekFirst().isBefore(threshold)) {
            recentFailures.removeFirst();
        }
    }

    record GeminiImageInput(byte[] imageBytes, String mimeType) {
    }

    private enum RetryCategory {
        RATE_LIMIT,
        SERVER_ERROR,
        TIMEOUT_OR_CONNECTION
    }

    private static final class RetryableGeminiException extends IOException {
        private final RetryCategory category;
        private final boolean timeout;

        private RetryableGeminiException(RetryCategory category, String message, boolean timeout, Throwable cause) {
            super(message, cause);
            this.category = category;
            this.timeout = timeout;
        }

        private RetryCategory category() {
            return category;
        }

        private boolean timeout() {
            return timeout;
        }
    }
}
