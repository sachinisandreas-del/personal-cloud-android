package com.andreas.personalcloudclient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private boolean isLoginMode = true;

    // View References
    private TextInputLayout textInputLayoutUsername;
    private TextInputEditText editTextUsername; // Need a direct reference to get text
    private TextInputLayout textInputLayoutPassword;
    private TextInputEditText editTextPassword;
    private TextInputLayout textInputLayoutConfirmPassword;
    private TextInputEditText editTextConfirmPassword;
    private Button buttonAction;
    private MaterialButton buttonSignInWithGoogle;
    private TextView textViewSwitchMode;
    private ProgressBar progressBar;

    // ViewModel Reference
    private LoginViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // --- Get the ViewModel instance ---
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        // --- Find all the views ---
        textInputLayoutUsername = findViewById(R.id.textInputLayoutUsername);
        editTextUsername = findViewById(R.id.editTextUsername);
        textInputLayoutPassword = findViewById(R.id.textInputLayoutPassword);
        editTextPassword = findViewById(R.id.editTextPassword);
        textInputLayoutConfirmPassword = findViewById(R.id.textInputLayoutConfirmPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonAction = findViewById(R.id.buttonLogin);
        buttonSignInWithGoogle = findViewById(R.id.buttonSignInWithGoogle);
        textViewSwitchMode = findViewById(R.id.textViewSwitchMode);
        progressBar = findViewById(R.id.progressBarLogin);

        // --- Set up the click listeners ---
        buttonAction.setOnClickListener(v -> {
            // Get the text from the input fields.
            String username = editTextUsername.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();

            if (isLoginMode) {
                // In login mode, call the login method in the ViewModel.
                // We use the 'username' field for the email, as the hint suggests.
                viewModel.login(username, password);
            } else {
                // In register mode, get the confirm password text as well.
                String confirmPassword = editTextConfirmPassword.getText().toString().trim();
                // Call the register method in the ViewModel.
                viewModel.register(username, username, password, confirmPassword); // Sending username for both user/email for now
            }
        });

        buttonSignInWithGoogle.setOnClickListener(v -> {
            // TODO: Call the Google Sign-In method (will be implemented in a later step).
            Toast.makeText(this, "Google Sign-In coming soon!", Toast.LENGTH_SHORT).show();
        });

        textViewSwitchMode.setOnClickListener(v -> {
            toggleMode();
        });

        // --- Set up observers for LiveData ---
        setupObservers();
    }

    private void setupObservers() {
        // Observer for the loading state.
        viewModel.isLoading.observe(this, isLoading -> {
            if (isLoading != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                // Disable buttons while loading to prevent multiple clicks.
                buttonAction.setEnabled(!isLoading);
                buttonSignInWithGoogle.setEnabled(!isLoading);
            }
        });

        // Observer for toast messages.
        viewModel.toastMessage.observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                // Signal to the ViewModel that the message has been shown.
                viewModel.onToastMessageShown();
            }
        });

        // Observer for the login success event.
        viewModel.loginSuccess.observe(this, authResponse -> {
            if (authResponse != null) {
                // Login was successful!
                Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show();

                // Get the singleton instance of SessionManager and save the tokens.
                SessionManager.getInstance().saveTokens(authResponse);

                // Navigate to the MainActivity.
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                // Clear the activity stack so the user can't press "back" to get to the login screen.
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish(); // Close the LoginActivity.
            }
        });
    }

    private void toggleMode() {
        isLoginMode = !isLoginMode;

        if (isLoginMode) {
            // UI changes for LOGIN mode
            textInputLayoutUsername.setHint("Username or Email");
            textInputLayoutConfirmPassword.setVisibility(View.GONE);
            buttonAction.setText("Log In");
            textViewSwitchMode.setText("Don't have an account? Sign Up");
        } else {
            // UI changes for REGISTER mode
            // In register mode, the first field must be the username.
            textInputLayoutUsername.setHint("Username");
            // We need a separate email field in a real app, but for now we'll reuse.
            // A better UI would have three fields in register mode.
            textInputLayoutConfirmPassword.setVisibility(View.VISIBLE);
            buttonAction.setText("Register");
            textViewSwitchMode.setText("Already have an account? Sign In");
        }
    }
}
