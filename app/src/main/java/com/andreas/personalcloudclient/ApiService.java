package com.andreas.personalcloudclient;

import java.util.List;
import okhttp3.MultipartBody;
import okhttp3.ResponseBody; // <-- NEW: Import for file streams
import retrofit2.Call;
import retrofit2.http.DELETE; // <-- NEW
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;     // <-- NEW
import retrofit2.http.Streaming; // <-- NEW
import okhttp3.RequestBody;
import retrofit2.http.Body;
public interface ApiService {
    @POST("upload")
    Call<UploadResponse> uploadFile(@Body RequestBody body);

    @Multipart
    @POST("upload")
    Call<UploadResponse> uploadFile(@Part MultipartBody.Part file);

    @GET("files")
    Call<List<FileMetadata>> getFiles();

    // --- NEW: Endpoint to download a file ---
    @GET("download/{filename}")
    @Streaming // Crucial for downloading files efficiently
    Call<ResponseBody> downloadFile(@Path("filename") String filename);

    // --- NEW: Endpoint to delete a file ---
    @DELETE("delete/{filename}")
    Call<ResponseBody> deleteFile(@Path("filename") String filename);
}
