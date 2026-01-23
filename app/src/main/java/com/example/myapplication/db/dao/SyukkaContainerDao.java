package com.example.myapplication.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myapplication.db.entity.SyukkaContainerEntity;

import java.util.List;

@Dao
public interface SyukkaContainerDao {

    @Query("SELECT * FROM T_SYUKKA_CONTAINER WHERE CONTAINER_ID = :containerId")
    SyukkaContainerEntity findById(int containerId);

    @Query("SELECT * FROM T_SYUKKA_CONTAINER WHERE BOOKING_NO = :bookingNo")
    List<SyukkaContainerEntity> findByBookingNo(String bookingNo);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(SyukkaContainerEntity entity);

    @Query("SELECT * FROM T_SYUKKA_CONTAINER WHERE DATA_SEND_YMDHMS IS NULL ORDER BY CONTAINER_ID")
    List<SyukkaContainerEntity> findUnsent();

    @Query("UPDATE T_SYUKKA_CONTAINER SET DATA_SEND_YMDHMS = :dataSendYmdhms WHERE CONTAINER_ID = :containerId")
    int markSent(int containerId, String dataSendYmdhms);

    @Query("DELETE FROM T_SYUKKA_CONTAINER WHERE DATA_SEND_YMDHMS IS NOT NULL")
    void deleteSent();

    @Query("DELETE FROM T_SYUKKA_CONTAINER")
    void deleteAll();
}