package com.andreas.personalcloudclient;

import androidx.annotation.NonNull; // <-- NEW
import androidx.room.ColumnInfo; // <-- NEW
import androidx.room.Entity;     // <-- NEW
import androidx.room.PrimaryKey; // <-- NEW
import com.google.gson.annotations.SerializedName;

// @Entity tells Room to create a database table for this object.
@Entity(tableName = "files")
public class FileMetadata {

    // @PrimaryKey defines the unique ID for each row in the table.
    // @NonNull ensures the filename can't be null.
    @PrimaryKey
    @NonNull
    @SerializedName("filename")
    private String filename;

    @ColumnInfo(name = "file_type")
    @SerializedName("file_type")
    private String fileType;

    @ColumnInfo(name = "size")
    @SerializedName("size")
    private long size;

    @ColumnInfo(name = "modified_at")
    @SerializedName("modified_at")
    private String modifiedAt;

    // --- Room needs an empty constructor ---
    public FileMetadata() {}

    // --- Getters and Setters (Room needs setters) ---
    @NonNull
    public String getFilename() { return filename; }
    public void setFilename(@NonNull String filename) { this.filename = filename; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public String getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(String modifiedAt) { this.modifiedAt = modifiedAt; }
}
