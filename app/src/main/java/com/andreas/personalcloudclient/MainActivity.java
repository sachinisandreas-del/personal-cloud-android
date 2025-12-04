package com.andreas.personalcloudclient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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

import java.io.File;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements FileAdapter.OnFileClickListener {

    private FileListViewModel viewModel;
    private FileAdapter fileAdapter;
    private ActionMode actionMode;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private boolean isGridLayout = true; // To track the current layout state
    private MenuItem toggleLayoutMenuItem; // To change the icon dynamically

    private String baseUrl;

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                viewModel.uploadFile(result.getData().getData());
            } else {
                Toast.makeText(this, "File selection cancelled", Toast.LENGTH_SHORT).show();
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication())).get(FileListViewModel.class);

        baseUrl = getString(R.string.api_base_url);
        setupUI();
        setupObservers();

        viewModel.loadFileList();
    }

    private void setupUI() {

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        recyclerView = findViewById(R.id.recyclerViewFiles);

        fileAdapter = new FileAdapter(baseUrl);

        recyclerView.setAdapter(fileAdapter);
        // Start with a Grid Layout
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        fileAdapter.setOnFileClickListener(this);

        swipeRefreshLayout.setOnRefreshListener(() -> {
            // When the user swipes, tell the ViewModel to reload the file list.
            // The loading indicator will be handled by the observer below.
            viewModel.loadFileList();
        });

    }

    private void setupObservers() {
        viewModel.fileList.observe(this, files -> {
            if (files != null) {
                fileAdapter.setFiles(files);
                findViewById(R.id.textViewEmpty).setVisibility(files.isEmpty() ? View.VISIBLE : View.GONE);
                recyclerView.setVisibility(files.isEmpty() ? View.GONE : View.VISIBLE);
            }
        });

        viewModel.isLoading.observe(this, isLoading -> {
            if (isLoading != null) {
                // This 'if' condition prevents the big central progress bar from
                // showing when using swipe-to-refresh.
                if (!swipeRefreshLayout.isRefreshing()) {
                    findViewById(R.id.progressBar).setVisibility(isLoading ? View.VISIBLE : View.GONE);
                }

                // This 'if' condition tells the swipe-to-refresh layout to hide
                // its spinning icon when the loading is finished.
                if (!isLoading) {
                    swipeRefreshLayout.setRefreshing(false);
                }
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
                    // If all items are deselected, the action mode should finish.
                    if (selection.isEmpty()) {
                        actionMode.finish();
                    }
                }
            }
        });

        viewModel.isSelectionModeActive.observe(this, isActive -> {
            if (isActive != null && !isActive && actionMode != null) {
                actionMode.finish();
            }
        });
    }

    // --- Inflate the options menu from XML ---
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        // Find the specific menu item for toggling layout
        toggleLayoutMenuItem = menu.findItem(R.id.action_more).getSubMenu().findItem(R.id.action_toggle_layout);
        return true;
    }

    // --- Handle clicks on menu items ---
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_upload) {
            openFilePicker();
            return true;
        } else if (id == R.id.action_toggle_layout) {
            toggleLayout();
            return true;
        } else if (id == R.id.action_select_all) {
            viewModel.selectAllFiles();
            // Start action mode if it's not already active
            if (actionMode == null) {
                actionMode = startActionMode(actionModeCallback);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // --- Logic to switch between Grid and List layout ---
    private void toggleLayout() {
        if (isGridLayout) {
            // Switch to Linear Layout
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            toggleLayoutMenuItem.setIcon(R.drawable.ic_view_grid); // Show grid icon for next click
            toggleLayoutMenuItem.setTitle("Switch to Grid View");
        } else {
            // Switch to Grid Layout
            recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
            toggleLayoutMenuItem.setIcon(R.drawable.ic_view_list); // Show list icon for next click
            toggleLayoutMenuItem.setTitle("Switch to List View");
        }
        isGridLayout = !isGridLayout;
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
        String fileUrl = baseUrl + "download/" + filename;

        // Check the file type to decide how to open it.
        switch (fileType) {
            case "image":
                // Use our dedicated full-screen image viewer for the best experience.
                Intent imageIntent = new Intent(this, ImageViewerActivity.class);
                imageIntent.putExtra(ImageViewerActivity.EXTRA_IMAGE_URL, fileUrl);
                startActivity(imageIntent);
                break;

            case "video":
            case "pdf":
            case "text":
                // For all other viewable types, use the new external app method.
                openFileExternally(filename);
                break;

            default:
                // For all other types (archives, generic, etc.), use the
                // "download-then-open" strategy.
                Toast.makeText(this, "Preparing to download '" + filename + "'...", Toast.LENGTH_SHORT).show();
                viewModel.downloadFile(filename);
                // NOTE: This won't open the file automatically after download yet.
                break;
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

    private void openFileExternally(String filename) {

        viewModel.setLoadingState(true);

        //Create a new instance of the repository to perform the one-off download.
        FileRepository fileRepository = new FileRepository(getApplication());
        fileRepository.downloadFileToCache(filename, new FileRepository.RepositoryCallback<File>() {
            @Override
            public void onSuccess(File file) {

                viewModel.setLoadingState(false);

                String authority = "com.andreas.personalcloudclient.provider";

                // Get the secure content:// URI from FileProvider.
                Uri fileUri = FileProvider.getUriForFile(
                    MainActivity.this,
                    authority,
                    file
                );

                // Create the Intent to view the file.
                Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                viewIntent.setData(fileUri);
                viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                try {
                    startActivity(viewIntent);
                } catch (ActivityNotFoundException e) {
                    // This happens if the user has no app that can open the file type.
                    Toast.makeText(MainActivity.this, "No app found to open this file type.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String message) {
                viewModel.setLoadingState(false);
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
