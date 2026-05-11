package com.ecobook.service;

import com.ecobook.dto.GeminiResponseDTO;
import com.ecobook.dto.PredictionFieldDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiServiceTest {

    private final GeminiService geminiService = new GeminiService(new ObjectMapper());

    @Test
    @DisplayName("classifyMaterial should return deterministic local suggestions when mock mode is enabled")
    void shouldReturnMockPreviewWhenEnabled() {
        ReflectionTestUtils.setField(geminiService, "mockEnabled", true);

        GeminiResponseDTO response = geminiService.classifyMaterial(
                "matematica-7-ano.png",
                new byte[]{1, 2, 3},
                "image/png"
        );

        assertThat(response.getStatusIa()).isEqualTo("LOW_CONFIDENCE");
        assertThat(response.getBestPrediction())
                .containsEntry("disciplina", PredictionFieldDTO.builder().value("MATEMATICA").confidence(0.66).build());
        assertThat(response.getErrorDetails().getMessage()).contains("Modo local de preview ativo");
    }

    @Test
    @DisplayName("classifyMaterial should fail gracefully when the API key is absent outside mock mode")
    void shouldFallbackWhenApiKeyIsMissing() {
        ReflectionTestUtils.setField(geminiService, "mockEnabled", false);
        ReflectionTestUtils.setField(geminiService, "apiKey", "");

        GeminiResponseDTO response = geminiService.classifyMaterial(
                "matematica-7-ano.png",
                new byte[]{1, 2, 3},
                "image/png"
        );

        assertThat(response.getStatusIa()).isEqualTo("FAILURE");
        assertThat(response.getBestPrediction()).isEmpty();
        assertThat(response.getErrorDetails().getMessage()).contains("Gemini");
    }

    @Test
    @DisplayName("parseGeminiResponse should keep valid predictions and downgrade invalid enums")
    void shouldParseAndNormalizeGeminiPayload() {
        String payload = """
                {
                  "best_prediction": {
                    "titulo": { "value": "Geometria Plana 7 Ano", "confidence": 0.92 },
                    "disciplina": { "value": "MUSICA", "confidence": 0.91 },
                    "nivel_ensino": { "value": "fundamental", "confidence": 0.89 },
                    "ano": { "value": 7, "confidence": 0.87 },
                    "sistema_ensino": { "value": "Anglo", "confidence": 0.88 },
                    "estado_conservacao": { "value": "Bom", "confidence": 0.84 },
                    "data_publicacao": { "value": 2021, "confidence": 0.79 }
                  }
                }
                """;

        GeminiResponseDTO response = geminiService.parseGeminiResponse(payload);

        assertThat(response.getStatusIa()).isEqualTo("LOW_CONFIDENCE");
        assertThat(response.getBestPrediction().get("nivel_ensino").getValue()).isEqualTo("FUNDAMENTAL");
        assertThat(response.getBestPrediction().get("sistema_ensino").getValue()).isEqualTo("ANGLO");
        assertThat(response.getBestPrediction().get("disciplina").getValue()).isNull();
        assertThat(response.getErrorDetails().getInvalidEnums()).containsExactly("disciplina");
    }

    @Test
    @DisplayName("parseGeminiResponse should return FAILURE when the model payload is malformed")
    void shouldReturnFailureForMalformedJson() {
        GeminiResponseDTO response = geminiService.parseGeminiResponse("not-json");

        assertThat(response.getStatusIa()).isEqualTo("FAILURE");
        assertThat(response.getErrorDetails().isMalformedResponse()).isTrue();
        assertThat(response.getBestPrediction()).isEmpty();
    }

    @Test
    @DisplayName("classifyMaterial should return FAILURE with timeout details when Gemini keeps timing out")
    void shouldReturnFailureForTimeouts() {
        GeminiService timeoutService = new GeminiService(new ObjectMapper()) {
            @Override
            OkHttpClient createHttpClient() {
                Interceptor timeoutInterceptor = chain -> {
                    throw new SocketTimeoutException("simulated timeout");
                };
                return new OkHttpClient.Builder()
                        .addInterceptor(timeoutInterceptor)
                        .connectTimeout(Duration.ofMillis(100))
                        .readTimeout(Duration.ofMillis(100))
                        .writeTimeout(Duration.ofMillis(100))
                        .build();
            }

            @Override
            void sleepQuietly(Duration duration) {
                // Skip retry backoff in the test so timeout handling remains deterministic and fast.
            }
        };

        ReflectionTestUtils.setField(timeoutService, "mockEnabled", false);
        ReflectionTestUtils.setField(timeoutService, "apiKey", "test-key");
        ReflectionTestUtils.setField(timeoutService, "model", "gemini-2.5-flash");

        GeminiResponseDTO response = timeoutService.classifyMaterial(
                "matematica-7-ano.png",
                new byte[]{1, 2, 3},
                "image/png"
        );

        assertThat(response.getStatusIa()).isEqualTo("FAILURE");
        assertThat(response.getErrorDetails().isTimeout()).isTrue();
        assertThat(response.getErrorDetails().isMalformedResponse()).isFalse();
        assertThat(response.getErrorDetails().getMessage()).contains("Timeout do Gemini");
    }

    @Test
    @DisplayName("determineFallbackStatus should classify complete high-confidence predictions as SUCCESS")
    void shouldDetermineSuccessStatus() {
        Map<String, PredictionFieldDTO> predictions = Map.of(
                "titulo", PredictionFieldDTO.builder().value("Livro").confidence(0.93).build(),
                "disciplina", PredictionFieldDTO.builder().value("MATEMATICA").confidence(0.94).build(),
                "nivel_ensino", PredictionFieldDTO.builder().value("FUNDAMENTAL").confidence(0.88).build(),
                "ano", PredictionFieldDTO.builder().value(7).confidence(0.85).build()
        );

        assertThat(geminiService.determineFallbackStatus(predictions).name()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("buildRequestBody should skip google_search for Gemini 2.5 JSON-mode requests")
    @SuppressWarnings("unchecked")
    void shouldSkipGoogleSearchToolForGemini25() {
        ReflectionTestUtils.setField(geminiService, "googleSearchEnabled", true);
        ReflectionTestUtils.setField(geminiService, "model", "gemini-2.5-flash");

        boolean includeGoogleSearchTool = (boolean) ReflectionTestUtils.invokeMethod(geminiService, "shouldUseGoogleSearchTool");
        Map<String, Object> requestBody = (Map<String, Object>) ReflectionTestUtils.invokeMethod(
                geminiService,
                "buildRequestBody",
                new byte[]{1, 2, 3},
                "image/png",
                includeGoogleSearchTool
        );

        assertThat(includeGoogleSearchTool).isFalse();
        assertThat(requestBody).doesNotContainKey("tools");

        List<Map<String, Object>> contents = (List<Map<String, Object>>) requestBody.get("contents");
        List<Map<String, Object>> parts = (List<Map<String, Object>>) contents.get(0).get("parts");
        String prompt = (String) parts.get(0).get("text");
        assertThat(prompt).doesNotContain("Google Search grounding");
    }

    @Test
    @DisplayName("buildRequestBody should keep google_search available for Gemini 3 models")
    @SuppressWarnings("unchecked")
    void shouldKeepGoogleSearchToolForGemini3() {
        ReflectionTestUtils.setField(geminiService, "googleSearchEnabled", true);
        ReflectionTestUtils.setField(geminiService, "model", "gemini-3-flash-preview");

        boolean includeGoogleSearchTool = (boolean) ReflectionTestUtils.invokeMethod(geminiService, "shouldUseGoogleSearchTool");
        Map<String, Object> requestBody = (Map<String, Object>) ReflectionTestUtils.invokeMethod(
                geminiService,
                "buildRequestBody",
                new byte[]{1, 2, 3},
                "image/png",
                includeGoogleSearchTool
        );

        assertThat(includeGoogleSearchTool).isTrue();
        assertThat(requestBody).containsKey("tools");

        List<Map<String, Object>> tools = (List<Map<String, Object>>) requestBody.get("tools");
        assertThat(tools).containsExactly(Map.of("google_search", Map.of()));
    }

    @Test
    @DisplayName("buildGeminiHttpErrorMessage should expose the API message when Gemini rejects the request")
    void shouldExposeGeminiApiErrorMessage() {
        String payload = """
                {
                  "error": {
                    "code": 400,
                    "message": "Tool use with a response mime type: 'application/json' is unsupported",
                    "status": "INVALID_ARGUMENT"
                  }
                }
                """;

        String message = ReflectionTestUtils.invokeMethod(geminiService, "buildGeminiHttpErrorMessage", 400, payload);

        assertThat(message).isEqualTo(
                "Gemini retornou erro HTTP 400: Tool use with a response mime type: 'application/json' is unsupported"
        );
    }
}
