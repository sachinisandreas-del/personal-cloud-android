package com.andreas.personalcloudclient;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;

public class DownloadProgressInterceptor implements Interceptor {

    private final DownloadProgressListener listener;

    public DownloadProgressInterceptor(DownloadProgressListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Response originalResponse = chain.proceed(chain.request());

        if (listener == null) {
            return originalResponse;
        }

        return originalResponse.newBuilder()
            .body(new ProgressResponseBody(originalResponse.body(), listener))
            .build();
    }

    private static class ProgressResponseBody extends ResponseBody {
        private final ResponseBody responseBody;
        private final DownloadProgressListener progressListener;
        private BufferedSource bufferedSource;

        ProgressResponseBody(ResponseBody responseBody, DownloadProgressListener progressListener) {
            this.responseBody = responseBody;
            this.progressListener = progressListener;
        }

        @Override
        public MediaType contentType() {
            return responseBody.contentType();
        }

        @Override
        public long contentLength() {
            return responseBody.contentLength();
        }

        @NonNull
        @Override
        public BufferedSource source() {
            if (bufferedSource == null) {
                bufferedSource = Okio.buffer(source(responseBody.source()));
            }
            return bufferedSource;
        }

        private Source source(Source source) {
            return new ForwardingSource(source) {
                long totalBytesRead = 0L;
                Handler handler = new Handler(Looper.getMainLooper());

                @Override
                public long read(@NonNull Buffer sink, long byteCount) throws IOException {
                    long bytesRead = super.read(sink, byteCount);
                    if (bytesRead != -1) {
                        totalBytesRead += bytesRead;
                        int progress = (int) ((totalBytesRead * 100) / responseBody.contentLength());

                        // Post the progress update to the main thread
                        handler.post(() -> progressListener.onProgressUpdate(progress));
                    }
                    return bytesRead;
                }
            };
        }
    }
}
