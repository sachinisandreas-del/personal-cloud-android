package com.andreas.personalcloudclient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import java.util.Set;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements FileAdapter.OnFileClickListener {

    private static final String TAG = "MainActivity";
    private static final String DOWNLOAD_CHANNEL_ID = "personal_cloud_downloads";
    private static final int NOTIFICATION_PERMISSION_CODE = 102;

    private FileListViewModel viewModel;
    private FileAdapter fileAdapter;

    private RecyclerView recyclerView;
    private Button selectFileButton;
    private ProgressBar progressBar;
    private TextView emptyTextView;

    private ActionMode actionMode;

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                uploadFile(result.getData().getData());
            } else {
                Toast.makeText(this, "File selection cancelled", Toast.LENGTH_SHORT).show();
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(FileListViewModel.class);

        createNotificationChannel();
        requestNotificationPermission(); // This method is now included
        selectFileButton = findViewById(R.id.buttonSelectFile);
        progressBar = findViewById(R.id.progressBar);
        emptyTextView = findViewById(R.id.textViewEmpty);
        recyclerView = findViewById(R.id.recyclerViewFiles);
        setupRecyclerView(); // This method is now included
        selectFileButton.setOnClickListener(v -> openFilePicker());

        setupObservers();

        viewModel.loadFileList();
    }

    private void setupObservers() {
        viewModel.fileList.observe(this, files -> {
            if (files != null) {
                fileAdapter.setFiles(files);
                showEmptyView(files.isEmpty());
            }
        });

        viewModel.isLoading.observe(this, isLoading -> {
            if (isLoading != null) {
                showLoading(isLoading);
            }
        });

        viewModel.toastMessage.observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                viewModel.onToastMessageShown();
            }
        });

        viewModel.selectedItems.observe(this, selection -> {
            if (selection != null) {
                fileAdapter.setSelectionMode(selection);
                if (actionMode != null) {
                    actionMode.setTitle(selection.size() + " selected");
                    actionMode.invalidate();
                }
            }
        });

        viewModel.isSelectionModeActive.observe(this, isActive -> {
            if (isActive != null && !isActive && actionMode != null) {
                actionMode.finish();
                actionMode = null;
            }
        });
    }

    @Override
    public void onFileClicked(FileMetadata file) {
        if (viewModel.isSelectionModeActive.getValue() != null && viewModel.isSelectionModeActive.getValue()) {
            viewModel.toggleSelection(file.getFilename());
        } else {
            viewFile(file);
        }
    }

    @Override
    public void onFileLongClicked(FileMetadata file) {
        if (actionMode == null) {
            actionMode = startActionMode(actionModeCallback);
        }
        viewModel.toggleSelection(file.getFilename());
    }

    private void viewFile(FileMetadata file) {
        String filename = file.getFilename();
        String fileType = file.getFileType();
        String fileUrl = RetrofitClient.BASE_URL + "download/" + filename;

        if ("image".equals(fileType)) {
            Intent intent = new Intent(this, ImageViewerActivity.class);
            intent.putExtra(ImageViewerActivity.EXTRA_IMAGE_URL, fileUrl);
            startActivity(intent);
        } else {
            String streamUrl = fileUrl + "?view=true";
            Intent intent = new Intent(Intent.ACTION_VIEW);
            String mimeType = getMimeType(filename);
            intent.setDataAndType(Uri.parse(streamUrl), mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "No app found to open this file type", Toast.LENGTH_LONG).show();
            }
        }
    }

    private ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.contextual_action_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int itemId = item.getItemId();
            if (itemId == R.id.action_delete_contextual) {
                viewModel.deleteSelectedFiles();
                return true;
            } else if (itemId == R.id.action_download_contextual) {
                Set<String> selectedFiles = viewModel.selectedItems.getValue();
                if(selectedFiles != null){
                    for(String filename : selectedFiles){
                        downloadFileFromServer(filename);
                    }
                }
                mode.finish();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            viewModel.clearSelection();
            actionMode = null;
        }
    };

    // --- ALL THE HELPER AND API METHODS ARE NOW INCLUDED ---

    private void setupRecyclerView() {
        fileAdapter = new FileAdapter();
        recyclerView.setAdapter(fileAdapter);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        fileAdapter.setOnFileClickListener(this);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    private void uploadFile(Uri fileUri) {
        File tempFile = createTempFileFromUri(fileUri);
        if (tempFile == null) return;

        final int notificationId = (int) System.currentTimeMillis();
        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, DOWNLOAD_CHANNEL_ID)
            .setContentTitle(getFileNameFromUri(fileUri))
            .setContentText("Upload starting...")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true);

        ProgressRequestBody.ProgressListener progressListener = (bytesUploaded, totalBytes) -> {
            int progress = (int) ((100 * bytesUploaded) / totalBytes);
            builder.setProgress(100, progress, false).setContentText(progress + "%");
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(notificationId, builder.build());
            }
        };

        ProgressRequestBody requestBody = new ProgressRequestBody(tempFile, progressListener);
        MultipartBody multipartBody = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", getFileNameFromUri(fileUri), requestBody)
            .build();

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(notificationId, builder.build());
        }

        apiService.uploadFile(multipartBody).enqueue(new Callback<UploadResponse>() {
            @Override
            public void onResponse(@NonNull Call<UploadResponse> call, @NonNull Response<UploadResponse> response) {
                viewModel.loadFileList();
                builder.setOngoing(false);
                if (response.isSuccessful()) {
                    builder.setContentText("Upload complete").setProgress(0, 0, false).setSmallIcon(android.R.drawable.stat_sys_upload_done);
                } else {
                    builder.setContentText("Upload failed").setProgress(0, 0, false);
                }
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    notificationManager.notify(notificationId, builder.build());
                }
            }
            @Override
            public void onFailure(@NonNull Call<UploadResponse> call, @NonNull Throwable t) {
                viewModel.loadFileList();
                builder.setOngoing(false).setContentText("Upload failed: " + t.getMessage()).setProgress(0, 0, false);
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    notificationManager.notify(notificationId, builder.build());
                }
            }
        });
    }

    private void downloadFileFromServer(String filename) {
        final int notificationId = (int) System.currentTimeMillis();
        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, DOWNLOAD_CHANNEL_ID)
            .setContentTitle(filename)
            .setContentText("Download starting...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true);

        DownloadProgressListener progressListener = progress -> {
            builder.setProgress(100, progress, false).setContentText(progress + "%");
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(notificationId, builder.build());
            }
        };

        ApiService downloadService = RetrofitClient.getDownloadApiService(progressListener);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(notificationId, builder.build());
        }

        downloadService.downloadFile(filename).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    new Thread(() -> {
                        boolean success = writeResponseBodyToDisk(response.body(), filename);
                        new Handler(Looper.getMainLooper()).post(() -> {
                            builder.setOngoing(false);
                            if (success) {
                                builder.setContentText("Download complete").setProgress(0, 0, false).setSmallIcon(android.R.drawable.stat_sys_download_done);
                            } else {
                                builder.setContentText("Download failed: Error writing file").setProgress(0, 0, false);
                            }
                            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                                notificationManager.notify(notificationId, builder.build());
                            }
                        });
                    }).start();
                } else {
                    builder.setOngoing(false).setContentText("Download failed: " + response.code()).setProgress(0, 0, false);
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                        notificationManager.notify(notificationId, builder.build());
                    }
                }
            }
            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                builder.setOngoing(false).setContentText("Download failed: " + t.getMessage()).setProgress(0, 0, false);
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    notificationManager.notify(notificationId, builder.build());
                }
            }
        });
    }

    private boolean writeResponseBodyToDisk(ResponseBody body, String filename) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
            values.put(MediaStore.MediaColumns.MIME_TYPE, getMimeType(filename));
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) return false;
            try (OutputStream outputStream = getContentResolver().openOutputStream(uri);
                 InputStream inputStream = body.byteStream()) {
                if (outputStream == null) return false;
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

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Download & Upload Notifications";
            String description = "Shows progress of file transfers";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(DOWNLOAD_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private String getMimeType(String filename) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(filename);
        if (extension != null) {
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
            return mimeType != null ? mimeType : "application/octet-stream";
        }
        return "application/octet-stream";
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void showEmptyView(boolean show) {
        if(show){
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
