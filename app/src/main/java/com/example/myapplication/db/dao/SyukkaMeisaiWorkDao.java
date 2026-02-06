package com.example.myapplication.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myapplication.db.entity.SyukkaMeisaiWorkEntity;

import java.util.List;


@Dao

//====================================
//　処理概要　:　SyukkaMeisaiWorkDaoクラス
//====================================

public interface SyukkaMeisaiWorkDao {

    @Query("SELECT * FROM " +
            "W_SYUKKA_MEISAI"
    )
        //====================================================
        //　機　能　:　find Allの処理
        //　引　数　:　なし
        //　戻り値　:　[List<SyukkaMeisaiWorkEntity>] ..... なし
        //====================================================
    List<SyukkaMeisaiWorkEntity> findAll();

    @Query("SELECT * FROM " +
            "W_SYUKKA_MEISAI " +
            "WHERE " +
            "TRIM(HEAT_NO) = TRIM(:heatNo) " +
            "AND " +
            "TRIM(SOKUBAN) = TRIM(:sokuban) " +
            "LIMIT 1"
    )
        //==============================================
        //　機　能　:　find Oneの処理
        //　引　数　:　heatNo ..... String
        //　　　　　:　sokuban ..... String
        //　戻り値　:　[SyukkaMeisaiWorkEntity] ..... なし
        //==============================================
    SyukkaMeisaiWorkEntity findOne(String heatNo, String sokuban);

    //================================================
    //　機　能　:　upsertの処理
    //　引　数　:　entity ..... SyukkaMeisaiWorkEntity
    //　戻り値　:　[void] ..... なし
    //================================================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(SyukkaMeisaiWorkEntity entity);

    @Query("DELETE FROM " +
            "W_SYUKKA_MEISAI " +
            "WHERE " +
            "TRIM(HEAT_NO) = TRIM(:heatNo) " +
            "AND " +
            "TRIM(SOKUBAN) = TRIM(:sokuban)"
    )
        //=================================
        //　機　能　:　oneを削除する
        //　引　数　:　heatNo ..... String
        //　　　　　:　sokuban ..... String
        //　戻り値　:　[int] ..... なし
        //=================================
    int deleteOne(String heatNo, String sokuban);

    //============================
    //　機　能　:　allを削除する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    @Query("DELETE FROM W_SYUKKA_MEISAI")
    void deleteAll();

    @Query("SELECT " +
            " count(W.HEAT_NO) AS sokusu, " +
            " sum(T.JYURYO) AS jyuryo, " +
            " max(T.BOOKING_NO) AS bookingNo " +
            "FROM " +
            "W_SYUKKA_MEISAI W " +
            "INNER JOIN " +
            "T_SYUKKA_MEISAI T " +
            " ON " +
            "TRIM(W.HEAT_NO) = TRIM(T.HEAT_NO) " +
            "AND " +
            "TRIM(W.SOKUBAN) = TRIM(T.SOKUBAN)"
    )
        //===================================
        //　機　能　:　work Summaryを取得する
        //　引　数　:　なし
        //　戻り値　:　[WorkSummary] ..... なし
        //===================================
    WorkSummary getWorkSummary();

    // ========= C# readWorkTblToList 相当（WとTのJOIN） =========
    @Query("SELECT " +
            " W.HEAT_NO AS heatNo, " +
            " W.SOKUBAN AS sokuban, " +
            " T.SYUKKA_SASHIZU_NO AS packingNo, " +
            " T.BUNDLE_NO AS bundleNo, " +
            " T.JYURYO AS jyuryo, " +
            " T.BOOKING_NO AS bookingNo " +
            "FROM " +
            "W_SYUKKA_MEISAI W " +
            "INNER JOIN " +
            "T_SYUKKA_MEISAI T " +
            " ON " +
            "TRIM(W.HEAT_NO) = TRIM(T.HEAT_NO) " +
            "AND " +
            "TRIM(W.SOKUBAN) = TRIM(T.SOKUBAN) " +
            "ORDER BY " +
            "W.INSERT_YMD"
    )
    //=========================================
    //　機　能　:　select Work Joinedの処理
    //　引　数　:　なし
    //　戻り値　:　[List<WorkJoinRow>] ..... なし
    //=========================================
    List<WorkJoinRow> selectWorkJoined();

    class WorkJoinRow {
        public String heatNo;
        public String sokuban;
        public String packingNo;
        public String bundleNo;
        public Integer jyuryo;
        public String bookingNo;
    }

    class WorkSummary {
        public int sokusu;
        public Integer jyuryo;
        public String bookingNo;
    }
}
