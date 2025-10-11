package com.andreas.personalcloudclient;

import com.google.gson.annotations.SerializedName;

public class UploadResponse {

    // @SerializedName matches the JSON key from the Flask server
    @SerializedName("message")
    private String message;

    @SerializedName("filename")
    private String filename;

    // --- Getters ---
    public String getMessage() {
        return message;
    }

    public String getFilename() {
        return filename;
    }
}
