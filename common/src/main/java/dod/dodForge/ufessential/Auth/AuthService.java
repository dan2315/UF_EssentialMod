package dod.dodForge.ufessential.Auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dod.dodForge.ufessential.utils.TokenStorage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class AuthService {
    private static final String BASE_URL = "https://unrealfrontiers.xyz";
    private static final String REQUEST_ACCESS_ENDPOINT = "/auth/verify_nickname";
    private static final String REFRESH_TOKENS_ENDPOINT = "/auth/refresh_tokens";

    public static boolean isAuthenticated(String nickname) throws Exception {
        String[] tokens = TokenStorage.loadTokens();
        return isAuthenticated(tokens[0], tokens[1], nickname);
    }

    public static boolean isAuthenticated(String accessToken, String refreshToken, String nickname) throws Exception {
        if (isNicknameValid(accessToken, nickname)) {
            return true;
        } else {
            // Try refreshing the token
            String newAccessToken = refreshAccessToken(refreshToken);
            if (newAccessToken != null) {
                // Save the updated access token
                TokenStorage.saveTokens(newAccessToken, refreshToken);
                return true;
            }
        }
        return false;
    }

    public static boolean isNicknameValid(String accessToken, String nickname) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(BASE_URL + REQUEST_ACCESS_ENDPOINT + "?nickname=" + URLEncoder.encode(nickname, StandardCharsets.UTF_8));
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
            URL url = new URL(BASE_URL + REFRESH_TOKENS_ENDPOINT);
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
