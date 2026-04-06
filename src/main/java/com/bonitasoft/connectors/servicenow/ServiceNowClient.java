package com.bonitasoft.connectors.servicenow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * HTTP client facade for the ServiceNow Table API.
 * Uses java.net.http.HttpClient and Jackson for JSON processing.
 */
@Slf4j
public class ServiceNowClient {

    private final ServiceNowConfiguration configuration;
    private final RetryPolicy retryPolicy;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private String oauthAccessToken;

    public ServiceNowClient(ServiceNowConfiguration configuration) throws ServiceNowException {
        this.configuration = configuration;
        this.retryPolicy = new RetryPolicy(configuration.getMaxRetries());
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(configuration.getConnectTimeout()))
                .build();

        if ("OAUTH2".equals(configuration.getAuthMode())) {
            this.oauthAccessToken = obtainOAuthToken();
        }
        log.debug("ServiceNowClient initialized with auth mode: {}", configuration.getAuthMode());
    }

    // === Operation methods ===

    public Map<String, Object> createIncident(Map<String, Object> fields) throws ServiceNowException {
        return retryPolicy.execute(() -> {
            String body = serializeJson(fields);
            HttpRequest request = buildPostRequest("/api/now/table/incident", body);
            return executeAndParseRecord(request);
        });
    }

    public Map<String, Object> updateIncident(String sysId, Map<String, Object> fields) throws ServiceNowException {
        return retryPolicy.execute(() -> {
            String body = serializeJson(fields);
            HttpRequest request = buildPatchRequest("/api/now/table/incident/" + sysId, body);
            return executeAndParseRecord(request);
        });
    }

    public Map<String, Object> getRecord(String tableName, String sysId) throws ServiceNowException {
        return retryPolicy.execute(() -> {
            HttpRequest request = buildGetRequest("/api/now/table/" + tableName + "/" + sysId);
            return executeAndParseRecord(request);
        });
    }

    public Map<String, Object> createChangeRequest(Map<String, Object> fields) throws ServiceNowException {
        return retryPolicy.execute(() -> {
            String body = serializeJson(fields);
            HttpRequest request = buildPostRequest("/api/now/table/change_request", body);
            return executeAndParseRecord(request);
        });
    }

    public QueryResult queryTable(String tableName, String query, String fields, int limit, int offset)
            throws ServiceNowException {
        return retryPolicy.execute(() -> {
            StringBuilder path = new StringBuilder("/api/now/table/");
            path.append(tableName);
            path.append("?sysparm_query=").append(URLEncoder.encode(query != null ? query : "", StandardCharsets.UTF_8));
            if (fields != null && !fields.isBlank()) {
                path.append("&sysparm_fields=").append(URLEncoder.encode(fields, StandardCharsets.UTF_8));
            }
            path.append("&sysparm_limit=").append(limit);
            path.append("&sysparm_offset=").append(offset);

            HttpRequest request = buildGetRequest(path.toString());
            HttpResponse<String> response = sendRequest(request);
            handleErrors(response);

            Map<String, Object> responseBody = parseJson(response.body());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> records = (List<Map<String, Object>>) responseBody.get("result");
            String totalCount = response.headers().firstValue("X-Total-Count").orElse(null);
            return new QueryResult(records != null ? records : List.of(),
                    totalCount != null ? Integer.parseInt(totalCount) : -1);
        });
    }

    public Map<String, Object> createRecord(String tableName, Map<String, Object> fields) throws ServiceNowException {
        return retryPolicy.execute(() -> {
            String body = serializeJson(fields);
            HttpRequest request = buildPostRequest("/api/now/table/" + tableName, body);
            return executeAndParseRecord(request);
        });
    }

    public Map<String, Object> updateRecord(String tableName, String sysId, Map<String, Object> fields)
            throws ServiceNowException {
        return retryPolicy.execute(() -> {
            String body = serializeJson(fields);
            HttpRequest request = buildPatchRequest("/api/now/table/" + tableName + "/" + sysId, body);
            return executeAndParseRecord(request);
        });
    }

    public Map<String, Object> addJournal(String tableName, String sysId, String journalField, String journalValue)
            throws ServiceNowException {
        return retryPolicy.execute(() -> {
            Map<String, Object> fields = Map.of(journalField, journalValue);
            String body = serializeJson(fields);
            HttpRequest request = buildPatchRequest("/api/now/table/" + tableName + "/" + sysId, body);
            return executeAndParseRecord(request);
        });
    }

    // === Internal helpers ===

    private Map<String, Object> executeAndParseRecord(HttpRequest request) throws ServiceNowException {
        HttpResponse<String> response = sendRequest(request);
        handleErrors(response);
        Map<String, Object> responseBody = parseJson(response.body());
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) responseBody.get("result");
        return result != null ? result : Map.of();
    }

    private HttpRequest buildGetRequest(String path) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(buildUri(path))
                .timeout(Duration.ofMillis(configuration.getReadTimeout()))
                .header("Accept", "application/json")
                .GET();
        addAuthHeader(builder);
        return builder.build();
    }

    private HttpRequest buildPostRequest(String path, String body) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(buildUri(path))
                .timeout(Duration.ofMillis(configuration.getReadTimeout()))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        addAuthHeader(builder);
        return builder.build();
    }

    private HttpRequest buildPatchRequest(String path, String body) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(buildUri(path))
                .timeout(Duration.ofMillis(configuration.getReadTimeout()))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body));
        addAuthHeader(builder);
        return builder.build();
    }

    private URI buildUri(String path) {
        String baseUrl = configuration.getInstanceUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return URI.create(baseUrl + path);
    }

    private void addAuthHeader(HttpRequest.Builder builder) {
        switch (configuration.getAuthMode()) {
            case "BASIC":
                String credentials = configuration.getUsername() + ":" + configuration.getPassword();
                String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
                builder.header("Authorization", "Basic " + encoded);
                break;
            case "OAUTH2":
                builder.header("Authorization", "Bearer " + oauthAccessToken);
                break;
            case "API_KEY":
                builder.header("X-ServiceNow-API-Key", configuration.getApiKey());
                break;
            default:
                break;
        }
    }

    private String obtainOAuthToken() throws ServiceNowException {
        try {
            String tokenUrl = configuration.getInstanceUrl();
            if (tokenUrl.endsWith("/")) {
                tokenUrl = tokenUrl.substring(0, tokenUrl.length() - 1);
            }
            tokenUrl += "/oauth_token.do";

            String formBody = "grant_type=client_credentials"
                    + "&client_id=" + URLEncoder.encode(configuration.getClientId(), StandardCharsets.UTF_8)
                    + "&client_secret=" + URLEncoder.encode(configuration.getClientSecret(), StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenUrl))
                    .timeout(Duration.ofMillis(configuration.getConnectTimeout()))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new ServiceNowException(
                        "OAuth2 token request failed with status " + response.statusCode() + ": " + response.body(),
                        response.statusCode(), false);
            }
            Map<String, Object> tokenResponse = parseJson(response.body());
            String accessToken = (String) tokenResponse.get("access_token");
            if (accessToken == null || accessToken.isBlank()) {
                throw new ServiceNowException("OAuth2 response did not contain access_token");
            }
            return accessToken;
        } catch (ServiceNowException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ServiceNowException("Failed to obtain OAuth2 token", e);
        }
    }

    private HttpResponse<String> sendRequest(HttpRequest request) throws ServiceNowException {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new ServiceNowException("Network error: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceNowException("Request interrupted", e);
        }
    }

    private void handleErrors(HttpResponse<String> response) throws ServiceNowException {
        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            return;
        }
        boolean retryable = RetryPolicy.isRetryableStatusCode(status);
        String message = "ServiceNow API error " + status + ": " + response.body();
        throw new ServiceNowException(message, status, retryable);
    }

    private String serializeJson(Object obj) throws ServiceNowException {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new ServiceNowException("Failed to serialize request body to JSON", e);
        }
    }

    private Map<String, Object> parseJson(String json) throws ServiceNowException {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new ServiceNowException("Failed to parse response JSON", e);
        }
    }

    /** Immutable result for query operations. */
    public record QueryResult(List<Map<String, Object>> records, int totalCount) {}
}
