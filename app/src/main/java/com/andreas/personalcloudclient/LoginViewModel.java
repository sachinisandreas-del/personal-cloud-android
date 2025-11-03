package com.andreas.personalcloudclient;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import okhttp3.ResponseBody;

public class LoginViewModel extends AndroidViewModel {

    private static final String TAG = "LoginViewModel";

    private final AuthRepository authRepository;

    // --- LiveData for UI State ---

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public final LiveData<Boolean> isLoading = _isLoading;

    // The corrected line is here:
    private final MutableLiveData<String> _toastMessage = new MutableLiveData<>();
    public final LiveData<String> toastMessage = _toastMessage;

    private final MutableLiveData<AuthResponse> _loginSuccess = new MutableLiveData<>();
    public final LiveData<AuthResponse> loginSuccess = _loginSuccess;

    // --- Constructor ---
    public LoginViewModel(@NonNull Application application) {
        super(application);
        // Pass the application context to the repository.
        this.authRepository = new AuthRepository(application);
    }


    // --- Public Methods Called by LoginActivity ---

    public void login(String loginIdentifier, String password) {
        if (loginIdentifier.isEmpty() || password.isEmpty()) {
            _toastMessage.setValue("Identifier and password cannot be empty.");
            return;
        }
        _isLoading.setValue(true);
        // Pass the identifier to the repository.
        authRepository.login(loginIdentifier, password, new AuthRepository.AuthCallback<AuthResponse>() {
            // ... the rest of the method is exactly the same ...
            @Override
            public void onSuccess(AuthResponse response) {
                _isLoading.setValue(false);
                _loginSuccess.setValue(response);
                Log.d(TAG, "Login successful. Access Token: " + (response != null ? response.getAccessToken() : "null"));
            }

            @Override
            public void onError(String message) {
                _isLoading.setValue(false);
                _toastMessage.setValue(message);
                Log.e(TAG, "Login error: " + message);
            }
        });
    }

    public void register(String username, String email, String password, String confirmPassword) {
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            _toastMessage.setValue("Username, email, and password cannot be empty.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            _toastMessage.setValue("Passwords do not match.");
            return;
        }

        _isLoading.setValue(true);

        authRepository.register(username, email, password, new AuthRepository.AuthCallback<ResponseBody>() {
            @Override
            public void onSuccess(ResponseBody response) {
                _isLoading.setValue(false);
                _toastMessage.setValue("Registration successful! Please log in.");
                Log.d(TAG, "Registration successful.");
            }

            @Override
            public void onError(String message) {
                _isLoading.setValue(false);
                _toastMessage.setValue(message);
                Log.e(TAG, "Registration error: " + message);
            }
        });
    }

    public void onToastMessageShown() {
        _toastMessage.setValue(null);
    }
}
