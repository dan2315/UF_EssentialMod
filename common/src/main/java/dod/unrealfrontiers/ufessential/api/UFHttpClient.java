package dod.unrealfrontiers.ufessential.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dod.unrealfrontiers.ufessential.Auth.AuthService;
import dod.unrealfrontiers.ufessential.utils.TokenStorage;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Public API for other mods to call Unreal Frontiers backend endpoints.
 * Requests are sent asynchronously with the stored access token; on 401 the token
 * is refreshed once and the request is retried. Without stored tokens requests go out
 * with no Authorization header. Tokens come from the launcher (UF_ACCESS_TOKEN) or, as a fallback, the token file.
 * 4xx/5xx are normal responses; only network failures
 * complete the future exceptionally. Callbacks run off the game thread.
 *
 * <pre>
 * UFHttpClient.get("/some/endpoint").thenAccept(r -> ...);
 * UFHttpClient.postJson("/some/endpoint", "{\"a\":1}").thenAccept(r -> ...);
 * UFHttpClient.getBytes("/schematics/1/download").thenAccept(r -> ...);
 * UFHttpClient.sendMultipart("POST", "/schematics", new UFHttpClient.Multipart()
 *         .field("name", "House")
 *         .file("file", "house.nbt", "application/octet-stream", bytes));
 * </pre>
 */
public final class UFHttpClient {
    private static final Duration JSON_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration TRANSFER_TIMEOUT = Duration.ofSeconds(30);

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private UFHttpClient() {}

    public record Response(int statusCode, String body) {
        public boolean isSuccess() {
            return statusCode >= 200 && statusCode < 300;
        }

        /** The backend's {@code error} text, or {@code "HTTP <code>"} if there is none. */
        public String errorMessage() {
            return extractError(statusCode, body);
        }
    }

    public record BytesResponse(int statusCode, byte[] body, String contentType) {
        public boolean isSuccess() {
            return statusCode >= 200 && statusCode < 300;
        }

        /** The backend's {@code error} text, or {@code "HTTP <code>"} if there is none. */
        public String errorMessage() {
            return extractError(statusCode, body == null ? null : new String(body, StandardCharsets.UTF_8));
        }
    }

    /** True when the launcher handed over a token (or a readable token file exists), i.e. the player is signed in. */
    public static boolean isLoggedIn() {
        String[] tokens = TokenStorage.loadTokens();
        return tokens != null;
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
        byte[] body = jsonBody == null ? null : jsonBody.getBytes(StandardCharsets.UTF_8);
        return exchange(method, path, body, "application/json; charset=utf-8", JSON_TIMEOUT,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(r -> new Response(r.statusCode(), r.body()));
    }

    /** GET returning the raw response bytes (files, images). Error bodies are kept as-is. */
    public static CompletableFuture<BytesResponse> getBytes(String path) {
        return exchange("GET", path, null, null, TRANSFER_TIMEOUT, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(r -> new BytesResponse(r.statusCode(), r.body(),
                        r.headers().firstValue("Content-Type").orElse(null)));
    }

    /** Sends a multipart/form-data request (RFC 7578). */
    public static CompletableFuture<Response> sendMultipart(String method, String path, Multipart multipart) {
        // Built once up front so the 401 retry sends exactly the same bytes.
        byte[] body = multipart.build();
        return exchange(method, path, body, multipart.contentType(), TRANSFER_TIMEOUT,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(r -> new Response(r.statusCode(), r.body()));
    }

    /** Builder for multipart/form-data bodies. Parts are sent in the order they were added. */
    public static final class Multipart {
        private static final SecureRandom RANDOM = new SecureRandom();
        private static final byte[] CRLF = "\r\n".getBytes(StandardCharsets.UTF_8);

        private final String boundary;
        private final List<Part> parts = new ArrayList<>();

        private record Part(String name, String filename, String contentType, byte[] data) {}

        public Multipart() {
            StringBuilder sb = new StringBuilder("UFBoundary");
            for (int i = 0; i < 24; i++) {
                sb.append(Character.forDigit(RANDOM.nextInt(36), 36));
            }
            boundary = sb.toString();
        }

        /** Adds a UTF-8 text part. */
        public Multipart field(String name, String value) {
            parts.add(new Part(name, null, null, value.getBytes(StandardCharsets.UTF_8)));
            return this;
        }

        /** Adds a file part. May be called repeatedly with the same name. */
        public Multipart file(String name, String filename, String contentType, byte[] data) {
            parts.add(new Part(name, filename, contentType, data));
            return this;
        }

        String contentType() {
            return "multipart/form-data; boundary=" + boundary;
        }

        byte[] build() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (Part part : parts) {
                StringBuilder headers = new StringBuilder()
                        .append("--").append(boundary).append("\r\n")
                        .append("Content-Disposition: form-data; name=\"").append(escape(part.name())).append('"');
                if (part.filename() != null) {
                    headers.append("; filename=\"").append(escape(part.filename())).append('"');
                }
                headers.append("\r\n");
                if (part.filename() != null) {
                    String type = part.contentType() == null ? "application/octet-stream" : part.contentType();
                    headers.append("Content-Type: ").append(type).append("\r\n");
                }
                headers.append("\r\n");
                out.writeBytes(headers.toString().getBytes(StandardCharsets.UTF_8));
                out.writeBytes(part.data());
                out.writeBytes(CRLF);
            }
            out.writeBytes(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            return out.toByteArray();
        }

        // Same escaping browsers use for form-data names/filenames; other characters stay raw UTF-8.
        private static String escape(String s) {
            return s.replace("\"", "%22").replace("\r", "%0D").replace("\n", "%0A");
        }
    }

    private static <T> CompletableFuture<HttpResponse<T>> exchange(String method, String path, byte[] body,
                                                                   String contentType, Duration timeout,
                                                                   HttpResponse.BodyHandler<T> handler) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String[] tokens = TokenStorage.loadTokens();
                String access = tokens == null ? null : tokens[0];
                HttpResponse<T> response = execute(method, path, body, contentType, timeout, handler, access);
                // Launcher-provided tokens come without a refresh token: a 401 is then just returned
                if (response.statusCode() == 401 && tokens != null && !tokens[1].isBlank()) {
                    String refreshed = AuthService.refreshAccessToken(tokens[1]);
                    if (refreshed != null) {
                        TokenStorage.saveTokens(refreshed, tokens[1]);
                        response = execute(method, path, body, contentType, timeout, handler, refreshed);
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

    private static <T> HttpResponse<T> execute(String method, String path, byte[] body, String contentType,
                                               Duration timeout, HttpResponse.BodyHandler<T> handler,
                                               String accessToken) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(AuthService.getBaseUrl() + path))
                .timeout(timeout)
                .method(method, body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofByteArray(body));
        if (body != null && contentType != null) {
            builder.header("Content-Type", contentType);
        }
        if (accessToken != null) {
            builder.header("Authorization", "Bearer " + accessToken);
        }
        return CLIENT.send(builder.build(), handler);
    }

    private static String extractError(int statusCode, String body) {
        if (body != null && !body.isBlank()) {
            try {
                JsonElement json = JsonParser.parseString(body);
                if (json.isJsonObject()) {
                    JsonObject obj = json.getAsJsonObject();
                    if (obj.has("error") && obj.get("error").isJsonPrimitive()) {
                        return obj.get("error").getAsString();
                    }
                }
            } catch (RuntimeException ignored) {
                // not JSON
            }
        }
        return "HTTP " + statusCode;
    }
}
