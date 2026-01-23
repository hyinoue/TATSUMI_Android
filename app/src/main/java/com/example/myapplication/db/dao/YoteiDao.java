package com.example.myapplication.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myapplication.db.entity.YoteiEntity;

import java.util.List;

@Dao
public interface YoteiDao {

    @Query("SELECT * FROM T_YOTEI")
    List<YoteiEntity> findAll();

    @Query("SELECT * FROM T_YOTEI WHERE BOOKING_NO = :bookingNo")
    YoteiEntity findByBookingNo(String bookingNo);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(YoteiEntity entity);

    @Query("DELETE FROM T_YOTEI")
    void deleteAll();
}
