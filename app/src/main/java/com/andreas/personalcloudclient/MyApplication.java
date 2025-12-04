package com.andreas.personalcloudclient;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import com.orhanobut.hawk.Hawk;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Hawk for secure storage
        Hawk.init(this).build();

        // Create notification channels for downloads/uploads (Android 8.0+)
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        // Only needed on Android 8.0 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);

            // Upload Channel
            NotificationChannel uploadChannel = new NotificationChannel(
                "upload_channel",
                "File Uploads",
                NotificationManager.IMPORTANCE_LOW
            );
            uploadChannel.setDescription("Shows file upload progress");
            uploadChannel.setShowBadge(false);  // Don't show badge on app icon

            // Download Channel
            NotificationChannel downloadChannel = new NotificationChannel(
                "download_channel",
                "File Downloads",
                NotificationManager.IMPORTANCE_LOW
            );
            downloadChannel.setDescription("Shows file download progress");
            downloadChannel.setShowBadge(false);

            // Register both channels
            notificationManager.createNotificationChannel(uploadChannel);
            notificationManager.createNotificationChannel(downloadChannel);
        }
    }
}
