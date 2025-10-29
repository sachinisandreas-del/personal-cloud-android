package com.andreas.personalcloudclient;

import com.google.gson.annotations.SerializedName;

// This class represents the JSON response after a successful login,
// containing the access and refresh tokens.
public class AuthResponse {

    @SerializedName("access_token")
    private String accessToken;

    @SerializedName("refresh_token")
    private String refreshToken;

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
