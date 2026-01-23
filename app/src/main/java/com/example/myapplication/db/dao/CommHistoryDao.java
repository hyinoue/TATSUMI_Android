package com.example.myapplication.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myapplication.db.entity.CommHistoryEntity;

import java.util.List;

@Dao
public interface CommHistoryDao {

    @Query("SELECT * FROM C_COMM_HISTORY ORDER BY RENBAN DESC")
    List<CommHistoryEntity> findAllDesc();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(CommHistoryEntity entity);

    @Query("DELETE FROM C_COMM_HISTORY")
    void deleteAll();
}
