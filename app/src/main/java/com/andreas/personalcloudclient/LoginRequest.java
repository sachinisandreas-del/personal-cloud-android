package com.andreas.personalcloudclient;

// This class represents the JSON body for a traditional login request.
public class LoginRequest {
    private String email;
    private String password;

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
}
