package com.andreas.personalcloudclient;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

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

    private LoginViewModel viewModel;

    // --- Google Sign-In Components ---
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        // --- Configure Google Sign-In ---
        configureGoogleSignIn();

        // --- Register the ActivityResultLauncher for Google Sign-In ---
        googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // The Task returned from this call is always completed, no need to attach a listener.
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    handleGoogleSignInResult(task);
                } else {
                    Toast.makeText(this, "Google Sign In cancelled.", Toast.LENGTH_SHORT).show();
                }
            });

        // --- Find views ---
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

        // --- Set up click listeners ---
        buttonAction.setOnClickListener(v -> handleAuthAction());

        buttonSignInWithGoogle.setOnClickListener(v -> {
            // --- Start the Google Sign-In flow ---
            signInWithGoogle();
        });

        textViewSwitchMode.setOnClickListener(v -> toggleMode());

        setupObservers();

        // Initial UI state setup
        toggleMode();
        isLoginMode = false;
        toggleMode();
    }

    private void configureGoogleSignIn() {
        // Configure Google Sign-In to request the user's ID, email address, and basic profile.
        // ID and email are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            // We request an ID token. The string passed here is your *Web application's*
            // client ID from strings.xml. This is the crucial step that sets the 'audience'.
            .requestIdToken(getString(R.string.google_web_client_id))
            .requestEmail()
            .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void signInWithGoogle() {
        // Launch the Google Sign-In activity.
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            // Signed in successfully, get the ID token.
            String idToken = account.getIdToken();
            if (idToken != null) {
                Log.d(TAG, "Google ID Token: " + idToken);
                // Send the token to your ViewModel to be verified by your backend.
                viewModel.loginWithGoogle(idToken);
            } else {
                Toast.makeText(this, "Failed to get Google ID token.", Toast.LENGTH_LONG).show();
            }

        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            Toast.makeText(this, "Google Sign In failed. Code: " + e.getStatusCode(), Toast.LENGTH_LONG).show();
        }
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
