package com.andreas.personalcloudclient;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// This repository will handle all authentication-related network calls.
public class AuthRepository {

    private final ApiService apiService;

    public AuthRepository(Context context) {
        // Pass the context to the RetrofitClient
        this.apiService = RetrofitClient.getClient(context).create(ApiService.class);
    }

    // A generic callback for the ViewModel to use.
    public interface AuthCallback<T> {
        void onSuccess(T response);
        void onError(String message);
    }

    public void login(String loginIdentifier, String password, AuthCallback<AuthResponse> callback) {

        LoginRequest loginRequest = new LoginRequest(loginIdentifier, password);
        apiService.login(loginRequest).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    try {
                        if (response.errorBody() != null) {
                            callback.onError("Login failed: " + response.errorBody().string());
                        } else {
                            callback.onError("Login failed with code: " + response.code());
                        }
                    } catch (IOException e) {
                        callback.onError("Login failed with code: " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void register(String username, String email, String password, AuthCallback<ResponseBody> callback) {
        RegisterRequest registerRequest = new RegisterRequest(username, email, password);
        apiService.register(registerRequest).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body());
                } else {
                    try {
                        if (response.errorBody() != null) {
                            callback.onError("Registration failed: " + response.errorBody().string());
                        } else {
                            callback.onError("Registration failed with code: " + response.code());
                        }
                    } catch (IOException e) {
                        callback.onError("Registration failed with code: " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
    public void loginWithGoogle(String googleToken, AuthCallback<AuthResponse> callback) {
        //Create a simple Map to build the JSON body: {"google_token": "..."}
        Map<String, String> body = new HashMap<>();
        body.put("google_token", googleToken);

        apiService.loginWithGoogle(body).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    try {
                        if (response.errorBody() != null) {
                            // Can add specific handling for the "account_exists_with_password" error here later.
                            callback.onError("Google Sign-In failed: " + response.errorBody().string());
                        } else {
                            callback.onError("Google Sign-In failed with code: " + response.code());
                        }
                    } catch (IOException e) {
                        callback.onError("Google Sign-In failed with code: " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
}
