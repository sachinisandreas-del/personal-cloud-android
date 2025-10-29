package com.andreas.personalcloudclient;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

// This interceptor will add the Authorization header to requests.
public class AuthInterceptor implements Interceptor {

    private final SessionManager sessionManager;

    public AuthInterceptor(Context context) {
        // Get the singleton instance of our SessionManager.
        this.sessionManager = SessionManager.getInstance();
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        // 1. Get the original request from the chain.
        Request originalRequest = chain.request();

        // 2. Get the access token from our SessionManager.
        String accessToken = sessionManager.getAccessToken();

        // 3. If we don't have a token, proceed with the original request.
        //    This is important for requests like login/register that don't need a token.
        if (accessToken == null) {
            return chain.proceed(originalRequest);
        }

        // 4. If we DO have a token, build a new request.
        Request.Builder builder = originalRequest.newBuilder()
            // Add the Authorization header with the "Bearer" prefix.
            .header("Authorization", "Bearer " + accessToken);

        Request newRequest = builder.build();

        // 5. Proceed with the NEW, modified request.
        return chain.proceed(newRequest);
    }
}
