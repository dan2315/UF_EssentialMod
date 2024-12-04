package dod.dodForge.ufessential.utils;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;

public class TokenStorage {
    private static final String APP_DATA_PATH = System.getenv("APPDATA") + File.separator + "UnrealFrontiers";
    private static final String TOKEN_FILE_PATH = APP_DATA_PATH + File.separator + "tokens.dat";
    private static final String KEY_FILE_PATH = APP_DATA_PATH + File.separator + "encryption_key.dat";
    private static final int KEY_SIZE = 16; // 16 bytes for AES-128 encryption

    static {
        File appDataDir = new File(APP_DATA_PATH);
        if (!appDataDir.exists()) {
            appDataDir.mkdirs();
        }
    }

    private static byte[] loadOrGenerateEncryptionKey() throws Exception {
        File keyFile = new File(KEY_FILE_PATH);
        if (keyFile.exists()) {
            // Load existing encryption key
            return Files.readAllBytes(Paths.get(KEY_FILE_PATH));
        } else {
            // Generate a new encryption key
            byte[] key = new byte[KEY_SIZE];
            SecureRandom random = new SecureRandom();
            random.nextBytes(key);

            // Save the key to the file
            Files.write(Paths.get(KEY_FILE_PATH), key);
            keyFile.setReadable(false, false); // Hide the file on Unix systems
            keyFile.setReadable(true, true); // Make readable only by the owner

            return key;
        }
    }

    public static void saveTokens(String accessToken, String refreshToken) throws Exception {
        String tokens = accessToken + "|" + refreshToken;
        byte[] encryptedTokens = encrypt(tokens);

        Files.write(Paths.get(TOKEN_FILE_PATH), encryptedTokens);
    }

    public static String[] loadTokens() throws Exception {
        if (!Files.exists(Paths.get(TOKEN_FILE_PATH))) {
            throw new FileNotFoundException("Token file not found.");
        }

        byte[] encryptedTokens = Files.readAllBytes(Paths.get(TOKEN_FILE_PATH));
        String decryptedTokens = decrypt(encryptedTokens);
        return decryptedTokens.split("\\|");
    }

    public static void deleteTokens() {
        File tokenFile = new File(TOKEN_FILE_PATH);
        if (tokenFile.exists()) {
            tokenFile.delete();
        }
    }

    private static byte[] encrypt(String plainText) throws Exception {
        byte[] key = loadOrGenerateEncryptionKey();
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
        byte[] iv = cipher.getIV();

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             CipherOutputStream cipherOut = new CipherOutputStream(outputStream, cipher)) {

            outputStream.write(iv); // Write IV to the beginning of the file
            cipherOut.write(plainText.getBytes(StandardCharsets.UTF_8));
            cipherOut.flush();
            return outputStream.toByteArray();
        }
    }

    private static String decrypt(byte[] cipherText) throws Exception {
        byte[] key = loadOrGenerateEncryptionKey();
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

        // Extract the IV from the beginning of the cipherText
        byte[] iv = new byte[16];
        System.arraycopy(cipherText, 0, iv, 0, iv.length);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec);

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(cipherText, iv.length, cipherText.length - iv.length);
             CipherInputStream cipherIn = new CipherInputStream(inputStream, cipher);
             InputStreamReader inputStreamReader = new InputStreamReader(cipherIn, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(inputStreamReader)) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }
}
