package com.andreas.personalcloudclient;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import retrofit2.Call;

// This class is responsible for refreshing the access token when it expires.
public class TokenAuthenticator implements Authenticator {

    private static final String TAG = "TokenAuthenticator";
    private final SessionManager sessionManager;
    private final Context context;

    public TokenAuthenticator(Context context) {
        this.context = context.getApplicationContext();
        this.sessionManager = SessionManager.getInstance();
    }

    @Nullable
    @Override
    public Request authenticate(@NonNull Route route, @NonNull Response response) throws IOException {
        Log.d(TAG, "Authentication required. Response code: " + response.code());

        // 1. Get the refresh token from our session manager.
        final String refreshToken = sessionManager.getRefreshToken();

        // If we there is not a refresh token, can't do anything.
        // The user must log in from scratch.
        if (refreshToken == null) {
            Log.d(TAG, "No refresh token available. Cannot authenticate.");
            // TODO: Here need navigate the user back to the LoginActivity.
            return null; // Null means "give up".
        }

        // 2. Need to make a synchronous network call to the /token/refresh endpoint.
        //    It MUST be synchronous to block the original request chain.
        ApiService apiService = RetrofitClient.getClient(context).create(ApiService.class);
        Map<String, String> body = new HashMap<>();
        body.put("refresh_token", refreshToken);
        Call<AuthResponse> refreshTokenCall = apiService.refreshToken(body);

        try {
            // Execute the call synchronously.
            retrofit2.Response<AuthResponse> refreshResponse = refreshTokenCall.execute();

            // 3. Check the response from the refresh endpoint.
            if (refreshResponse.isSuccessful() && refreshResponse.body() != null) {
                Log.d(TAG, "Token refresh successful.");
                // 4. If successful, save the new tokens.
                AuthResponse newTokens = refreshResponse.body();
                sessionManager.saveTokens(newTokens);

                // 5. Build a NEW request, which is a copy of the original failed request,
                //    but with the NEW access token in the Authorization header.
                return response.request().newBuilder()
                    .header("Authorization", "Bearer " + newTokens.getAccessToken())
                    .build();
            } else {
                Log.e(TAG, "Token refresh failed with code: " + refreshResponse.code());
                // The refresh token might be invalid or expired.
                // The user must log in from scratch.
                sessionManager.clearTokens(); // Log the user out.
                // TODO: Navigate the user back to the LoginActivity.
                return null; // Null means "give up".
            }
        } catch (IOException e) {
            Log.e(TAG, "Token refresh network call failed", e);
            return null; // Null means "give up".
        }
    }
}
