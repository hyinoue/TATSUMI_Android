package com.example.myapplication.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myapplication.db.entity.KakuninMeisaiEntity;

import java.util.List;


@Dao

//=================================
//　処理概要　:　KakuninMeisaiDaoクラス
//=================================

public interface KakuninMeisaiDao {

    @Query("SELECT * FROM " +
            "T_KAKUNIN_MEISAI " +
            "WHERE " +
            "TRIM(HEAT_NO) = TRIM(:heatNo) " +
            "AND " +
            "TRIM(SOKUBAN) = TRIM(:sokuban)"
    )
        //===========================================
        //　機　能　:　find Oneの処理
        //　引　数　:　heatNo ..... String
        //　　　　　:　sokuban ..... String
        //　戻り値　:　[KakuninMeisaiEntity] ..... なし
        //===========================================
    KakuninMeisaiEntity findOne(String heatNo, String sokuban);

    @Query("SELECT * FROM " +
            "T_KAKUNIN_MEISAI " +
            "WHERE " +
            "TRIM(CONTAINER_ID) = TRIM(:containerId)"
    )
        //=================================================
        //　機　能　:　find By Container Idの処理
        //　引　数　:　containerId ..... String
        //　戻り値　:　[List<KakuninMeisaiEntity>] ..... なし
        //=================================================
    List<KakuninMeisaiEntity> findByContainerId(String containerId);

    //=============================================
    //　機　能　:　upsertの処理
    //　引　数　:　entity ..... KakuninMeisaiEntity
    //　戻り値　:　[void] ..... なし
    //=============================================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(KakuninMeisaiEntity entity);

    @Query("DELETE FROM" +
            " T_KAKUNIN_MEISAI"
    )
        //============================
        //　機　能　:　allを削除する
        //　引　数　:　なし
        //　戻り値　:　[void] ..... なし
        //============================
    void deleteAll();
}
