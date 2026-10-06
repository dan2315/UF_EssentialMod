package dod.unrealfrontiers.ufessential.api;

import dod.unrealfrontiers.ufessential.Auth.AuthService;
import dod.unrealfrontiers.ufessential.utils.TokenStorage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Public API for other mods to call Unreal Frontiers backend endpoints.
 * Requests are sent asynchronously with the stored access token; on 401 the token
 * is refreshed once and the request is retried.
 *
 * <pre>
 * UFHttpClient.get("/some/endpoint").thenAccept(r -> ...);
 * UFHttpClient.postJson("/some/endpoint", "{\"a\":1}").thenAccept(r -> ...);
 * </pre>
 */
public final class UFHttpClient {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private UFHttpClient() {}

    public record Response(int statusCode, String body) {
        public boolean isSuccess() {
            return statusCode >= 200 && statusCode < 300;
        }
    }

    /** @param path endpoint path relative to the configured backend base URL, e.g. "/auth/verify_nickname?nickname=x" */
    public static CompletableFuture<Response> get(String path) {
        return send("GET", path, null);
    }

    public static CompletableFuture<Response> postJson(String path, String jsonBody) {
        return send("POST", path, jsonBody);
    }

    public static CompletableFuture<Response> putJson(String path, String jsonBody) {
        return send("PUT", path, jsonBody);
    }

    public static CompletableFuture<Response> delete(String path) {
        return send("DELETE", path, null);
    }

    public static CompletableFuture<Response> send(String method, String path, String jsonBody) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String[] tokens = loadTokens();
                String access = tokens == null ? null : tokens[0];
                Response response = execute(method, path, jsonBody, access);
                if (response.statusCode() == 401 && tokens != null && tokens.length > 1) {
                    String refreshed = AuthService.refreshAccessToken(tokens[1]);
                    if (refreshed != null) {
                        TokenStorage.saveTokens(refreshed, tokens[1]);
                        response = execute(method, path, jsonBody, refreshed);
                    }
                }
                return response;
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static String[] loadTokens() {
        try {
            return TokenStorage.loadTokens();
        } catch (Exception e) {
            return null; // not logged in: request goes out unauthenticated
        }
    }

    private static Response execute(String method, String path, String jsonBody, String accessToken) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(AuthService.getBaseUrl() + path))
                .timeout(Duration.ofSeconds(10))
                .method(method, jsonBody == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));
        if (jsonBody != null) {
            builder.header("Content-Type", "application/json; charset=utf-8");
        }
        if (accessToken != null) {
            builder.header("Authorization", "Bearer " + accessToken);
        }
        HttpResponse<String> r = CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new Response(r.statusCode(), r.body());
    }
}
