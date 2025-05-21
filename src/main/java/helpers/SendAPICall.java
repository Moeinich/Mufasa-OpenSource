package helpers;

import helpers.utils.HttpResponseException;
import helpers.utils.SendAPICallException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class SendAPICall {
    // Singleton HttpClient instance
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * Sends a synchronous POST request with JSON payload.
     *
     * @param endpoint The API endpoint URL.
     * @param payload  The JSON payload as a String.
     * @return The response body as a String.
     * @throws SendAPICallException If an error occurs during the API call.
     */
    public static String sendRequest(String endpoint, String payload) throws SendAPICallException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            String responseBody = response.body();

            if (statusCode >= 200 && statusCode < 300) {
                return responseBody;
            } else {
                throw new HttpResponseException(statusCode, responseBody);
            }
        } catch (IOException | InterruptedException e) {
            throw new SendAPICallException("Failed to send request to " + endpoint, e);
        }
    }

    /**
     * Sends an asynchronous POST request with JSON payload.
     *
     * @param endpoint The API endpoint URL.
     * @param payload  The JSON payload as a String.
     * @return A CompletableFuture containing the response body as a String.
     */
    public static CompletableFuture<String> sendRequestAsync(String endpoint, String payload) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> {
                    int statusCode = response.statusCode();
                    String responseBody = response.body();

                    if (statusCode >= 200 && statusCode < 300) {
                        return CompletableFuture.completedFuture(responseBody);
                    } else {
                        CompletableFuture<String> failedFuture = new CompletableFuture<>();
                        failedFuture.completeExceptionally(new HttpResponseException(statusCode, responseBody));
                        return failedFuture;
                    }
                });
    }

    /**
     * Sends a synchronous POST request with JSON payload and retry logic.
     *
     * @param endpoint    The API endpoint URL.
     * @param payload     The JSON payload as a String.
     * @param maxRetries  Maximum number of retry attempts.
     * @param retryDelay  Delay between retries in milliseconds.
     * @return The response body as a String.
     * @throws SendAPICallException If all retry attempts fail.
     */
    public static String sendRequestWithRetry(String endpoint, String payload, int maxRetries, long retryDelay) throws SendAPICallException {
        int attempt = 0;
        while (attempt <= maxRetries) {
            try {
                return sendRequest(endpoint, payload);
            } catch (SendAPICallException e) {
                attempt++;
                if (attempt > maxRetries) {
                    throw new SendAPICallException("Exceeded maximum retry attempts for endpoint: " + endpoint, e);
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(retryDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new SendAPICallException("Retry interrupted", ie);
                }
            }
        }
        throw new SendAPICallException("Failed to send request after retries");
    }
}
