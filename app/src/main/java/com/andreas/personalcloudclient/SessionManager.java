package com.andreas.personalcloudclient;

import android.content.Context;
import com.orhanobut.hawk.Hawk;

// This class is a singleton to ensure there's only one instance managing session data.
public class SessionManager {

    // --- Singleton Instance ---
    private static volatile SessionManager INSTANCE;

    // --- Hawk Keys ---
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";

    // The constructor is private to enforce the singleton pattern.
    private SessionManager() {
        // No complex setup needed. Hawk is initialized in the MyApplication class.
    }

    // The public method to get the singleton instance.
    public static SessionManager getInstance() {
        if (INSTANCE == null) {
            synchronized (SessionManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SessionManager();
                }
            }
        }
        return INSTANCE;
    }

    // --- Public Methods for Token Management ---

    /**
     * Saves the access and refresh tokens using Hawk.
     * @param tokens The AuthResponse object received from the server.
     */
    public void saveTokens(AuthResponse tokens) {
        if (tokens != null) {
            Hawk.put(KEY_ACCESS_TOKEN, tokens.getAccessToken());
            Hawk.put(KEY_REFRESH_TOKEN, tokens.getRefreshToken());
        }
    }

    /**
     * Retrieves the saved access token from Hawk.
     * @return The access token, or null if not found.
     */
    public String getAccessToken() {
        return Hawk.get(KEY_ACCESS_TOKEN, null);
    }

    /**
     * Retrieves the saved refresh token from Hawk.
     * @return The refresh token, or null if not found.
     */
    public String getRefreshToken() {
        return Hawk.get(KEY_REFRESH_TOKEN, null);
    }

    /**
     * Clears all saved session data from Hawk (logs the user out).
     */
    public void clearTokens() {
        Hawk.delete(KEY_ACCESS_TOKEN);
        Hawk.delete(KEY_REFRESH_TOKEN);
    }
}
