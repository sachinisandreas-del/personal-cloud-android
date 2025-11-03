package com.andreas.personalcloudclient;

import com.google.gson.annotations.SerializedName;

// This class represents the JSON body for a traditional login request.
public class LoginRequest {

    @SerializedName("login_identifier")
    private String loginIdentifier;

    private String password;

    public LoginRequest(String loginIdentifier, String password) {
        this.loginIdentifier = loginIdentifier;
        this.password = password;
    }
}
