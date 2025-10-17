package com.andreas.personalcloudclient;

import androidx.annotation.NonNull;
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

    // --- NEW CLASS MEMBERS ---
    private RecyclerView recyclerView;
    private boolean isGridLayout = true; // To track the current layout state
    private MenuItem toggleLayoutMenuItem; // To change the icon dynamically

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

        setupUI();
        setupObservers();

        viewModel.loadFileList();
    }

    private void setupUI() {
        // --- MODIFIED: Assign recyclerView to the class member ---
        recyclerView = findViewById(R.id.recyclerViewFiles);

        fileAdapter = new FileAdapter();
        recyclerView.setAdapter(fileAdapter);
        // Start with a Grid Layout
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        fileAdapter.setOnFileClickListener(this);

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

    // --- NEW: Inflate the options menu from XML ---
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        // Find the specific menu item for toggling layout
        toggleLayoutMenuItem = menu.findItem(R.id.action_more).getSubMenu().findItem(R.id.action_toggle_layout);
        return true;
    }

    // --- NEW: Handle clicks on menu items ---
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

    // --- NEW: Logic to switch between Grid and List layout ---
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
