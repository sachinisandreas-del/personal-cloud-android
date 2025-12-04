package com.andreas.personalcloudclient;

import android.graphics.Color;
import androidx.core.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    private List<FileMetadata> fileList = new ArrayList<>();
    private Set<String> selectedItems = new HashSet<>();
    private OnFileClickListener listener;

    private final String baseUrl;

    public FileAdapter(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public interface OnFileClickListener {
        void onFileClicked(FileMetadata file);
        void onFileLongClicked(FileMetadata file);
    }

    public void setOnFileClickListener(OnFileClickListener listener) {
        this.listener = listener;
    }

    public void setSelectionMode(Set<String> selectedItems) {
        this.selectedItems = selectedItems;
        notifyDataSetChanged();
    }

    public static class FileViewHolder extends RecyclerView.ViewHolder {
        public ImageView iconImageView;
        public TextView fileNameTextView;
        public TextView fileSizeTextView;
        public ImageButton optionsButton;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            iconImageView = itemView.findViewById(R.id.imageViewIcon);
            fileNameTextView = itemView.findViewById(R.id.textViewFileName);
            fileSizeTextView = itemView.findViewById(R.id.textViewFileSize);
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
        FileMetadata file = fileList.get(position);

        holder.fileNameTextView.setText(file.getFilename());
        holder.fileSizeTextView.setText(formatFileSize(file.getSize()));
        holder.optionsButton.setVisibility(View.GONE);

        if (selectedItems.contains(file.getFilename())) {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.selected_item_color));
        } else {
            // Use transparent to allow the default selectableItemBackground ripple effect to show
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }

        switch (file.getFileType()) {
            case "image":
                // --- NEW AUTHENTICATED GLIDE LOGIC ---

                // 1. Get the SessionManager to retrieve our token.
                SessionManager sessionManager = SessionManager.getInstance();

                // 2. Create a GlideUrl, which allows us to attach headers.
                GlideUrl glideUrl = new GlideUrl(
                    baseUrl + "download/" + file.getFilename(),
                    new LazyHeaders.Builder()
                        // Add the Authorization header.
                        .addHeader("Authorization", "Bearer " + sessionManager.getAccessToken())
                        .build()
                );

                // 3. Tell Glide to load the GlideUrl instead of the simple String URL.
                Glide.with(holder.itemView.getContext())
                    .load(glideUrl) // We now load the object with headers
                    .placeholder(R.drawable.ic_file_generic)
                    .error(R.drawable.ic_file_generic)
                    .into(holder.iconImageView);
                break;
            case "pdf":
                holder.iconImageView.setImageResource(R.drawable.ic_file_pdf);
                break;
            case "text":
                holder.iconImageView.setImageResource(R.drawable.ic_file_text);
                break;
            case "archive":
                holder.iconImageView.setImageResource(R.drawable.ic_file_archive);
                break;
            case "audio":
                holder.iconImageView.setImageResource(R.drawable.ic_file_audio);
                break;
            case "video":
                holder.iconImageView.setImageResource(R.drawable.ic_file_video);
                break;
            default:
                holder.iconImageView.setImageResource(R.drawable.ic_file_generic);
                break;
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFileClicked(file);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onFileLongClicked(file);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    public void setFiles(List<FileMetadata> newFiles) {
        this.fileList.clear();
        this.fileList.addAll(newFiles);
        notifyDataSetChanged();
    }

    private String formatFileSize(long bytes) {
        if (-1000 < bytes && bytes < 1000) {
            return bytes + " B";
        }
        CharacterIterator ci = new StringCharacterIterator("kMGTPE");
        while (bytes <= -999_950 || bytes >= 999_950) {
            bytes /= 1000;
            ci.next();
        }
        return String.format("%.1f %cB", bytes / 1000.0, ci.current());
    }
}
