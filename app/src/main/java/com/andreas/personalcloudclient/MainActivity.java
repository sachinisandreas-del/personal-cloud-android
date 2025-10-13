package com.andreas.personalcloudclient; // Make sure this matches your package name

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
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
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements FileAdapter.OnFileClickListener {

    private static final String TAG = "MainActivity";

    // UI Components & API Service
    private RecyclerView recyclerView;
    private FileAdapter fileAdapter;
    private Button selectFileButton;
    private ProgressBar progressBar;
    private TextView emptyTextView;
    private ApiService apiService;

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                uploadFile(result.getData().getData());
            } else {
                Toast.makeText(this, "File selection cancelled", Toast.LENGTH_SHORT).show();
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        apiService = RetrofitClient.getClient().create(ApiService.class);
        selectFileButton = findViewById(R.id.buttonSelectFile);
        progressBar = findViewById(R.id.progressBar);
        emptyTextView = findViewById(R.id.textViewEmpty);
        recyclerView = findViewById(R.id.recyclerViewFiles);
        setupRecyclerView();
        selectFileButton.setOnClickListener(v -> openFilePicker());

        fetchFileList();
    }

    private void setupRecyclerView() {
        fileAdapter = new FileAdapter();
        recyclerView.setAdapter(fileAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        fileAdapter.setOnFileClickListener(this);
    }

    @Override
    public void onFileOptionsClicked(String filename) {
        View view = findViewByFileName(filename);
        if (view == null) return;

        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.file_options_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_download) {
                downloadFileFromServer(filename);
                return true;
            } else if (itemId == R.id.action_delete) {
                new AlertDialog.Builder(this)
                    .setTitle("Delete File")
                    .setMessage("Are you sure you want to delete '" + filename + "'?")
                    .setPositiveButton("Delete", (dialog, which) -> deleteFileFromServer(filename))
                    .setNegativeButton("Cancel", null)
                    .show();
                return true;
            }
            return false;
        });
        popup.show();
    }

    // --- API Call Methods ---

    private void fetchFileList() {
        showLoading(true);
        Call<List<String>> call = apiService.getFiles();
        call.enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(@NonNull Call<List<String>> call, @NonNull Response<List<String>> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    fileAdapter.setFiles(response.body());
                    showEmptyView(response.body().isEmpty());
                } else {
                    Toast.makeText(MainActivity.this, "Failed to fetch files. Check server.", Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<String>> call, @NonNull Throwable t) {
                showLoading(false);
                Toast.makeText(MainActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void uploadFile(Uri fileUri) {
        File tempFile = createTempFileFromUri(fileUri);
        if (tempFile == null) {
            Toast.makeText(this, "Failed to read file", Toast.LENGTH_SHORT).show();
            return;
        }

        String originalFileName = getFileNameFromUri(fileUri);
        RequestBody requestFile = RequestBody.create(MediaType.parse(getContentResolver().getType(fileUri)), tempFile);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", originalFileName, requestFile);

        showLoading(true);
        Call<UploadResponse> call = apiService.uploadFile(body);
        call.enqueue(new Callback<UploadResponse>() {
            @Override
            public void onResponse(@NonNull Call<UploadResponse> call, @NonNull Response<UploadResponse> response) {
                fetchFileList();
                if (response.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Upload successful!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Upload failed. Code: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(@NonNull Call<UploadResponse> call, @NonNull Throwable t) {
                fetchFileList();
                Toast.makeText(MainActivity.this, "Upload error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void deleteFileFromServer(String filename) {
        showLoading(true);
        Call<ResponseBody> call = apiService.deleteFile(filename);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                fetchFileList();
                if (response.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "'" + filename + "' deleted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Delete failed. Code: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                fetchFileList();
                Toast.makeText(MainActivity.this, "Delete error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void downloadFileFromServer(String filename) {
        Toast.makeText(this, "Starting download...", Toast.LENGTH_SHORT).show();
        Call<ResponseBody> call = apiService.downloadFile(filename);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    new Thread(() -> {
                        boolean success = writeResponseBodyToDisk(response.body(), filename);
                        runOnUiThread(() -> {
                            if (success) {
                                Toast.makeText(MainActivity.this, filename + " downloaded.", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(MainActivity.this, "Failed to save file.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }).start();
                } else {
                    Toast.makeText(MainActivity.this, "Download failed. Code: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Toast.makeText(MainActivity.this, "Download error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // --- MODERN STORAGE & FILE HELPER METHODS (WITH FIX) ---

    private boolean writeResponseBodyToDisk(ResponseBody body, String filename) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);

            // --- THIS IS THE FIX ---
            // We get the MIME type from the filename and add it to the MediaStore record.
            values.put(MediaStore.MediaColumns.MIME_TYPE, getMimeType(filename));
            // ---------------------

            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                Log.e(TAG, "Failed to create new MediaStore record.");
                return false;
            }

            try (OutputStream outputStream = getContentResolver().openOutputStream(uri);
                 InputStream inputStream = body.byteStream()) {

                if (outputStream == null) {
                    Log.e(TAG, "Failed to open output stream for MediaStore Uri.");
                    return false;
                }

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
                return true;
            }

        } catch (IOException e) {
            Log.e(TAG, "Failed to save downloaded file", e);
            return false;
        }
    }

    private String getMimeType(String filename) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(filename);
        if (extension != null) {
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
            // Return a generic stream type if the MIME type is unknown
            return mimeType != null ? mimeType : "application/octet-stream";
        }
        return "application/octet-stream";
    }

    // --- Other Helper Methods (Unchanged) ---

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        if (isLoading) emptyTextView.setVisibility(View.GONE);
    }

    private void showEmptyView(boolean show) {
        emptyTextView.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(intent);
    }

    private View findViewByFileName(String filename) {
        for (int i = 0; i < fileAdapter.getItemCount(); i++) {
            FileAdapter.FileViewHolder holder = (FileAdapter.FileViewHolder) recyclerView.findViewHolderForAdapterPosition(i);
            if (holder != null && holder.fileNameTextView.getText().toString().equals(filename)) {
                return holder.optionsButton;
            }
        }
        return null;
    }

    private String getFileNameFromUri(Uri uri) {
        String fileName = null;
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex);
                }
            }
        }
        return (fileName != null) ? fileName : "upload_" + System.currentTimeMillis();
    }

    private File createTempFileFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if(inputStream == null) return null;
            File tempFile = new File(getCacheDir(), "upload_temp.tmp");
            try (OutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
                outputStream.flush();
            }
            inputStream.close();
            return tempFile;
        } catch (IOException e) {
            Log.e(TAG, "Failed to create temp file from Uri", e);
            return null;
        }
    }
}
