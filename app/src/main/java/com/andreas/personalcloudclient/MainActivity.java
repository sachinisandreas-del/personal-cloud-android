package com.andreas.personalcloudclient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements FileAdapter.OnFileClickListener {

    private FileListViewModel viewModel;
    private FileAdapter fileAdapter;
    private ActionMode actionMode;

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                // --- DELEGATE TO VIEWMODEL ---
                viewModel.uploadFile(result.getData().getData());
            } else {
                Toast.makeText(this, "File selection cancelled", Toast.LENGTH_SHORT).show();
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(FileListViewModel.class);

        setupUI();
        setupObservers();

        viewModel.loadFileList();
    }

    private void setupUI() {
        Button selectFileButton = findViewById(R.id.buttonSelectFile);
        ProgressBar progressBar = findViewById(R.id.progressBar);
        TextView emptyTextView = findViewById(R.id.textViewEmpty);
        RecyclerView recyclerView = findViewById(R.id.recyclerViewFiles);

        fileAdapter = new FileAdapter();
        recyclerView.setAdapter(fileAdapter);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        fileAdapter.setOnFileClickListener(this);

        selectFileButton.setOnClickListener(v -> openFilePicker());
    }

    private void setupObservers() {
        viewModel.fileList.observe(this, files -> {
            if (files != null) {
                fileAdapter.setFiles(files);
                findViewById(R.id.textViewEmpty).setVisibility(files.isEmpty() ? View.VISIBLE : View.GONE);
                findViewById(R.id.recyclerViewFiles).setVisibility(files.isEmpty() ? View.GONE : View.VISIBLE);
            }
        });

        viewModel.isLoading.observe(this, isLoading -> {
            if (isLoading != null) {
                findViewById(R.id.progressBar).setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.toastMessage.observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                viewModel.onToastMessageShown();
            }
        });

        viewModel.selectedItems.observe(this, selection -> {
            if (selection != null) {
                fileAdapter.setSelectionMode(selection);
                if (actionMode != null) {
                    actionMode.setTitle(selection.size() + " selected");
                    actionMode.invalidate();
                }
            }
        });

        viewModel.isSelectionModeActive.observe(this, isActive -> {
            if (isActive != null && !isActive && actionMode != null) {
                actionMode.finish();
            }
        });
    }

    @Override
    public void onFileClicked(FileMetadata file) {
        if (Boolean.TRUE.equals(viewModel.isSelectionModeActive.getValue())) {
            viewModel.toggleSelection(file.getFilename());
        } else {
            viewFile(file);
        }
    }

    @Override
    public void onFileLongClicked(FileMetadata file) {
        if (actionMode == null) {
            actionMode = startActionMode(actionModeCallback);
        }
        viewModel.toggleSelection(file.getFilename());
    }

    private void viewFile(FileMetadata file) {
        String filename = file.getFilename();
        String fileType = file.getFileType();
        String fileUrl = RetrofitClient.BASE_URL + "download/" + filename;

        if ("image".equals(fileType)) {
            Intent intent = new Intent(this, ImageViewerActivity.class);
            intent.putExtra(ImageViewerActivity.EXTRA_IMAGE_URL, fileUrl);
            startActivity(intent);
        } else {
            String streamUrl = fileUrl + "?view=true";
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(streamUrl));
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "No app found to open this file type", Toast.LENGTH_LONG).show();
            }
        }
    }

    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.contextual_action_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) { return false; }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int itemId = item.getItemId();
            if (itemId == R.id.action_delete_contextual) {
                viewModel.deleteSelectedFiles();
                return true;
            } else if (itemId == R.id.action_download_contextual) {
                Set<String> selectedFiles = viewModel.selectedItems.getValue();
                if(selectedFiles != null){
                    for(String filename : selectedFiles){
                        viewModel.downloadFile(filename);
                    }
                }
                mode.finish();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            viewModel.clearSelection();
            actionMode = null;
        }
    };

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(intent);
    }
}
