package com.andreas.personalcloudclient;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private Button selectFileButton;
    private TextView statusTextView;

    // This is the modern way to handle getting a result from another activity (like the file picker)
    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                // This block runs if the user successfully selected a file.
                Intent data = result.getData();
                if (data != null) {
                    Uri fileUri = data.getData();
                    Log.d(TAG, "File selected: " + fileUri.toString());
                    statusTextView.setText("File selected. Preparing to upload...");

                    uploadFile(fileUri);
                }
            } else {
                // This block runs if the user cancels the file picker.
                Log.d(TAG, "File selection cancelled.");
                statusTextView.setText("File selection cancelled.");
            }
        });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Find the UI elements from the XML layout using their IDs
        selectFileButton = findViewById(R.id.buttonSelectFile);
        statusTextView = findViewById(R.id.textViewStatus);

        // 2. Set an OnClickListener for our button
        selectFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // This is what happens when the button is clicked.
                Log.d(TAG, "Select file button clicked.");
                openFilePicker();
            }
        });
    }

    private void openFilePicker() {
        // 3. Create an Intent to open the system's file picker
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // Allow all file types to be selected
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // 4. Launch the file picker and wait for the result
        filePickerLauncher.launch(intent);
    }

    private void uploadFile(Uri fileUri) {
        // Create a file object from the Uri
        File file = createTempFileFromUri(fileUri);
        if (file == null) {
            statusTextView.setText("Error: Could not read file");
            return;
        }

        // Create a RequestBody instance from the file
        RequestBody requestFile = RequestBody.create(MediaType.parse(getContentResolver().getType(fileUri)), file);

        // Create a MultipartBody.Part instance using the RequestBody
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        // Get our ApiService instance from the RetrofitClient
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);

        // Create the call
        Call<UploadResponse> call = apiService.uploadFile(body);

        // Execute the call asynchronously
        call.enqueue(new Callback<UploadResponse>() {
            @Override
            public void onResponse(Call<UploadResponse> call, Response<UploadResponse> response) {
                // This block is executed when a response is received from the server
                if (response.isSuccessful() && response.body() != null) {
                    // Server returned a success response
                    UploadResponse uploadResponse = response.body();
                    String serverMessage = "Success: " + uploadResponse.getMessage() + "\nFile: " + uploadResponse.getFilename();
                    statusTextView.setText(serverMessage);
                    Log.d(TAG, "Upload success: " + serverMessage);
                } else {
                    // Server returned an error response
                    String errorMessage = "Upload failed: Server returned an error. Code: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            errorMessage += "\n" + response.errorBody().string();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error parsing error body", e);
                    }
                    statusTextView.setText(errorMessage);
                    Log.e(TAG, errorMessage);
                }
            }

            @Override
            public void onFailure(Call<UploadResponse> call, Throwable t) {
                // This block is executed when the network request itself fails (e.g., no internet)
                String errorMessage = "Upload failed: " + t.getMessage();
                statusTextView.setText(errorMessage);
                Log.e(TAG, "Upload failure", t);
            }
        });
    }

    /**
     * Helper method to copy the content from a Uri into a temporary local file.
     * This is necessary because Retrofit works best with File objects.
     */
    private File createTempFileFromUri(Uri uri) {
        File tempFile = null;
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                // We'll create a temporary file in the app's cache directory
                tempFile = new File(getCacheDir(), "upload_temp_file");
                try (OutputStream outputStream = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[4 * 1024]; // 4k buffer
                    int read;
                    while ((read = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, read);
                    }
                    outputStream.flush();
                }
                inputStream.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to create temp file from Uri", e);
            return null;
        }
        return tempFile;
    }

}
