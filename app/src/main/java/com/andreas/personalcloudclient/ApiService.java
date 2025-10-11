package com.andreas.personalcloudclient;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ApiService {

    // This defines our '/upload' endpoint
    @Multipart
    @POST("upload") // The path of the endpoint (relative to the BASE_URL)
    Call<UploadResponse> uploadFile(@Part MultipartBody.Part file);

}
