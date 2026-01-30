package com.example.myapplication.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myapplication.db.entity.SystemEntity;


@Dao

//============================================================
//　処理概要　:　SystemDaoクラス
//============================================================

public interface SystemDao {

    @Query("SELECT * FROM " +
            "M_SYSTEM " +
            "WHERE " +
            "RENBAN = :renban"
    )
    SystemEntity findById(int renban);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(SystemEntity entity);

    @Query("UPDATE " +
            "M_SYSTEM " +
            "SET " +
            "DATA_CONF_YMDHMS = :dataConfYmdhms " +
            "WHERE " +
            "RENBAN = :renban"
    )
    int updateDataConf(int renban, String dataConfYmdhms);

    @Query("UPDATE " +
            "M_SYSTEM " +
            "SET " +
            "DATA_CONF_YMDHMS = :dataConfYmdhms, " +
            "DATA_RECV_YMDHMS = :dataRecvYmdhms " +
            "WHERE " +
            "RENBAN = :renban"
    )
    int updateDataSync(int renban, String dataConfYmdhms, String dataRecvYmdhms);

    @Query("DELETE FROM " +
            "M_SYSTEM"
    )
    void deleteAll();
}
