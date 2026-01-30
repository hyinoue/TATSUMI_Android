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
            "HEAT_NO = :heatNo " +
            "AND " +
            "SOKUBAN = :sokuban"
    )
    KakuninMeisaiEntity findOne(String heatNo, String sokuban);

    @Query("SELECT * FROM " +
            "T_KAKUNIN_MEISAI " +
            "WHERE " +
            "CONTAINER_ID = :containerId"
    )
    List<KakuninMeisaiEntity> findByContainerId(String containerId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(KakuninMeisaiEntity entity);

    @Query("DELETE FROM" +
            " T_KAKUNIN_MEISAI"
    )
    void deleteAll();
}
