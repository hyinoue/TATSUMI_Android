package com.example.myapplication.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myapplication.db.entity.KakuninContainerEntity;

import java.util.List;


@Dao

//====================================
//　処理概要　:　KakuninContainerDaoクラス
//====================================

public interface KakuninContainerDao {

    @Query("SELECT * FROM " +
            "T_KAKUNIN_CONTAINER " +
            "WHERE " +
            "TRIM(CONTAINER_ID) = TRIM(:containerId)"
    )
        //==============================================
        //　機　能　:　find By Container Idの処理
        //　引　数　:　containerId ..... String
        //　戻り値　:　[KakuninContainerEntity] ..... なし
        //==============================================
    KakuninContainerEntity findByContainerId(String containerId);

    //================================================
    //　機　能　:　upsertの処理
    //　引　数　:　entity ..... KakuninContainerEntity
    //　戻り値　:　[void] ..... なし
    //================================================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(KakuninContainerEntity entity);

    @Query("SELECT * FROM " +
            "T_KAKUNIN_CONTAINER " +
            "WHERE " +
            "CONTAINER_SYOUGO_KANRYO = 1 " +
            "AND " +
            "DATA_SEND_YMDHMS IS NULL " +
            "ORDER BY " +
            "CONTAINER_ID"
    )
        //====================================================
        //　機　能　:　find Unsent Completedの処理
        //　引　数　:　なし
        //　戻り値　:　[List<KakuninContainerEntity>] ..... なし
        //====================================================
    List<KakuninContainerEntity> findUnsentCompleted();

    @Query("UPDATE " +
            "T_KAKUNIN_CONTAINER " +
            "SET " +
            "DATA_SEND_YMDHMS = :dataSendYmdhms " +
            "WHERE " +
            "TRIM(CONTAINER_ID) = TRIM(:containerId)"
    )
        //========================================
        //　機　能　:　mark Sentの処理
        //　引　数　:　containerId ..... String
        //　　　　　:　dataSendYmdhms ..... String
        //　戻り値　:　[int] ..... なし
        //========================================
    int markSent(String containerId, String dataSendYmdhms);

    @Query("DELETE FROM " +
            "T_KAKUNIN_CONTAINER")
        //============================
        //　機　能　:　allを削除する
        //　引　数　:　なし
        //　戻り値　:　[void] ..... なし
        //============================
    void deleteAll();

    @Query("SELECT * FROM " +
            "T_KAKUNIN_CONTAINER " +
            "WHERE " +
            "CONTAINER_SYOUGO_KANRYO = 0 " +
            "ORDER BY " +
            "CONTAINER_ID"
    )
        //====================================================
        //　機　能　:　find Uncollatedの処理
        //　引　数　:　なし
        //　戻り値　:　[List<KakuninContainerEntity>] ..... なし
        //====================================================
    List<KakuninContainerEntity> findUncollated();
}
