package com.andreas.personalcloudclient;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private Button selectFileButton;
    private TextView statusTextView;

    // This is the modern way to handle getting a result from another activity (like the file picker)
    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                // This block runs if the user successfully selected a file.
                Intent data = result.getData();
                if (data != null) {
                    Uri fileUri = data.getData();
                    Log.d(TAG, "File selected: " + fileUri.toString());
                    statusTextView.setText("File selected. Preparing to upload...");

                    // TODO: We will add the upload logic here in the next step.
                }
            } else {
                // This block runs if the user cancels the file picker.
                Log.d(TAG, "File selection cancelled.");
                statusTextView.setText("File selection cancelled.");
            }
        });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Find the UI elements from the XML layout using their IDs
        selectFileButton = findViewById(R.id.buttonSelectFile);
        statusTextView = findViewById(R.id.textViewStatus);

        // 2. Set an OnClickListener for our button
        selectFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // This is what happens when the button is clicked.
                Log.d(TAG, "Select file button clicked.");
                openFilePicker();
            }
        });
    }

    private void openFilePicker() {
        // 3. Create an Intent to open the system's file picker
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // Allow all file types to be selected
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // 4. Launch the file picker and wait for the result
        filePickerLauncher.launch(intent);
    }
}
