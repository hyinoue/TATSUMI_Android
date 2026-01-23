package com.example.myapplication.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myapplication.db.entity.KakuninContainerEntity;

import java.util.List;

@Dao
public interface KakuninContainerDao {

    @Query("SELECT * FROM T_KAKUNIN_CONTAINER WHERE KAKUNIN_CONTAINER_ID = :id")
    KakuninContainerEntity findById(int id);

    @Query("SELECT * FROM T_KAKUNIN_CONTAINER WHERE BOOKING_NO = :bookingNo")
    List<KakuninContainerEntity> findByBookingNo(String bookingNo);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(KakuninContainerEntity entity);

    @Query("DELETE FROM T_KAKUNIN_CONTAINER")
    void deleteAll();
}
