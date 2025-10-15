package com.andreas.personalcloudclient;

import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FileRepository {

    // A simple interface to send results back asynchronously
    public interface RepositoryCallback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    private final ApiService apiService;
    // Our in-memory cache
    private List<FileMetadata> cachedFiles = null;

    public FileRepository() {
        // Get the standard Retrofit service instance
        this.apiService = RetrofitClient.getClient().create(ApiService.class);
    }

    /**
     * Fetches the list of files. It always tries to get the latest from the network.
     */
    public void getFiles(RepositoryCallback<List<FileMetadata>> callback) {
        apiService.getFiles().enqueue(new Callback<List<FileMetadata>>() {
            @Override
            public void onResponse(@NonNull Call<List<FileMetadata>> call, @NonNull Response<List<FileMetadata>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // On success, update the cache and return the data via the callback
                    cachedFiles = response.body();
                    callback.onSuccess(new ArrayList<>(cachedFiles)); // Return a copy
                } else {
                    callback.onError("Failed to fetch files. Code: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<FileMetadata>> call, @NonNull Throwable t) {
                callback.onError("Network Error: " + t.getMessage());
            }
        });
    }

    /**
     * Deletes a file from the server.
     * On success, it also removes the file from the local cache.
     */
    public void deleteFile(String filename, RepositoryCallback<String> callback) {
        apiService.deleteFile(filename).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    // --- CACHE MODIFICATION ---
                    // If the server deletion was successful, remove the file from our local cache.
                    if (cachedFiles != null) {
                        cachedFiles.removeIf(file -> file.getFilename().equals(filename));
                    }
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
}
