package com.andreas.personalcloudclient;

import android.content.Context;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    public static final String BASE_URL = "http://192.168.100.51:5000/"; // Ensure this IP is correct for your network

    // The singleton instance of our main, configured Retrofit object.
    private static Retrofit retrofit = null;

    /**
     * Gets the singleton instance of the main Retrofit client.
     * This client is configured with interceptors for logging and automatic authentication.
     * It also includes an authenticator for automatic token refreshing.
     *
     * @param context The application context.
     * @return The configured Retrofit instance.
     */
    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            // 1. Set up the logging interceptor for debugging network requests.
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            // 2. Set up our custom interceptor to add the auth token to headers.
            AuthInterceptor authInterceptor = new AuthInterceptor(context);

            // 3. Set up our custom authenticator to handle 401 errors and refresh the token.
            TokenAuthenticator tokenAuthenticator = new TokenAuthenticator(context);

            // 4. Build the OkHttpClient, adding all our components.
            OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(authInterceptor)      // Runs first to add the token.
                .addInterceptor(loggingInterceptor)  // Runs second to log the request with the token.
                .authenticator(tokenAuthenticator)   // Runs ONLY if the server returns a 401 error.
                .build();

            // 5. Build the Retrofit instance using our custom client.
            retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        }
        return retrofit;
    }

    /**
     * Gets a specialized ApiService for downloading files with progress listening.
     * Note: This client does NOT have the auth interceptor/authenticator yet.
     * We will address this in a future step if needed.
     *
     * @param listener The listener to report download progress.
     * @return A specialized ApiService instance.
     */
    public static ApiService getDownloadApiService(DownloadProgressListener listener) {
        // This client only has the progress interceptor.
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
