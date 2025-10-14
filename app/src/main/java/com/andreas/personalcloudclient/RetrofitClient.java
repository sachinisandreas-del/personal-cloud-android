package com.andreas.personalcloudclient;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    public static final String BASE_URL = "http://192.168.100.51:5000/";

    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build();

            retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        }
        return retrofit;
    }

    public static ApiService getDownloadApiService(DownloadProgressListener listener) {
        OkHttpClient downloadClient = new OkHttpClient.Builder()
            .addInterceptor(new DownloadProgressInterceptor(listener)) // This line will now work
            .build();

        Retrofit downloadRetrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(downloadClient)
            .build();

        return downloadRetrofit.create(ApiService.class);
    }
}
