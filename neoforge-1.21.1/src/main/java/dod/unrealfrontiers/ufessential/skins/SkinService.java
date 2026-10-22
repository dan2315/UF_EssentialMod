package dod.unrealfrontiers.ufessential.skins;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dod.unrealfrontiers.ufessential.Auth.AuthService;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Game-server calls to the backend's skin API ({@code x-api-key}). Players are identified by nickname, the same one
 * the server verifies at login. Futures complete off the server thread; a non-2xx answer (other than 404) fails them.
 */
public final class SkinService {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private SkinService() {}

    /** Mojang-signed textures property of a player's active skin. */
    public record Skin(String value, String signature, String model, long updatedAt) {}

    /** The player's active skin, or empty when they have none (404). */
    public static CompletableFuture<Optional<Skin>> fetch(String name, String apiKey, Duration timeout) {
        HttpRequest request = request("/players/" + URLEncoder.encode(name, StandardCharsets.UTF_8) + "/skin", apiKey, timeout)
                .GET()
                .build();
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).thenApply(r -> {
            if (r.statusCode() == 404) {
                return Optional.empty();
            }
            requireSuccess(r);
            return Optional.of(parseSkin(JsonParser.parseString(r.body()).getAsJsonObject()));
        });
    }

    /** Active skins for several players, keyed by lower-case nickname. Players without a skin are absent. */
    public static CompletableFuture<Map<String, Skin>> lookup(Collection<String> names, String apiKey, Duration timeout) {
        JsonArray array = new JsonArray();
        names.forEach(array::add);
        JsonObject body = new JsonObject();
        body.add("names", array);

        HttpRequest request = request("/players/skins/lookup", apiKey, timeout)
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).thenApply(r -> {
            requireSuccess(r);
            Map<String, Skin> skins = new HashMap<>();
            for (JsonElement element : JsonParser.parseString(r.body()).getAsJsonObject().getAsJsonArray("skins")) {
                JsonObject skin = element.getAsJsonObject();
                skins.put(skin.get("name").getAsString().toLowerCase(Locale.ROOT), parseSkin(skin));
            }
            return skins;
        });
    }

    private static HttpRequest.Builder request(String path, String apiKey, Duration timeout) {
        return HttpRequest.newBuilder(URI.create(AuthService.getBaseUrl() + path))
                .timeout(timeout)
                .header("x-api-key", apiKey);
    }

    private static Skin parseSkin(JsonObject json) {
        return new Skin(
                json.get("value").getAsString(),
                json.get("signature").getAsString(),
                json.has("model") ? json.get("model").getAsString() : "classic",
                json.has("updatedAt") ? json.get("updatedAt").getAsLong() : 0);
    }

    private static void requireSuccess(HttpResponse<String> response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
    }
}
