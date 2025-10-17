package com.andreas.personalcloudclient;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface FileDao {

    @Query("SELECT * FROM files ORDER BY filename ASC")
    List<FileMetadata> getAllFiles();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<FileMetadata> files);

    @Query("DELETE FROM files")
    void deleteAll();

    // --- ADD THIS METHOD ---
    @Query("DELETE FROM files WHERE filename = :filename")
    void deleteFileByFilename(String filename);
    // ----------------------
}
