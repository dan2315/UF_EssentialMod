package dod.unrealfrontiers.ufessential.Auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dod.unrealfrontiers.ufessential.utils.TokenStorage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class AuthService {
    public static final String DEFAULT_BASE_URL = "https://unrealfrontiers.com";
    /** Backend URL handed over by the launcher. */
    public static final String ENV_BACKEND_URL = "UF_BACKEND_URL";
    private static volatile String baseUrl = defaultBaseUrl();
    private static final String REQUEST_ACCESS_ENDPOINT = "/auth/verify_nickname";
    private static final String REFRESH_TOKENS_ENDPOINT = "/auth/refresh_tokens";

    public static String getBaseUrl() {
        return baseUrl;
    }

    /** Overrides the backend URL; null or blank falls back to {@link #ENV_BACKEND_URL}, then {@link #DEFAULT_BASE_URL}. */
    public static void setBaseUrl(String url) {
        baseUrl = url == null || url.isBlank() ? defaultBaseUrl() : normalize(url);
    }

    private static String defaultBaseUrl() {
        String env = System.getenv(ENV_BACKEND_URL);
        return env == null || env.isBlank() ? DEFAULT_BASE_URL : normalize(env);
    }

    private static String normalize(String url) {
        String trimmed = url.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    /** Checks this machine's tokens, saving a refreshed access token when they came from the token file. */
    public static boolean isAuthenticated(String nickname) throws Exception {
        String[] tokens = TokenStorage.loadTokens();
        if (tokens == null) {
            return false;
        }
        if (isNicknameValid(tokens[0], nickname)) {
            return true;
        }
        if (tokens[1].isBlank()) {
            return false;
        }
        String newAccessToken = refreshAccessToken(tokens[1]);
        if (newAccessToken != null && isNicknameValid(newAccessToken, nickname)) {
            TokenStorage.saveTokens(newAccessToken, tokens[1]);
            return true;
        }
        return false;
    }

    /**
     * Checks tokens a player sent (server side). A refreshed token is only used for the check, never stored, and
     * must belong to the same nickname: a successful refresh alone doesn't prove who the player is.
     * Players started by a current launcher send no refresh token, so there's nothing to retry with.
     */
    public static boolean isAuthenticated(String accessToken, String refreshToken, String nickname) throws Exception {
        if (accessToken == null || accessToken.isBlank()) {
            return false;
        }
        if (isNicknameValid(accessToken, nickname)) {
            return true;
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            return false;
        }
        String newAccessToken = refreshAccessToken(refreshToken);
        return newAccessToken != null && isNicknameValid(newAccessToken, nickname);
    }

    public static boolean isNicknameValid(String accessToken, String nickname) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(getBaseUrl() + REQUEST_ACCESS_ENDPOINT + "?nickname=" + URLEncoder.encode(nickname, StandardCharsets.UTF_8));
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(10_000);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String response = new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                return json.has("valid") && json.get("valid").getAsBoolean();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return false;
    }

    public static String refreshAccessToken(String refreshToken) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(getBaseUrl() + REFRESH_TOKENS_ENDPOINT);
            System.err.println(url);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(10_000);
            connection.setDoOutput(true);

            JsonObject jsonBody = new JsonObject();
            jsonBody.addProperty("refreshToken", refreshToken);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonBody.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String response = new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                // The backend answers {"token", "refreshToken"}; "accessToken" is the older name
                if (json.has("token")) {
                    return json.get("token").getAsString();
                }
                if (json.has("accessToken")) {
                    return json.get("accessToken").getAsString();
                }
            } else {
                String errorResponse = "";
                try (InputStream errorStream = connection.getErrorStream())
                {
                    if (errorStream != null)
                    {
                        errorResponse = new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);
                    }
                }
                System.err.println("[UF] Failed to refresh token: " + connection.getResponseMessage() + ": " + errorResponse);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return null;
    }

    public static void saveNewTokens(String accessToken, String refreshToken) {
        try {
            TokenStorage.saveTokens(accessToken, refreshToken);
            System.out.println("Tokens saved successfully.");
        } catch (Exception e) {
            System.err.println("Error saving tokens: " + e.getMessage());
        }
    }

    public static void deleteTokens() {
        TokenStorage.deleteTokens();
        System.out.println("Tokens deleted successfully.");
    }
}
