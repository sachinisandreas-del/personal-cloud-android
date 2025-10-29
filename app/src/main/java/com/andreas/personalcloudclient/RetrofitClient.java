package com.andreas.personalcloudclient;

import android.content.Context;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    public static final String BASE_URL = "http://192.168.100.51:5000/"; // Make sure this is your server's IP

    private static Retrofit retrofit = null;

    // The getClient method now requires a Context.
    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            // --- Interceptor Setup ---
            // 1. Logging Interceptor (for debugging)
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            // 2. Authentication Interceptor (our new class)
            AuthInterceptor authInterceptor = new AuthInterceptor(context);


            // --- Build the OkHttpClient ---
            OkHttpClient client = new OkHttpClient.Builder()
                // The order of interceptors can matter.
                // Add the AuthInterceptor first.
                .addInterceptor(authInterceptor)
                // Add the LoggingInterceptor second to see the final request with the header.
                .addInterceptor(loggingInterceptor)
                .build();

            // --- Build the Retrofit Instance ---
            retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client) // Use our new client with the interceptors
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        }
        return retrofit;
    }

    // The download client does not need the AuthInterceptor because we will pass the
    // token manually for now. We will update this later.
    public static ApiService getDownloadApiService(DownloadProgressListener listener) {
        OkHttpClient downloadClient = new OkHttpClient.Builder()
            .addInterceptor(new DownloadProgressInterceptor(listener))
            .build();

        Retrofit downloadRetrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(downloadClient)
            .build();

        return downloadRetrofit.create(ApiService.class);
    }
}
