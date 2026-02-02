package com.example.myapplication.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myapplication.db.entity.KakuninContainerEntity;

import java.util.List;


@Dao

//============================================================
//　処理概要　:　KakuninContainerDaoクラス
//============================================================

public interface KakuninContainerDao {

    @Query("SELECT * FROM " +
            "T_KAKUNIN_CONTAINER " +
            "WHERE " +
            "TRIM(CONTAINER_ID) = TRIM(:containerId)"
    )
    KakuninContainerEntity findByContainerId(String containerId);

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
    List<KakuninContainerEntity> findUnsentCompleted();

    @Query("UPDATE " +
            "T_KAKUNIN_CONTAINER " +
            "SET " +
            "DATA_SEND_YMDHMS = :dataSendYmdhms " +
            "WHERE " +
            "TRIM(CONTAINER_ID) = TRIM(:containerId)"
    )
    int markSent(String containerId, String dataSendYmdhms);

    @Query("DELETE FROM " +
            "T_KAKUNIN_CONTAINER")
    void deleteAll();

    @Query("SELECT * FROM " +
            "T_KAKUNIN_CONTAINER " +
            "WHERE " +
            "CONTAINER_SYOUGO_KANRYO = 0 " +
            "ORDER BY " +
            "CONTAINER_ID"
    )
    List<KakuninContainerEntity> findUncollated();
}
