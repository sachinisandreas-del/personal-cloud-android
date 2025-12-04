package com.andreas.personalcloudclient;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Streaming;

public interface ApiService {

    // --- AUTHENTICATION ENDPOINTS ---

    @POST("register")
    Call<ResponseBody> register(@Body RegisterRequest registerRequest);

    @POST("login")
    Call<AuthResponse> login(@Body LoginRequest loginRequest);

    // We send the Google token in a simple Map.
    @POST("login/google")
    Call<AuthResponse> loginWithGoogle(@Body Map<String, String> googleToken);

    @POST("token/refresh")
    Call<AuthResponse> refreshToken(@Body Map<String, String> refreshToken);


    // --- FILE OPERATION ENDPOINTS ---
    // All of these require an Authorization header.

    @GET("files")
    Call<List<FileMetadata>> getFiles();

    @POST("upload")
    Call<UploadResponse> uploadFile(@Body RequestBody body);

    @GET("download/{filename}")
    @Streaming
    Call<ResponseBody> downloadFile(@Path("filename") String filename);

    @DELETE("delete/{filename}")
    Call<ResponseBody> deleteFile(@Path("filename") String filename);
}
