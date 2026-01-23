package com.example.myapplication.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myapplication.db.entity.SyukkaMeisaiWorkEntity;

import java.util.List;

@Dao
public interface SyukkaMeisaiWorkDao {

    @Query("SELECT * FROM W_SYUKKA_MEISAI")
    List<SyukkaMeisaiWorkEntity> findAll();

    @Query("SELECT * FROM W_SYUKKA_MEISAI WHERE HEAT_NO = :heatNo AND SOKUBAN = :sokuban LIMIT 1")
    SyukkaMeisaiWorkEntity findOne(String heatNo, String sokuban);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(SyukkaMeisaiWorkEntity entity);

    @Query("DELETE FROM W_SYUKKA_MEISAI WHERE HEAT_NO = :heatNo AND SOKUBAN = :sokuban")
    int deleteOne(String heatNo, String sokuban);

    @Query("DELETE FROM W_SYUKKA_MEISAI")
    void deleteAll();

    // ========= C# readWorkTblToList 相当（WとTのJOIN） =========
    @Query("SELECT " +
            " W.HEAT_NO AS heatNo, " +
            " W.SOKUBAN AS sokuban, " +
            " T.SYUKKA_SASHIZU_NO AS packingNo, " +
            " T.BUNDLE_NO AS bundleNo, " +
            " T.JYURYO AS jyuryo, " +
            " T.BOOKING_NO AS bookingNo " +
            "FROM W_SYUKKA_MEISAI W " +
            "INNER JOIN T_SYUKKA_MEISAI T " +
            " ON W.HEAT_NO = T.HEAT_NO AND W.SOKUBAN = T.SOKUBAN " +
            "ORDER BY W.UPDATE_YMD")
    List<WorkJoinRow> selectWorkJoined();

    class WorkJoinRow {
        public String heatNo;
        public String sokuban;
        public String packingNo;
        public String bundleNo;
        public Integer jyuryo;
        public String bookingNo;
    }
}
