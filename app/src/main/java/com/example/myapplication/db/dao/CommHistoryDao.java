package com.example.myapplication.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myapplication.db.entity.CommHistoryEntity;

import java.util.List;


@Dao

//===============================
//　処理概要　:　CommHistoryDaoクラス
//===============================

public interface CommHistoryDao {

    @Query("SELECT * FROM " +
            "C_COMM_HISTORY " +
            "ORDER BY " +
            "START_YMDHMS DESC, END_YMDHMS DESC"
    )
        //===============================================
        //　機　能　:　find All Descの処理
        //　引　数　:　なし
        //　戻り値　:　[List<CommHistoryEntity>] ..... なし
        //===============================================
    List<CommHistoryEntity> findAllDesc();

    //===========================================
    //　機　能　:　upsertの処理
    //　引　数　:　entity ..... CommHistoryEntity
    //　戻り値　:　[void] ..... なし
    //===========================================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(CommHistoryEntity entity);

    @Query("DELETE FROM " +
            "C_COMM_HISTORY " +
            "WHERE " +
            "START_YMDHMS < :threshold"
    )
        //===================================
        //　機　能　:　beforeを削除する
        //　引　数　:　threshold ..... String
        //　戻り値　:　[void] ..... なし
        //===================================
    void deleteBefore(String threshold);

    @Query("DELETE FROM " +
            "C_COMM_HISTORY"
    )
        //============================
        //　機　能　:　allを削除する
        //　引　数　:　なし
        //　戻り値　:　[void] ..... なし
        //============================
    void deleteAll();
}
