package com.andreas.personalcloudclient;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    // IMPORTANT: Replace this with your server's actual IP address
    private static final String BASE_URL = "http://192.168.100.51:5000/";

    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            // Create a logging interceptor to see request and response logs
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            // Create an OkHttpClient and add the interceptor
            OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build();

            // Build the Retrofit instance
            retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client) // Set the custom OkHttpClient
                .addConverterFactory(GsonConverterFactory.create()) // Use Gson for JSON parsing
                .build();
        }
        return retrofit;
    }
}
