package com.andreas.personalcloudclient;

import android.app.Application;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FileListViewModel extends AndroidViewModel {

    private final FileRepository fileRepository;

    private final MutableLiveData<List<FileMetadata>> _fileList = new MutableLiveData<>();
    public final LiveData<List<FileMetadata>> fileList = _fileList;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    public final LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<String> _toastMessage = new MutableLiveData<>();
    public final LiveData<String> toastMessage = _toastMessage;

    private final MutableLiveData<Set<String>> _selectedItems = new MutableLiveData<>(new HashSet<>());
    public final LiveData<Set<String>> selectedItems = _selectedItems;

    private final MutableLiveData<Boolean> _isSelectionModeActive = new MutableLiveData<>(false);
    public final LiveData<Boolean> isSelectionModeActive = _isSelectionModeActive;

    public FileListViewModel(@NonNull Application application) {
        super(application);
        this.fileRepository = new FileRepository(application);
    }

    public void loadFileList() {
        _isLoading.setValue(true);

        fileRepository.getFiles(new FileRepository.GetFilesCallback() {
            @Override
            public void onCacheLoaded(List<FileMetadata> cachedFiles) {
                _fileList.setValue(cachedFiles);
            }

            @Override
            public void onNetworkResult(List<FileMetadata> networkFiles, String error) {
                _isLoading.setValue(false);
                if (networkFiles != null) {
                    _fileList.setValue(networkFiles);
                }
                if (error != null) {
                    _toastMessage.setValue(error);
                }
            }
        });
    }

    public void deleteSelectedFiles() {
        Set<String> filesToDelete = _selectedItems.getValue();
        if (filesToDelete == null || filesToDelete.isEmpty()) return;

        _isLoading.setValue(true);
        List<String> filesToDeleteList = new ArrayList<>(filesToDelete);

        final int totalFiles = filesToDeleteList.size();
        final int[] deletedCount = {0};

        for (String filename : filesToDeleteList) {
            fileRepository.deleteFile(filename, new FileRepository.RepositoryCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    deletedCount[0]++;
                    if (deletedCount[0] == totalFiles) {
                        _toastMessage.setValue(totalFiles + " file(s) deleted");
                        clearSelection();
                        loadFileList();
                    }
                }

                @Override
                public void onError(String message) {
                    deletedCount[0]++;
                    _toastMessage.setValue("Error deleting " + filename + ": " + message);
                    if (deletedCount[0] == totalFiles) {
                        clearSelection();
                        loadFileList();
                    }
                }
            });
        }
    }

    public void uploadFile(Uri fileUri) {
        _isLoading.setValue(true);
        fileRepository.uploadFile(fileUri, new FileRepository.RepositoryCallback<String>() {
            @Override
            public void onSuccess(String result) {
                _toastMessage.setValue(result);
                loadFileList();
            }

            @Override
            public void onError(String message) {
                _toastMessage.setValue(message);
                _isLoading.setValue(false);
            }
        });
    }

    public void downloadFile(String filename) {
        fileRepository.downloadFile(filename, new FileRepository.RepositoryCallback<String>() {
            @Override
            public void onSuccess(String result) {
                _toastMessage.setValue(result);
            }

            @Override
            public void onError(String message) {
                _toastMessage.setValue(message);
            }
        });
    }

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

    public void onToastMessageShown() {
        _toastMessage.setValue(null);
    }

    public void selectAllFiles() {
        List<FileMetadata> currentFiles = _fileList.getValue();
        if (currentFiles == null || currentFiles.isEmpty()) {
            return; // Nothing to select
        }

        Set<String> allFilenames = new HashSet<>();
        for (FileMetadata file : currentFiles) {
            allFilenames.add(file.getFilename());
        }

        _selectedItems.setValue(allFilenames);
        _isSelectionModeActive.setValue(true);
    }
}
