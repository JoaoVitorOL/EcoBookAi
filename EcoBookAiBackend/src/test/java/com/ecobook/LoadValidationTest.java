package com.ecobook;

import com.ecobook.config.TestDatabaseConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LoadValidationTest {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final MediaType PNG = MediaType.get("image/png");
    private static final Duration BARRIER_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration SAMPLER_INTERVAL = Duration.ofMillis(50);
    private static final DateTimeFormatter FILE_SUFFIX_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withLocale(Locale.ROOT)
            .withZone(java.time.ZoneOffset.UTC);

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(10))
            .readTimeout(Duration.ofSeconds(30))
            .writeTimeout(Duration.ofSeconds(30))
            .callTimeout(Duration.ofSeconds(45))
            .build();

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        TestDatabaseConfig.registerProperties(registry);
    }

    @Test
    @EnabledIfSystemProperty(named = "ecobook.runLoadTest", matches = "true")
    @DisplayName("T217 should sustain concurrent uploads and discovery traffic within the Phase 9 SLOs")
    void shouldSustainConcurrentUploadsAndSearches() throws Exception {
        int concurrentUploads = Integer.getInteger("ecobook.load.uploadUsers", 20);
        int concurrentSearches = Integer.getInteger("ecobook.load.searchUsers", 30);
        long searchP95BudgetMs = Long.getLong("ecobook.load.searchP95BudgetMs", 2_000L);
        double errorRateBudget = Double.parseDouble(System.getProperty("ecobook.load.errorRateBudget", "0.01"));

        String baseUrl = "http://127.0.0.1:" + port + "/api";
        byte[] pngBytes = validPng();

        RegisteredUser observer = registerCompleteUser(baseUrl, "load-observer@example.com", "Load Observer");
        seedDiscoveryCatalog(baseUrl, pngBytes);

        List<RegisteredUser> uploadUsers = new ArrayList<>();
        for (int index = 0; index < concurrentUploads; index++) {
            uploadUsers.add(registerCompleteUser(
                    baseUrl,
                    "upload-user-" + index + "@example.com",
                    "Upload User " + index
            ));
        }

        List<RegisteredUser> searchUsers = new ArrayList<>();
        for (int index = 0; index < concurrentSearches; index++) {
            searchUsers.add(registerCompleteUser(
                    baseUrl,
                    "search-user-" + index + "@example.com",
                    "Search User " + index
            ));
        }

        AtomicReference<String> latestPrometheusSnapshot = new AtomicReference<>("");
        List<MetricsSample> metricsSamples = java.util.Collections.synchronizedList(new ArrayList<>());

        ExecutorService executor = Executors.newFixedThreadPool(concurrentUploads + concurrentSearches);
        CountDownLatch readyLatch = new CountDownLatch(concurrentUploads + concurrentSearches);
        CountDownLatch startLatch = new CountDownLatch(1);

        try {
            List<CompletableFuture<OperationResult>> futures = new ArrayList<>();

            for (int index = 0; index < uploadUsers.size(); index++) {
                RegisteredUser uploadUser = uploadUsers.get(index);
                int userIndex = index;
                futures.add(CompletableFuture.supplyAsync(() -> runConcurrentOperation(readyLatch, startLatch, () -> {
                    long startedAt = System.nanoTime();
                    try {
                        String uploadId = previewMaterial(baseUrl, uploadUser.token(), pngBytes, "load-upload-" + userIndex + ".png");
                        createMaterial(baseUrl, uploadUser.token(), uploadId, "Colecao carga upload " + userIndex, 2020 + (userIndex % 5));
                        return OperationResult.success(
                                "upload",
                                elapsedMillis(startedAt),
                                201,
                                "upload_id=" + uploadId
                        );
                    } catch (HttpStatusException exception) {
                        return OperationResult.failure(
                                "upload",
                                elapsedMillis(startedAt),
                                exception.statusCode(),
                                exception.getMessage()
                        );
                    } catch (Exception exception) {
                        return OperationResult.failure(
                                "upload",
                                elapsedMillis(startedAt),
                                0,
                                exception.getMessage()
                        );
                    }
                }), executor));
            }

            for (int index = 0; index < searchUsers.size(); index++) {
                RegisteredUser searchUser = searchUsers.get(index);
                futures.add(CompletableFuture.supplyAsync(() -> runConcurrentOperation(readyLatch, startLatch, () -> {
                    long startedAt = System.nanoTime();
                    try {
                        JsonNode response = searchMaterials(baseUrl, searchUser.token(), "Colecao");
                        int resultCount = response.path("data").path("results").size();
                        return OperationResult.success(
                                "search",
                                elapsedMillis(startedAt),
                                200,
                                "results=" + resultCount
                        );
                    } catch (HttpStatusException exception) {
                        return OperationResult.failure(
                                "search",
                                elapsedMillis(startedAt),
                                exception.statusCode(),
                                exception.getMessage()
                        );
                    } catch (Exception exception) {
                        return OperationResult.failure(
                                "search",
                                elapsedMillis(startedAt),
                                0,
                                exception.getMessage()
                        );
                    }
                }), executor));
            }

            boolean allReady = readyLatch.await(BARRIER_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            assertThat(allReady)
                    .as("all concurrent load workers must be ready before the start barrier opens")
                    .isTrue();

            capturePrometheusSample(baseUrl, observer.token(), latestPrometheusSnapshot, metricsSamples);
            startLatch.countDown();

            while (futures.stream().anyMatch(future -> !future.isDone())) {
                capturePrometheusSample(baseUrl, observer.token(), latestPrometheusSnapshot, metricsSamples);
                Thread.sleep(SAMPLER_INTERVAL.toMillis());
            }
            capturePrometheusSample(baseUrl, observer.token(), latestPrometheusSnapshot, metricsSamples);

            List<OperationResult> results = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();

            LatencySummary uploadSummary = LatencySummary.from(results, "upload");
            LatencySummary searchSummary = LatencySummary.from(results, "search");
            double overallErrorRate = calculateOverallErrorRate(results);
            MetricsSummary metricsSummary = MetricsSummary.from(metricsSamples);

            Path reportDirectory = Path.of("target", "load-reports");
            Files.createDirectories(reportDirectory);

            String fileSuffix = FILE_SUFFIX_FORMATTER.format(Instant.now());
            Path reportPath = reportDirectory.resolve("t217-load-report-" + fileSuffix + ".json");
            Path metricsPath = reportDirectory.resolve("t217-prometheus-" + fileSuffix + ".prom");

            Map<String, Object> reportPayload = new LinkedHashMap<>();
            reportPayload.put("generated_at", Instant.now().toString());
            reportPayload.put("base_url", baseUrl);
            reportPayload.put("scenario", "T217");
            reportPayload.put("concurrent_uploads", concurrentUploads);
            reportPayload.put("concurrent_searches", concurrentSearches);
            reportPayload.put("thresholds", Map.of(
                    "search_p95_ms", searchP95BudgetMs,
                    "max_error_rate", errorRateBudget
            ));
            reportPayload.put("uploads", uploadSummary.toMap());
            reportPayload.put("searches", searchSummary.toMap());
            reportPayload.put("overall_error_rate", overallErrorRate);
            reportPayload.put("metrics", metricsSummary.toMap());
            reportPayload.put("report_path", reportPath.toString());
            reportPayload.put("prometheus_snapshot_path", metricsPath.toString());

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(reportPath.toFile(), reportPayload);
            Files.writeString(metricsPath, latestPrometheusSnapshot.get(), StandardCharsets.UTF_8);

            assertThat(metricsSummary.successfulSamples())
                    .as("the Prometheus sampler must collect at least one successful snapshot")
                    .isGreaterThan(0);
            assertThat(uploadSummary.successCount())
                    .as("all concurrent upload flows should complete successfully")
                    .isEqualTo(concurrentUploads);
            assertThat(searchSummary.successCount())
                    .as("all concurrent search flows should complete successfully")
                    .isEqualTo(concurrentSearches);
            assertThat(overallErrorRate)
                    .as("overall error rate must stay below the T217 threshold")
                    .isLessThan(errorRateBudget);
            assertThat(searchSummary.p95Ms())
                    .as("search p95 latency must stay below the T217 threshold")
                    .isLessThan(searchP95BudgetMs);
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private void seedDiscoveryCatalog(String baseUrl, byte[] pngBytes) throws Exception {
        for (int donorIndex = 0; donorIndex < 3; donorIndex++) {
            RegisteredUser donor = registerCompleteUser(
                    baseUrl,
                    "seed-donor-" + donorIndex + "@example.com",
                    "Seed Donor " + donorIndex
            );

            for (int materialIndex = 0; materialIndex < 4; materialIndex++) {
                String uploadId = previewMaterial(
                        baseUrl,
                        donor.token(),
                        pngBytes,
                        "seed-" + donorIndex + "-" + materialIndex + ".png"
                );
                createMaterial(
                        baseUrl,
                        donor.token(),
                        uploadId,
                        "Colecao seed " + donorIndex + "-" + materialIndex,
                        2018 + materialIndex
                );
            }
        }
    }

    private RegisteredUser registerCompleteUser(String baseUrl, String email, String name) throws Exception {
        JsonNode registerResponse = requestJson(
                new Request.Builder()
                        .url(baseUrl + "/v1/auth/register")
                        .post(jsonBody("""
                                {
                                  "email": "%s",
                                  "password": "SenhaSegura123",
                                  "nome": "%s"
                                }
                                """.formatted(email, name)))
                        .build(),
                201
        );

        String token = registerResponse.path("data").path("token").asText();
        assertThat(token).isNotBlank();

        requestJson(
                new Request.Builder()
                        .url(baseUrl + "/v1/usuarios/me")
                        .header("Authorization", "Bearer " + token)
                        .put(jsonBody("""
                                {
                                  "nome": "%s",
                                  "whatsapp": "+5511991234567",
                                  "cpf": "52998224725",
                                  "cidade": "Florianopolis",
                                  "bairro": "Centro",
                                  "instituicao": "Escola de Carga",
                                  "consentimento_ia": true,
                                  "necessidades_academicas": ["TEXTBOOKS", "TEST_PREP"]
                                }
                                """.formatted(name)))
                        .build(),
                200
        );

        return new RegisteredUser(email, name, token);
    }

    private String previewMaterial(String baseUrl, String token, byte[] pngBytes, String fileName) throws Exception {
        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", fileName, RequestBody.create(pngBytes, PNG))
                .build();

        JsonNode response = requestJson(
                new Request.Builder()
                        .url(baseUrl + "/v1/materiais/preview")
                        .header("Authorization", "Bearer " + token)
                        .post(body)
                        .build(),
                200
        );

        String uploadId = response.path("data").path("upload_id").asText();
        assertThat(uploadId).isNotBlank();
        return uploadId;
    }

    private JsonNode createMaterial(String baseUrl,
                                    String token,
                                    String uploadId,
                                    String title,
                                    int publicationYear) throws Exception {
        return requestJson(
                new Request.Builder()
                        .url(baseUrl + "/v1/materiais")
                        .header("Authorization", "Bearer " + token)
                        .post(jsonBody("""
                                {
                                  "upload_id": "%s",
                                  "titulo": "%s",
                                  "descricao": "Descricao de carga para %s",
                                  "disciplina": "MATEMATICA",
                                  "nivel_ensino": "FUNDAMENTAL",
                                  "ano": 7,
                                  "sistema_ensino": "ANGLO",
                                  "estado_conservacao": "BOM",
                                  "data_publicacao": %d
                                }
                                """.formatted(uploadId, title, title, publicationYear)))
                        .build(),
                201
        );
    }

    private JsonNode searchMaterials(String baseUrl, String token, String query) throws Exception {
        HttpUrl url = HttpUrl.parse(baseUrl + "/v1/materiais").newBuilder()
                .addQueryParameter("query", query)
                .addQueryParameter("page", "0")
                .addQueryParameter("size", "10")
                .build();

        return requestJson(
                new Request.Builder()
                        .url(url)
                        .header("Authorization", "Bearer " + token)
                        .get()
                        .build(),
                200
        );
    }

    private JsonNode requestJson(Request request, int expectedStatus) throws Exception {
        try (Response response = httpClient.newCall(request).execute()) {
            String body = response.body() == null ? "" : response.body().string();
            if (response.code() != expectedStatus) {
                throw new HttpStatusException(expectedStatus, response.code(), request.url().encodedPath(), body);
            }
            return objectMapper.readTree(body);
        }
    }

    private String requestText(String url, String token, int expectedStatus) throws Exception {
        Request.Builder builder = new Request.Builder()
                .url(url)
                .get();
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }

        try (Response response = httpClient.newCall(builder.build()).execute()) {
            String body = response.body() == null ? "" : response.body().string();
            if (response.code() != expectedStatus) {
                throw new HttpStatusException(expectedStatus, response.code(), response.request().url().encodedPath(), body);
            }
            return body;
        }
    }

    private RequestBody jsonBody(String payload) {
        return RequestBody.create(payload, JSON);
    }

    private OperationResult runConcurrentOperation(CountDownLatch readyLatch,
                                                   CountDownLatch startLatch,
                                                   LoadOperation operation) {
        readyLatch.countDown();
        try {
            boolean released = startLatch.await(BARRIER_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (!released) {
                return OperationResult.failure("system", 0L, 0, "start barrier timed out");
            }
            return operation.run();
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            return OperationResult.failure("system", 0L, 0, "worker interrupted");
        }
    }

    private double calculateOverallErrorRate(List<OperationResult> results) {
        if (results.isEmpty()) {
            return 0.0;
        }
        long errorCount = results.stream().filter(result -> !result.success()).count();
        return errorCount / (double) results.size();
    }

    private double parsePrometheusValue(String snapshot, String metricName) {
        for (String line : snapshot.split("\\R")) {
            if (line.startsWith(metricName + "{") || line.startsWith(metricName + " ")) {
                int separatorIndex = line.lastIndexOf(' ');
                if (separatorIndex < 0 || separatorIndex == line.length() - 1) {
                    continue;
                }
                String rawValue = line.substring(separatorIndex + 1).trim();
                return Double.parseDouble(rawValue);
            }
        }
        return 0.0;
    }

    private void capturePrometheusSample(String baseUrl,
                                         String token,
                                         AtomicReference<String> latestPrometheusSnapshot,
                                         List<MetricsSample> metricsSamples) {
        try {
            String snapshot = requestText(baseUrl + "/actuator/prometheus", token, 200);
            latestPrometheusSnapshot.set(snapshot);
            metricsSamples.add(new MetricsSample(
                    Instant.now(),
                    parsePrometheusValue(snapshot, "hikaricp_connections"),
                    parsePrometheusValue(snapshot, "hikaricp_connections_active"),
                    parsePrometheusValue(snapshot, "hikaricp_connections_pending"),
                    parsePrometheusValue(snapshot, "hikaricp_connections_acquire_seconds_count"),
                    parsePrometheusValue(snapshot, "hikaricp_connections_acquire_seconds_max"),
                    parsePrometheusValue(snapshot, "hikaricp_connections_timeout_total"),
                    true,
                    null
            ));
        } catch (Exception exception) {
            metricsSamples.add(new MetricsSample(
                    Instant.now(),
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    false,
                    exception.getMessage()
            ));
        }
    }

    private long elapsedMillis(long startedAtNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos);
    }

    private byte[] validPng() throws IOException {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                image.setRGB(x, y, 0x00A86B);
            }
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return outputStream.toByteArray();
    }

    @FunctionalInterface
    private interface LoadOperation {
        OperationResult run();
    }

    private record RegisteredUser(String email, String name, String token) {
    }

    private record OperationResult(String type,
                                   boolean success,
                                   long durationMs,
                                   int statusCode,
                                   String detail) {

        private static OperationResult success(String type, long durationMs, int statusCode, String detail) {
            return new OperationResult(type, true, durationMs, statusCode, detail);
        }

        private static OperationResult failure(String type, long durationMs, int statusCode, String detail) {
            return new OperationResult(type, false, durationMs, statusCode, detail);
        }
    }

    private record LatencySummary(String type,
                                  int totalCount,
                                  int successCount,
                                  int errorCount,
                                  double errorRate,
                                  double averageMs,
                                  long p50Ms,
                                  long p95Ms,
                                  long maxMs,
                                  List<Map<String, Object>> failures) {

        private static LatencySummary from(List<OperationResult> results, String type) {
            List<OperationResult> filtered = results.stream()
                    .filter(result -> result.type().equals(type))
                    .toList();

            List<Long> successfulDurations = filtered.stream()
                    .filter(OperationResult::success)
                    .map(OperationResult::durationMs)
                    .sorted()
                    .toList();

            List<Map<String, Object>> failures = filtered.stream()
                    .filter(result -> !result.success())
                    .map(result -> Map.<String, Object>of(
                            "status_code", result.statusCode(),
                            "duration_ms", result.durationMs(),
                            "detail", result.detail()
                    ))
                    .toList();

            int totalCount = filtered.size();
            int successCount = successfulDurations.size();
            int errorCount = failures.size();
            double errorRate = totalCount == 0 ? 0.0 : errorCount / (double) totalCount;
            double averageMs = successfulDurations.stream().mapToLong(Long::longValue).average().orElse(0.0);
            long p50Ms = percentile(successfulDurations, 0.50);
            long p95Ms = percentile(successfulDurations, 0.95);
            long maxMs = successfulDurations.stream().mapToLong(Long::longValue).max().orElse(0L);

            return new LatencySummary(type, totalCount, successCount, errorCount, errorRate, averageMs, p50Ms, p95Ms, maxMs, failures);
        }

        private Map<String, Object> toMap() {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", type);
            payload.put("total_count", totalCount);
            payload.put("success_count", successCount);
            payload.put("error_count", errorCount);
            payload.put("error_rate", errorRate);
            payload.put("average_ms", averageMs);
            payload.put("p50_ms", p50Ms);
            payload.put("p95_ms", p95Ms);
            payload.put("max_ms", maxMs);
            payload.put("failures", failures);
            return payload;
        }

        private static long percentile(List<Long> sortedDurations, double percentile) {
            if (sortedDurations.isEmpty()) {
                return 0L;
            }
            int index = (int) Math.ceil(percentile * sortedDurations.size()) - 1;
            index = Math.max(0, Math.min(index, sortedDurations.size() - 1));
            return sortedDurations.get(index);
        }
    }

    private record MetricsSample(Instant collectedAt,
                                 double totalConnections,
                                 double activeConnections,
                                 double pendingConnections,
                                 double acquireCount,
                                 double maxAcquireSeconds,
                                 double timeoutTotal,
                                 boolean success,
                                 String errorMessage) {
    }

    private record MetricsSummary(int totalSamples,
                                  int successfulSamples,
                                  int failedSamples,
                                  double maxTotalConnections,
                                  double maxActiveConnections,
                                  double maxPendingConnections,
                                  double maxAcquireCount,
                                  double maxAcquireSeconds,
                                  double maxTimeoutTotal,
                                  List<String> sampleErrors) {

        private static MetricsSummary from(List<MetricsSample> samples) {
            int totalSamples = samples.size();
            List<MetricsSample> successful = samples.stream().filter(MetricsSample::success).toList();
            List<String> sampleErrors = samples.stream()
                    .filter(sample -> !sample.success())
                    .map(MetricsSample::errorMessage)
                    .sorted(Comparator.nullsLast(String::compareTo))
                    .distinct()
                    .toList();

            double maxTotalConnections = successful.stream()
                    .mapToDouble(MetricsSample::totalConnections)
                    .max()
                    .orElse(0.0);
            double maxActive = successful.stream()
                    .mapToDouble(MetricsSample::activeConnections)
                    .max()
                    .orElse(0.0);
            double maxPending = successful.stream()
                    .mapToDouble(MetricsSample::pendingConnections)
                    .max()
                    .orElse(0.0);
            double maxAcquireCount = successful.stream()
                    .mapToDouble(MetricsSample::acquireCount)
                    .max()
                    .orElse(0.0);
            double maxAcquireSeconds = successful.stream()
                    .mapToDouble(MetricsSample::maxAcquireSeconds)
                    .max()
                    .orElse(0.0);
            double maxTimeoutTotal = successful.stream()
                    .mapToDouble(MetricsSample::timeoutTotal)
                    .max()
                    .orElse(0.0);

            return new MetricsSummary(
                    totalSamples,
                    successful.size(),
                    totalSamples - successful.size(),
                    maxTotalConnections,
                    maxActive,
                    maxPending,
                    maxAcquireCount,
                    maxAcquireSeconds,
                    maxTimeoutTotal,
                    sampleErrors
            );
        }

        private Map<String, Object> toMap() {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("total_samples", totalSamples);
            payload.put("successful_samples", successfulSamples);
            payload.put("failed_samples", failedSamples);
            payload.put("max_hikaricp_connections", maxTotalConnections);
            payload.put("max_hikaricp_connections_active", maxActiveConnections);
            payload.put("max_hikaricp_connections_pending", maxPendingConnections);
            payload.put("max_hikaricp_acquire_count", maxAcquireCount);
            payload.put("max_hikaricp_acquire_seconds", maxAcquireSeconds);
            payload.put("max_hikaricp_timeout_total", maxTimeoutTotal);
            payload.put("sample_errors", sampleErrors);
            return payload;
        }
    }

    private static final class HttpStatusException extends RuntimeException {
        private final int statusCode;

        private HttpStatusException(int expectedStatus, int actualStatus, String path, String body) {
            super("Expected HTTP " + expectedStatus + " but got " + actualStatus + " for " + path + ": " + body);
            this.statusCode = actualStatus;
        }

        private int statusCode() {
            return statusCode;
        }
    }
}
