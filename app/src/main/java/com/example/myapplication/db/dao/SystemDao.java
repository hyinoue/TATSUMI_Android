package com.example.myapplication.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myapplication.db.entity.SystemEntity;


@Dao

//==========================
//　処理概要　:　SystemDaoクラス
//==========================

public interface SystemDao {

    @Insert
    void insert(SystemEntity entity);

    @Query("SELECT * FROM " +
            "M_SYSTEM " +
            "WHERE " +
            "RENBAN = 1"
    )
    SystemEntity get();

    @Query("SELECT * FROM " +
            "M_SYSTEM " +
            "WHERE " +
            "RENBAN = :renban"
    )
        //====================================
        //　機　能　:　find By Idの処理
        //　引　数　:　renban ..... int
        //　戻り値　:　[SystemEntity] ..... なし
        //====================================
    SystemEntity findById(int renban);

    //======================================
    //　機　能　:　upsertの処理
    //　引　数　:　entity ..... SystemEntity
    //　戻り値　:　[void] ..... なし
    //======================================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(SystemEntity entity);

    @Query("UPDATE " +
            "M_SYSTEM " +
            "SET " +
            "DATA_CONF_YMDHMS = :dataConfYmdhms, " +
            "UPDATE_PROC_NAME = :updateProcName, " +
            "UPDATE_YMD = :updateYmd " +
            "WHERE " +
            "RENBAN = :renban"
    )
        //========================================
        //　機　能　:　data Confを更新する
        //　引　数　:　renban ..... int
        //　　　　　:　dataConfYmdhms ..... String
        //　戻り値　:　[int] ..... なし
        //========================================
    int updateDataConf(int renban, String dataConfYmdhms, String updateProcName, String updateYmd);

    @Query("UPDATE " +
            "M_SYSTEM " +
            "SET " +
            "DATA_CONF_YMDHMS = :dataConfYmdhms, " +
            "DATA_RECV_YMDHMS = :dataRecvYmdhms, " +
            "UPDATE_PROC_NAME = :updateProcName, " +
            "UPDATE_YMD = :updateYmd " +
            "WHERE " +
            "RENBAN = :renban"
    )
        //========================================
        //　機　能　:　data Syncを更新する
        //　引　数　:　renban ..... int
        //　　　　　:　dataConfYmdhms ..... String
        //　　　　　:　dataRecvYmdhms ..... String
        //　　　　　:　updateProcName ..... String
        //　　　　　:　updateYmd　　　 ..... String
        //　戻り値　:　[int] ..... なし
        //========================================
    int updateDataSync(int renban, String dataConfYmdhms, String dataRecvYmdhms, String updateProcName, String updateYmd);

    @Query("DELETE FROM " +
            "M_SYSTEM"
    )
        //============================
        //　機　能　:　allを削除する
        //　引　数　:　なし
        //　戻り値　:　[void] ..... なし
        //============================
    void deleteAll();
}
