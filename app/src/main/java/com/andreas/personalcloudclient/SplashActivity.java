package com.andreas.personalcloudclient;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

// This activity has no UI. Its only job is to decide where to go.
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the SessionManager instance.
        SessionManager sessionManager = SessionManager.getInstance();

        Intent intent;
        // Check if there is a saved access token.
        if (sessionManager.getAccessToken() != null) {
            // If a token exists, the user is "logged in". Go to MainActivity.
            intent = new Intent(SplashActivity.this, MainActivity.class);
        } else {
            // If no token exists, the user is "logged out". Go to LoginActivity.
            intent = new Intent(SplashActivity.this, LoginActivity.class);
        }

        // Set flags to clear the back stack.
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        // IMPORTANT: Finish the SplashActivity so the user can't press "back" to get to it.
        finish();
    }
}
