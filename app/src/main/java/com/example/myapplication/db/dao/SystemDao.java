package com.example.myapplication.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myapplication.db.entity.SystemEntity;

@Dao
public interface SystemDao {

    @Query("SELECT * FROM M_SYSTEM WHERE RENBAN = :renban")
    SystemEntity findById(int renban);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(SystemEntity entity);

    @Query("DELETE FROM M_SYSTEM")
    void deleteAll();
}
