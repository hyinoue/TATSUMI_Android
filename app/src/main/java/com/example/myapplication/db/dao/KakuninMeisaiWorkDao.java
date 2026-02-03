package com.example.myapplication.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myapplication.db.entity.KakuninMeisaiWorkEntity;

import java.util.List;


@Dao

//===============================
//　処理概要　:　KakuninMeisaiWorkDaoクラス
//===============================

public interface KakuninMeisaiWorkDao {

    @Query("SELECT * FROM " +
            "W_KAKUNIN_MEISAI"
    )
    //===============================================
    //　機　能　:　find Allの処理
    //　引　数　:　なし
    //　戻り値　:　[List<KakuninMeisaiWorkEntity>] ..... なし
    //===============================================
    List<KakuninMeisaiWorkEntity> findAll();

    @Query("SELECT * FROM " +
            "W_KAKUNIN_MEISAI " +
            "ORDER BY " +
            "SYUKKA_SASHIZU_NO, " +
            "BUNDLE_NO, " +
            "SOKUBAN"
    )
    //===============================================
    //　機　能　:　find All Orderedの処理
    //　引　数　:　なし
    //　戻り値　:　[List<KakuninMeisaiWorkEntity>] ..... なし
    //===============================================
    List<KakuninMeisaiWorkEntity> findAllOrdered();

    @Query("SELECT * FROM " +
            "W_KAKUNIN_MEISAI " +
            "WHERE " +
            "TRIM(HEAT_NO) = TRIM(:heatNo) " +
            "AND " +
            "TRIM(SOKUBAN) = TRIM(:sokuban)"
    )
    //=========================================
    //　機　能　:　find Oneの処理
    //　引　数　:　heatNo ..... String
    //　　　　　:　sokuban ..... String
    //　戻り値　:　[KakuninMeisaiWorkEntity] ..... なし
    //=========================================
    KakuninMeisaiWorkEntity findOne(String heatNo, String sokuban);

    //===========================================
    //　機　能　:　upsertの処理
    //　引　数　:　entity ..... KakuninMeisaiWorkEntity
    //　戻り値　:　[void] ..... なし
    //===========================================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(KakuninMeisaiWorkEntity entity);

    @Query("DELETE FROM " +
            "W_KAKUNIN_MEISAI " +
            "WHERE " +
            "TRIM(HEAT_NO) = TRIM(:heatNo) " +
            "AND " +
            "TRIM(SOKUBAN) = TRIM(:sokuban)")
    //===========================
    //　機　能　:　oneを削除する
    //　引　数　:　heatNo ..... String
    //　　　　　:　sokuban ..... String
    //　戻り値　:　[int] ..... なし
    //===========================
    int deleteOne(String heatNo, String sokuban);

    @Query("DELETE FROM" +
            " W_KAKUNIN_MEISAI")
    //======================
    //　機　能　:　allを削除する
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //======================
    void deleteAll();
}
