package com.andreas.personalcloudclient;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import com.bumptech.glide.Glide;

public class ImageViewerActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URL = "image_url";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        ImageView imageView = findViewById(R.id.imageViewFull);

        // Get the image URL that MainActivity passed
        String imageUrl = getIntent().getStringExtra(EXTRA_IMAGE_URL);

        if (imageUrl != null) {
            // Use Glide to load the full-resolution image
            Glide.with(this)
                .load(imageUrl)
                .into(imageView);
        }
    }
}
