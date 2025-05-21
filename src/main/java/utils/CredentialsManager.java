package utils;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.JSONObject;

import java.util.Base64;
import java.util.Set;

public class CredentialsManager {
    private final String CRED_FILE_PATH = SystemUtils.getSystemPath() + "creds.json";

    public Set<String> loadUsers() {
        try {
            if (Files.exists(Paths.get(CRED_FILE_PATH))) {
                String content = new String(Files.readAllBytes(Paths.get(CRED_FILE_PATH)));
                JSONObject jsonObject = new JSONObject(content);
                return jsonObject.keySet();
            }
        } catch (Exception e) {
            e.printStackTrace(); // Handle exceptions appropriately
        }
        return null; // or return an empty set
    }

    public String getPassword(String username) {
        return getDecryptedData(username, "password");
    }

    public String getBankPin(String username) {
        return getDecryptedData(username, "bankPin");
    }

    public String getWebhookURL(String username) {
        String decryptedURL = getDecryptedData(username, "webhookURL");
        if (decryptedURL != null && decryptedURL.contains("https")) {
            return decryptedURL; // Return the URL if it contains "https"
        } else {
            return null; // Return null if "https" is missing
        }
    }

    public String getFrequency(String username) {
        return getData(username, "frequency");
    }

    public Boolean isBreaksEnabled(String username) {
        return getBooleanData(username, "breaksEnabled", true);
    }

    private String getData(String username, String key) {
        try {
            JSONObject jsonObject = new JSONObject(new String(Files.readAllBytes(Paths.get(CRED_FILE_PATH))));
            if (jsonObject.has(username)) {
                JSONObject userData = jsonObject.getJSONObject(username);
                if (!userData.has(key)) return null;  // Key does not exist for this user
                return userData.getString(key);  // Return the plain data
            }
        } catch (Exception e) {
            e.printStackTrace();  // Log exception to standard error
        }
        return null;  // Return null if any error occurs or the data is not found
    }

    private Boolean getBooleanData(String username, String key, boolean defaultValue) {
        try {
            JSONObject jsonObject = new JSONObject(new String(Files.readAllBytes(Paths.get(CRED_FILE_PATH))));
            if (jsonObject.has(username)) {
                JSONObject userData = jsonObject.getJSONObject(username);
                // Return the boolean value, defaulting to the provided defaultValue if the key is missing
                return userData.optBoolean(key, defaultValue);
            }
        } catch (Exception e) {
            e.printStackTrace();  // Log exception to standard error
        }
        return defaultValue;  // Return defaultValue (true) if an error occurs or the key is not found
    }

    private String getDecryptedData(String username, String key) {
        try {
            JSONObject jsonObject = new JSONObject(new String(Files.readAllBytes(Paths.get(CRED_FILE_PATH))));
            if (jsonObject.has(username)) {
                JSONObject userData = jsonObject.getJSONObject(username);
                if (!userData.has(key)) return null;
                String encryptedData = userData.getString(key);
                String ivString = userData.getString("iv");
                byte[] ivBytes = Base64.getDecoder().decode(ivString);

                return Encryption.decrypt(encryptedData, ivBytes);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
