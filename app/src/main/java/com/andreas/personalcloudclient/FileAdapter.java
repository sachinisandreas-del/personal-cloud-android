package com.andreas.personalcloudclient;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    private List<String> fileList = new ArrayList<>();
    private OnFileClickListener listener; // <-- NEW: The listener variable

    // --- NEW: The Listener Interface ---
    // This interface defines the actions that can be performed on a list item.
    // MainActivity will implement this.
    public interface OnFileClickListener {
        void onFileOptionsClicked(String filename);
    }

    // --- NEW: A method to set the listener ---
    public void setOnFileClickListener(OnFileClickListener listener) {
        this.listener = listener;
    }
    // ------------------------------------

    public static class FileViewHolder extends RecyclerView.ViewHolder {
        public TextView fileNameTextView;
        public ImageButton optionsButton;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            fileNameTextView = itemView.findViewById(R.id.textViewFileName);
            optionsButton = itemView.findViewById(R.id.buttonOptions);
        }
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        String fileName = fileList.get(position);
        holder.fileNameTextView.setText(fileName);

        // --- NEW: Set the click listener for the options button ---
        holder.optionsButton.setOnClickListener(v -> {
            if (listener != null) {
                // When the button is clicked, call the listener's method
                listener.onFileOptionsClicked(fileName);
            }
        });
        // -----------------------------------------------------------
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    public void setFiles(List<String> newFiles) {
        this.fileList.clear();
        this.fileList.addAll(newFiles);
        notifyDataSetChanged();
    }
}
