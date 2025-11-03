package com.andreas.personalcloudclient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
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
    private TextInputEditText editTextUsername;
    private TextInputLayout textInputLayoutEmail;
    private TextInputEditText editTextEmail;
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
        textInputLayoutEmail = findViewById(R.id.textInputLayoutEmail);
        editTextEmail = findViewById(R.id.editTextEmail);
        textInputLayoutPassword = findViewById(R.id.textInputLayoutPassword);
        editTextPassword = findViewById(R.id.editTextPassword);
        textInputLayoutConfirmPassword = findViewById(R.id.textInputLayoutConfirmPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonAction = findViewById(R.id.buttonLogin);
        buttonSignInWithGoogle = findViewById(R.id.buttonSignInWithGoogle);
        textViewSwitchMode = findViewById(R.id.textViewSwitchMode);
        progressBar = findViewById(R.id.progressBarLogin);

        // --- Set up the click listeners ---
        buttonAction.setOnClickListener(v -> handleAuthAction());

        buttonSignInWithGoogle.setOnClickListener(v -> {
            Toast.makeText(this, "Google Sign-In coming soon!", Toast.LENGTH_SHORT).show();
        });

        textViewSwitchMode.setOnClickListener(v -> {
            toggleMode();
        });

        // --- Set up observers for LiveData ---
        setupObservers();

        // --- Initial UI state setup ---
        toggleMode(); // Call this once to set the initial state correctly
        isLoginMode = false; // so that the first toggle makes it true
        toggleMode();
    }

    private void handleAuthAction() {
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (isLoginMode) {
            // In login mode, the first field is for the email.
            viewModel.login(username, password);
        } else {
            // In register mode, get the text from all three fields.
            String email = editTextEmail.getText().toString().trim();
            String confirmPassword = editTextConfirmPassword.getText().toString().trim();

            // Call the register method with the correct variables.
            viewModel.register(username, email, password, confirmPassword);
        }
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
                Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show();

                SessionManager.getInstance().saveTokens(authResponse);

                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private void toggleMode() {
        isLoginMode = !isLoginMode;

        textInputLayoutUsername.setError(null);
        textInputLayoutEmail.setError(null);
        textInputLayoutPassword.setError(null);
        textInputLayoutConfirmPassword.setError(null);

        if (isLoginMode) {
            // --- THIS IS THE KEY UI CHANGE ---
            // UI for LOGIN mode
            textInputLayoutUsername.setHint("Username or Email"); // New Hint
            editTextUsername.setInputType(android.text.InputType.TYPE_CLASS_TEXT); // Use generic text
            textInputLayoutEmail.setVisibility(View.GONE);
            textInputLayoutConfirmPassword.setVisibility(View.GONE);
            buttonAction.setText("Log In");
            textViewSwitchMode.setText("Don't have an account? Sign Up");
        } else {
            // UI for REGISTER mode
            textInputLayoutUsername.setHint("Username");
            editTextUsername.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
            textInputLayoutEmail.setVisibility(View.VISIBLE);
            textInputLayoutConfirmPassword.setVisibility(View.VISIBLE);
            buttonAction.setText("Register");
            textViewSwitchMode.setText("Already have an account? Sign In");
        }
    }
}
