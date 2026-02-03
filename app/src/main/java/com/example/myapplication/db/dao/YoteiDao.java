package com.example.myapplication.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myapplication.db.entity.YoteiEntity;

import java.util.List;


@Dao

//===================
//　処理概要　:　YoteiDaoクラス
//===================

public interface YoteiDao {

    @Query("SELECT * FROM " +
            "T_YOTEI"
    )
    //===================================
    //　機　能　:　find Allの処理
    //　引　数　:　なし
    //　戻り値　:　[List<YoteiEntity>] ..... なし
    //===================================
    List<YoteiEntity> findAll();

    @Query("SELECT * FROM " +
            "T_YOTEI " +
            "LIMIT 1"
    )
    //=============================
    //　機　能　:　find Firstの処理
    //　引　数　:　なし
    //　戻り値　:　[YoteiEntity] ..... なし
    //=============================
    YoteiEntity findFirst();

    @Query("SELECT * FROM " +
            "T_YOTEI " +
            "WHERE " +
            "LAST_UPD_YMDHMS IS NULL"
    )
    //===================================
    //　機　能　:　find With Null Last Updの処理
    //　引　数　:　なし
    //　戻り値　:　[List<YoteiEntity>] ..... なし
    //===================================
    List<YoteiEntity> findWithNullLastUpd();

    @Query("SELECT * FROM " +
            "T_YOTEI " +
            "WHERE " +
            "TRIM(BOOKING_NO) = TRIM(:bookingNo)"
    )
    //=============================
    //　機　能　:　find By Booking Noの処理
    //　引　数　:　bookingNo ..... String
    //　戻り値　:　[YoteiEntity] ..... なし
    //=============================
    YoteiEntity findByBookingNo(String bookingNo);

    @Query("UPDATE " +
            "T_YOTEI " +
            "SET " +
            "KANRYO_CONTAINER = KANRYO_CONTAINER + 1, " +
            "KANRYO_BUNDOLE = KANRYO_BUNDOLE + :bundleCount, " +
            "KANRYO_JYURYO = KANRYO_JYURYO + :jyuryo " +
            "WHERE " +
            "TRIM(BOOKING_NO) = TRIM(:bookingNo)"
    )
    //=============================
    //　機　能　:　increment Kanryoの処理
    //　引　数　:　bookingNo ..... String
    //　　　　　:　bundleCount ..... int
    //　　　　　:　jyuryo ..... int
    //　戻り値　:　[int] ..... なし
    //=============================
    int incrementKanryo(String bookingNo, int bundleCount, int jyuryo);

    //===============================
    //　機　能　:　upsertの処理
    //　引　数　:　entity ..... YoteiEntity
    //　戻り値　:　[void] ..... なし
    //===============================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(YoteiEntity entity);

    @Query("DELETE FROM " +
            "T_YOTEI"
    )
    //======================
    //　機　能　:　allを削除する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //======================
    void deleteAll();
}
