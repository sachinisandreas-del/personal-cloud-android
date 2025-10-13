package com.andreas.personalcloudclient; // Make sure this matches your package name

import android.os.Handler;
import android.os.Looper;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ProgressRequestBody extends RequestBody {

    private final File file;
    private final ProgressListener listener;
    private static final int DEFAULT_BUFFER_SIZE = 4096;

    public interface ProgressListener {
        void onProgressUpdate(long bytesUploaded, long totalBytes);
    }

    public ProgressRequestBody(File file, ProgressListener listener) {
        this.file = file;
        this.listener = listener;
    }

    @Override
    public MediaType contentType() {
        // We can guess the media type from the file, or use a generic one
        return MediaType.parse("application/octet-stream");
    }

    @Override
    public long contentLength() {
        return file.length();
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        long fileLength = file.length();
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        long uploaded = 0;

        try (FileInputStream in = new FileInputStream(file)) {
            int read;
            Handler handler = new Handler(Looper.getMainLooper());
            while ((read = in.read(buffer)) != -1) {
                uploaded += read;
                sink.write(buffer, 0, read);

                // Post progress update back to the main thread
                final long finalUploaded = uploaded;
                handler.post(() -> listener.onProgressUpdate(finalUploaded, fileLength));
            }
        }
    }
}
