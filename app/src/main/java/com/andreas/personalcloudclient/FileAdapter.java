package com.andreas.personalcloudclient;

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
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    private List<FileMetadata> fileList = new ArrayList<>();
    private OnFileClickListener listener;

    public interface OnFileClickListener {
        void onFileClicked(FileMetadata file);
        void onFileOptionsClicked(FileMetadata file);
    }

    public void setOnFileClickListener(OnFileClickListener listener) {
        this.listener = listener;
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

        // Reset the image view to avoid flickering from recycled views
        holder.iconImageView.setImageResource(R.drawable.ic_file_generic);

        switch (file.getFileType()) {
            case "image":
                String imageUrl = RetrofitClient.BASE_URL + "download/" + file.getFilename();
                Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
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

        holder.optionsButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFileOptionsClicked(file);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFileClicked(file);
            }
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
