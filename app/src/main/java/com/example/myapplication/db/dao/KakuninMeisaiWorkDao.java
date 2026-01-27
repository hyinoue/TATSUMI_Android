package com.example.myapplication.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myapplication.db.entity.KakuninMeisaiWorkEntity;

import java.util.List;

@Dao
public interface KakuninMeisaiWorkDao {

    @Query("SELECT * FROM " +
            "W_KAKUNIN_MEISAI"
    )
    List<KakuninMeisaiWorkEntity> findAll();

    @Query("SELECT * FROM " +
            "W_KAKUNIN_MEISAI " +
            "WHERE " +
            "HEAT_NO = :heatNo " +
            "AND " +
            "SOKUBAN = :sokuban"
    )
    KakuninMeisaiWorkEntity findOne(String heatNo, String sokuban);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(KakuninMeisaiWorkEntity entity);

    @Query("DELETE FROM " +
            "W_KAKUNIN_MEISAI " +
            "WHERE " +
            "HEAT_NO = :heatNo " +
            "AND " +
            "SOKUBAN = :sokuban")
    int deleteOne(String heatNo, String sokuban);

    @Query("DELETE FROM" +
            " W_KAKUNIN_MEISAI")
    void deleteAll();
}
