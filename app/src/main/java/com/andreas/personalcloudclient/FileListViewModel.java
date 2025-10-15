package com.andreas.personalcloudclient;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FileListViewModel extends ViewModel {

    private final FileRepository fileRepository;

    // --- LiveData for UI state ---
    private final MutableLiveData<List<FileMetadata>> _fileList = new MutableLiveData<>();
    public final LiveData<List<FileMetadata>> fileList = _fileList;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    public final LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<String> _toastMessage = new MutableLiveData<>();
    public final LiveData<String> toastMessage = _toastMessage;

    // --- LiveData for selection mode ---
    private final MutableLiveData<Set<String>> _selectedItems = new MutableLiveData<>(new HashSet<>());
    public final LiveData<Set<String>> selectedItems = _selectedItems;

    private final MutableLiveData<Boolean> _isSelectionModeActive = new MutableLiveData<>(false);
    public final LiveData<Boolean> isSelectionModeActive = _isSelectionModeActive;

    public FileListViewModel() {
        this.fileRepository = new FileRepository();
    }

    // --- Methods to manage selection ---

    public void toggleSelection(String filename) {
        Set<String> currentSelection = _selectedItems.getValue();
        if (currentSelection == null) currentSelection = new HashSet<>();

        if (currentSelection.contains(filename)) {
            currentSelection.remove(filename);
        } else {
            currentSelection.add(filename);
        }

        _selectedItems.setValue(currentSelection);
        _isSelectionModeActive.setValue(!currentSelection.isEmpty());
    }

    public void clearSelection() {
        _selectedItems.setValue(new HashSet<>());
        _isSelectionModeActive.setValue(false);
    }

    // --- Data operation methods ---

    public void loadFileList() {
        _isLoading.setValue(true);
        fileRepository.getFiles(new FileRepository.RepositoryCallback<List<FileMetadata>>() {
            @Override
            public void onSuccess(List<FileMetadata> result) {
                _fileList.setValue(result);
                _isLoading.setValue(false);
            }

            @Override
            public void onError(String message) {
                _toastMessage.setValue(message);
                _isLoading.setValue(false);
            }
        });
    }

    public void deleteSelectedFiles() {
        Set<String> filesToDelete = _selectedItems.getValue();
        if (filesToDelete == null || filesToDelete.isEmpty()) return;

        _isLoading.setValue(true);
        // Create a copy for safe iteration
        List<String> filesToDeleteList = new ArrayList<>(filesToDelete);

        // Use a counter to know when the last file is deleted
        final int totalFiles = filesToDeleteList.size();
        final int[] deletedCount = {0};

        for (String filename : filesToDeleteList) {
            fileRepository.deleteFile(filename, new FileRepository.RepositoryCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    deletedCount[0]++;
                    // When the last file is deleted, refresh the list and clear selection.
                    if (deletedCount[0] == totalFiles) {
                        _toastMessage.setValue(totalFiles + " file(s) deleted");
                        clearSelection();
                        loadFileList(); // Reload the list from the server
                    }
                }

                @Override
                public void onError(String message) {
                    deletedCount[0]++;
                    _toastMessage.setValue("Error deleting " + filename + ": " + message);
                    // Still refresh the list even if some deletions fail
                    if (deletedCount[0] == totalFiles) {
                        clearSelection();
                        loadFileList();
                    }
                }
            });
        }
    }

    public void onToastMessageShown() {
        _toastMessage.setValue(null);
    }
}
