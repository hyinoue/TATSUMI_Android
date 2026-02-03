package com.example.myapplication.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myapplication.db.entity.SyukkaContainerEntity;

import java.util.List;


@Dao

//=============================
//　処理概要　:　SyukkaContainerDaoクラス
//=============================

public interface SyukkaContainerDao {

    @Query("SELECT * FROM " +
            "T_SYUKKA_CONTAINER " +
            "WHERE " +
            "CONTAINER_ID = :containerId"
    )
    //=======================================
    //　機　能　:　find By Idの処理
    //　引　数　:　containerId ..... int
    //　戻り値　:　[SyukkaContainerEntity] ..... なし
    //=======================================
    SyukkaContainerEntity findById(int containerId);

    @Query("SELECT * FROM " +
            "T_SYUKKA_CONTAINER " +
            "WHERE " +
            "TRIM(BOOKING_NO) = TRIM(:bookingNo)"
    )
    //=============================================
    //　機　能　:　find By Booking Noの処理
    //　引　数　:　bookingNo ..... String
    //　戻り値　:　[List<SyukkaContainerEntity>] ..... なし
    //=============================================
    List<SyukkaContainerEntity> findByBookingNo(String bookingNo);

    //=========================================
    //　機　能　:　upsertの処理
    //　引　数　:　entity ..... SyukkaContainerEntity
    //　戻り値　:　[void] ..... なし
    //=========================================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(SyukkaContainerEntity entity);

    @Query("SELECT * FROM " +
            "T_SYUKKA_CONTAINER" +
            " WHERE " +
            "DATA_SEND_YMDHMS IS NULL ORDER BY CONTAINER_ID"
    )
    //=============================================
    //　機　能　:　find Unsentの処理
    //　引　数　:　なし
    //　戻り値　:　[List<SyukkaContainerEntity>] ..... なし
    //=============================================
    List<SyukkaContainerEntity> findUnsent();

    @Query("SELECT " +
            "MAX(CONTAINER_ID) " +
            "FROM T_SYUKKA_CONTAINER"
    )
    //============================
    //　機　能　:　max Container Idを取得する
    //　引　数　:　なし
    //　戻り値　:　[Integer] ..... なし
    //============================
    Integer getMaxContainerId();

    @Query("UPDATE " +
            "T_SYUKKA_CONTAINER " +
            "SET " +
            "DATA_SEND_YMDHMS = :dataSendYmdhms " +
            "WHERE " +
            "CONTAINER_ID = :containerId"
    )
    //==================================
    //　機　能　:　mark Sentの処理
    //　引　数　:　containerId ..... int
    //　　　　　:　dataSendYmdhms ..... String
    //　戻り値　:　[int] ..... なし
    //==================================
    int markSent(int containerId, String dataSendYmdhms);

    @Query("DELETE FROM " +
            "T_SYUKKA_CONTAINER " +
            "WHERE " +
            "DATA_SEND_YMDHMS IS NOT NULL"
    )
    //======================
    //　機　能　:　sentを削除する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //======================
    void deleteSent();

    @Query("DELETE FROM " +
            "T_SYUKKA_CONTAINER"
    )
    //======================
    //　機　能　:　allを削除する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //======================
    void deleteAll();
}
