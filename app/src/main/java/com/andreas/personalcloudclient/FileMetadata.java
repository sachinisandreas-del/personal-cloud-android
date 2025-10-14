package com.andreas.personalcloudclient;

import com.google.gson.annotations.SerializedName;

public class FileMetadata {

    @SerializedName("filename")
    private String filename;

    @SerializedName("file_type")
    private String fileType;

    @SerializedName("size")
    private long size;

    @SerializedName("modified_at")
    private String modifiedAt;

    // --- Getters ---

    public String getFilename() {
        return filename;
    }

    public String getFileType() {
        return fileType;
    }

    public long getSize() {
        return size;
    }

    public String getModifiedAt() {
        return modifiedAt;
    }
}
