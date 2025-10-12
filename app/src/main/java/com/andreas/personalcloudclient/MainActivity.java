package com.andreas.personalcloudclient; // Make sure this matches your package name

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// 1. IMPLEMENT THE INTERFACE
// This tells Java that MainActivity promises to handle the click events defined in our adapter.
public class MainActivity extends AppCompatActivity implements FileAdapter.OnFileClickListener {

    private static final String TAG = "MainActivity";

    // UI Components
    private RecyclerView recyclerView;
    private FileAdapter fileAdapter;
    private Button selectFileButton;
    private ProgressBar progressBar;
    private TextView emptyTextView;

    // API Service
    private ApiService apiService;

    // Launcher for the file picker
    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent data = result.getData();
                if (data != null) {
                    Uri fileUri = data.getData();
                    uploadFile(fileUri);
                }
            } else {
                Toast.makeText(this, "File selection cancelled", Toast.LENGTH_SHORT).show();
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize API Service
        apiService = RetrofitClient.getClient().create(ApiService.class);

        // Find views by their IDs
        selectFileButton = findViewById(R.id.buttonSelectFile);
        progressBar = findViewById(R.id.progressBar);
        emptyTextView = findViewById(R.id.textViewEmpty);
        recyclerView = findViewById(R.id.recyclerViewFiles);

        // Setup RecyclerView (this now includes setting the listener)
        setupRecyclerView();

        // Setup Listeners for the upload button
        selectFileButton.setOnClickListener(v -> openFilePicker());

        // Initial fetch of the file list from the server
        fetchFileList();
    }

    private void setupRecyclerView() {
        fileAdapter = new FileAdapter();
        recyclerView.setAdapter(fileAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 2. SET THE LISTENER
        // This is the crucial connection. We are telling the adapter,
        // "When a click happens, notify me (this activity)."
        fileAdapter.setOnFileClickListener(this);
    }

    // 3. FULFILL THE PROMISE
    // Because we implemented the interface, we MUST provide this method.
    // This is the code that will run when the options button on any list item is clicked.
    @Override
    public void onFileOptionsClicked(String filename) {
        // For now, we just show a message to confirm it's working.
        Toast.makeText(this, "Options clicked for: " + filename, Toast.LENGTH_SHORT).show();
        // Our NEXT STEP will be to replace this Toast with a real popup menu.
    }

    private void fetchFileList() {
        showLoading(true);
        Call<List<String>> call = apiService.getFiles();

        call.enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<String> files = response.body();
                    if (files.isEmpty()) {
                        showEmptyView(true);
                    } else {
                        showEmptyView(false);
                        fileAdapter.setFiles(files);
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Failed to fetch files", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<String>> call, Throwable t) {
                showLoading(false);
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void uploadFile(Uri fileUri) {
        File file = createTempFileFromUri(fileUri);
        if (file == null) {
            Toast.makeText(this, "Failed to read file", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody requestFile = RequestBody.create(MediaType.parse(getContentResolver().getType(fileUri)), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        showLoading(true);
        Call<UploadResponse> call = apiService.uploadFile(body);

        call.enqueue(new Callback<UploadResponse>() {
            @Override
            public void onResponse(Call<UploadResponse> call, Response<UploadResponse> response) {
                showLoading(false);
                if (response.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Upload successful!", Toast.LENGTH_SHORT).show();
                    fetchFileList(); // Refresh the file list after a successful upload
                } else {
                    Toast.makeText(MainActivity.this, "Upload failed. Code: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UploadResponse> call, Throwable t) {
                showLoading(false);
                Toast.makeText(MainActivity.this, "Upload error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // --- UI Helper Methods ---
    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            emptyTextView.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showEmptyView(boolean show) {
        if (show) {
            emptyTextView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyTextView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(intent);
    }

    // --- File Helper Method ---
    private File createTempFileFromUri(Uri uri) {
        File tempFile = null;
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                tempFile = new File(getCacheDir(), "upload_temp_file");
                try (OutputStream outputStream = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[4 * 1024];
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
