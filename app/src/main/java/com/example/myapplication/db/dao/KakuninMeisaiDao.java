package com.example.myapplication.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myapplication.db.entity.KakuninMeisaiEntity;

import java.util.List;


@Dao

//============================================================
//　処理概要　:　KakuninMeisaiDaoクラス
//============================================================

public interface KakuninMeisaiDao {

    @Query("SELECT * FROM " +
            "T_KAKUNIN_MEISAI " +
            "WHERE " +
            "TRIM(HEAT_NO) = TRIM(:heatNo) " +
            "AND " +
            "TRIM(SOKUBAN) = TRIM(:sokuban)"
    )
    KakuninMeisaiEntity findOne(String heatNo, String sokuban);

    @Query("SELECT * FROM " +
            "T_KAKUNIN_MEISAI " +
            "WHERE " +
            "TRIM(CONTAINER_ID) = TRIM(:containerId)"
    )
    List<KakuninMeisaiEntity> findByContainerId(String containerId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(KakuninMeisaiEntity entity);

    @Query("DELETE FROM" +
            " T_KAKUNIN_MEISAI"
    )
    void deleteAll();
}
