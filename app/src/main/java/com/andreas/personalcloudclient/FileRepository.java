package com.andreas.personalcloudclient;

import android.Manifest;
import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.webkit.MimeTypeMap;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FileRepository {

    private static final String TAG = "FileRepository";

    public interface GetFilesCallback {
        void onCacheLoaded(List<FileMetadata> cachedFiles);
        void onNetworkResult(List<FileMetadata> networkFiles, String error);
    }

    public interface RepositoryCallback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    private final Context context;
    private final ApiService apiService;
    private final FileDao fileDao;
    private final ExecutorService executor;

    public FileRepository(Application application) {
        this.context = application.getApplicationContext();
        AppDatabase database = AppDatabase.getDatabase(application);
        this.fileDao = database.fileDao();
        this.apiService = RetrofitClient.getClient().create(ApiService.class);
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void getFiles(GetFilesCallback callback) {
        executor.execute(() -> {
            List<FileMetadata> cachedFiles = fileDao.getAllFiles();
            new Handler(Looper.getMainLooper()).post(() -> callback.onCacheLoaded(cachedFiles));
        });

        if (!isNetworkAvailable()) {
            new Handler(Looper.getMainLooper()).post(() -> callback.onNetworkResult(null, "Offline: Showing cached files."));
            return;
        }

        apiService.getFiles().enqueue(new Callback<List<FileMetadata>>() {
            @Override
            public void onResponse(@NonNull Call<List<FileMetadata>> call, @NonNull Response<List<FileMetadata>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<FileMetadata> networkFiles = response.body();
                    executor.execute(() -> {
                        fileDao.deleteAll();
                        fileDao.insertAll(networkFiles);
                    });
                    callback.onNetworkResult(networkFiles, null);
                } else {
                    callback.onNetworkResult(null, "Failed to fetch files. Code: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<FileMetadata>> call, @NonNull Throwable t) {
                callback.onNetworkResult(null, "Network Error: " + t.getMessage());
            }
        });
    }

    public void deleteFile(String filename, RepositoryCallback<String> callback) {
        if (!isNetworkAvailable()) {
            callback.onError("Offline: Cannot delete file.");
            return;
        }
        apiService.deleteFile(filename).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    executor.execute(() -> fileDao.deleteFileByFilename(filename)); // Update database
                    callback.onSuccess(filename + " deleted successfully.");
                } else {
                    callback.onError("Delete failed. Code: " + response.code());
                }
            }
            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                callback.onError("Delete error: " + t.getMessage());
            }
        });
    }

    public void uploadFile(Uri fileUri, RepositoryCallback<String> callback) {
        if (!isNetworkAvailable()) {
            callback.onError("Offline: Cannot upload file.");
            return;
        }

        File tempFile = createTempFileFromUri(fileUri);
        if (tempFile == null) {
            callback.onError("Failed to read file for upload.");
            return;
        }

        String originalFileName = getFileNameFromUri(fileUri);
        final int notificationId = (int) System.currentTimeMillis();
        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "personal_cloud_downloads")
            .setContentTitle(originalFileName)
            .setContentText("Upload starting...")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true);

        ProgressRequestBody.ProgressListener progressListener = (bytesUploaded, totalBytes) -> {
            int progress = (int) ((100 * bytesUploaded) / totalBytes);
            builder.setProgress(100, progress, false).setContentText(progress + "%");
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(notificationId, builder.build());
            }
        };

        ProgressRequestBody requestBody = new ProgressRequestBody(tempFile, progressListener);
        MultipartBody multipartBody = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", originalFileName, requestBody)
            .build();

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(notificationId, builder.build());
        }

        apiService.uploadFile(multipartBody).enqueue(new Callback<UploadResponse>() {
            @Override
            public void onResponse(@NonNull Call<UploadResponse> call, @NonNull Response<UploadResponse> response) {
                builder.setOngoing(false);
                if (response.isSuccessful()) {
                    builder.setContentText("Upload complete").setProgress(0, 0, false).setSmallIcon(android.R.drawable.stat_sys_upload_done);
                    callback.onSuccess("Upload successful!");
                } else {
                    builder.setContentText("Upload failed").setProgress(0, 0, false);
                    callback.onError("Upload failed. Code: " + response.code());
                }
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    notificationManager.notify(notificationId, builder.build());
                }
            }
            @Override
            public void onFailure(@NonNull Call<UploadResponse> call, @NonNull Throwable t) {
                builder.setOngoing(false).setContentText("Upload failed").setProgress(0, 0, false);
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    notificationManager.notify(notificationId, builder.build());
                }
                callback.onError("Upload error: " + t.getMessage());
            }
        });
    }

    public void downloadFile(String filename, RepositoryCallback<String> callback) {
        if (!isNetworkAvailable()) {
            callback.onError("Offline: Cannot download file.");
            return;
        }

        final int notificationId = (int) System.currentTimeMillis();
        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "personal_cloud_downloads")
            .setContentTitle(filename)
            .setContentText("Download starting...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true);

        DownloadProgressListener progressListener = progress -> {
            builder.setProgress(100, progress, false).setContentText(progress + "%");
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(notificationId, builder.build());
            }
        };

        ApiService downloadService = RetrofitClient.getDownloadApiService(progressListener);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(notificationId, builder.build());
        }

        downloadService.downloadFile(filename).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                builder.setOngoing(false);
                if (response.isSuccessful() && response.body() != null) {
                    new Thread(() -> {
                        boolean success = writeResponseBodyToDisk(response.body(), filename);
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (success) {
                                builder.setContentText("Download complete").setProgress(0, 0, false).setSmallIcon(android.R.drawable.stat_sys_download_done);
                                callback.onSuccess(filename + " downloaded.");
                            } else {
                                builder.setContentText("Download failed").setProgress(0, 0, false);
                                callback.onError("Failed to save file.");
                            }
                            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                                notificationManager.notify(notificationId, builder.build());
                            }
                        });
                    }).start();
                } else {
                    builder.setContentText("Download failed").setProgress(0, 0, false);
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                        notificationManager.notify(notificationId, builder.build());
                    }
                    callback.onError("Download failed. Code: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                builder.setOngoing(false).setContentText("Download failed").setProgress(0, 0, false);
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    notificationManager.notify(notificationId, builder.build());
                }
                callback.onError("Download error: " + t.getMessage());
            }
        });
    }

    private boolean writeResponseBodyToDisk(ResponseBody body, String filename) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
            values.put(MediaStore.MediaColumns.MIME_TYPE, getMimeType(filename));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            }
            Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) return false;
            try (OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
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

    private String getMimeType(String filename) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(filename);
        if (extension != null) {
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
            return mimeType != null ? mimeType : "application/octet-stream";
        }
        return "application/octet-stream";
    }

    private String getFileNameFromUri(Uri uri) {
        String fileName = null;
        try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
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
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if(inputStream == null) return null;
            File tempFile = new File(context.getCacheDir(), "upload_temp.tmp");
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

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }
}
