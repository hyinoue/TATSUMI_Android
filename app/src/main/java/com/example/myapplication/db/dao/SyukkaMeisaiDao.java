package com.example.myapplication.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RoomWarnings;

import com.example.myapplication.db.entity.SyukkaMeisaiEntity;

import java.util.List;


@Dao

//============================================================
//　処理概要　:　SyukkaMeisaiDaoクラス
//============================================================

public interface SyukkaMeisaiDao {

    @Query("SELECT * FROM " +
            "T_SYUKKA_MEISAI " +
            "WHERE " +
            "HEAT_NO = :heatNo " +
            "AND " +
            "SOKUBAN = :sokuban LIMIT 1"
    )
    SyukkaMeisaiEntity findOne(String heatNo, String sokuban);

    @Query("SELECT * FROM " +
            "T_SYUKKA_MEISAI " +
            "WHERE " +
            "BOOKING_NO = :bookingNo"
    )
    List<SyukkaMeisaiEntity> findByBookingNo(String bookingNo);

    @Query("UPDATE " +
            "T_SYUKKA_MEISAI " +
            "SET " +
            "BUNDLE_NO = :bundleNo " +
            "WHERE " +
            "HEAT_NO = :heatNo " +
            "AND " +
            "SOKUBAN = :sokuban"
    )
    int updateBundleNo(String heatNo, String sokuban, String bundleNo);

    @Query("UPDATE " +
            "T_SYUKKA_MEISAI " +
            "SET " +
            "SYUKKA_SASHIZU_NO = :syukkaSashizuNo, " +
            "BUNDLE_NO = :bundleNo, " +
            "JYURYO = :jyuryo, " +
            "BOOKING_NO = :bookingNo " +
            "WHERE " +
            "HEAT_NO = :heatNo " +
            "AND " +
            "SOKUBAN = :sokuban"
    )
    int updateFromReceive(String heatNo,
                          String sokuban,
                          String syukkaSashizuNo,
                          String bundleNo,
                          Integer jyuryo,
                          String bookingNo);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insert(SyukkaMeisaiEntity entity);

    @Query("SELECT * FROM " +
            "T_SYUKKA_MEISAI " +
            "WHERE " +
            "CONTAINER_ID = :containerId"
    )
    List<SyukkaMeisaiEntity> findByContainerId(int containerId);

    @Query("DELETE FROM " +
            "T_SYUKKA_MEISAI " +
            "WHERE " +
            "CONTAINER_ID IS NOT NULL " +
            "AND " +
            "EXISTS(" +
            "SELECT 1 FROM " +
            "T_SYUKKA_CONTAINER " +
            "WHERE " +
            "T_SYUKKA_CONTAINER.CONTAINER_ID = T_SYUKKA_MEISAI.CONTAINER_ID " +
            "AND " +
            "T_SYUKKA_CONTAINER.DATA_SEND_YMDHMS IS NOT NULL)"
    )
    void deleteSentLinked();

    @Query("UPDATE T_SYUKKA_MEISAI SET CONTAINER_ID = :containerId " +
            "WHERE " +
            "EXISTS(" +
            "SELECT 1 FROM " +
            "W_SYUKKA_MEISAI W " +
            "WHERE " +
            "W.HEAT_NO = T_SYUKKA_MEISAI.HEAT_NO " +
            "AND " +
            "W.SOKUBAN = T_SYUKKA_MEISAI.SOKUBAN)"
    )
    int updateContainerIdForWork(int containerId);

    @Query("DELETE FROM " +
            "T_SYUKKA_MEISAI"
    )
    void deleteAll();

    // ========= Controller用 =========

    // 存在チェック用（CheckBundleで使う）
    @Query(
            "SELECT HEAT_NO, SOKUBAN, JYURYO, CONTAINER_ID, BOOKING_NO " +
                    "FROM " +
                    "T_SYUKKA_MEISAI " +
                    "WHERE " +
                    "trim(HEAT_NO) = trim(:heatNo) " +
                    "AND " +
                    "trim(SOKUBAN) = trim(:sokuban) " +
                    "LIMIT 1"
    )
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    SyukkaMeisaiEntity findOneForCheck(String heatNo, String sokuban);


    // AddBundle用（CONTAINER_ID IS NULL 条件）
    @Query(
            "SELECT * FROM " +
                    "T_SYUKKA_MEISAI " +
                    "WHERE " +
                    "trim(HEAT_NO) = trim(:heatNo) " +
                    "AND " +
                    "trim(SOKUBAN) = trim(:sokuban)"
    )
    SyukkaMeisaiEntity findOneForAdd(String heatNo, String sokuban);


    // AddBundleNo用（BUNDLE_NOが空の時だけ更新）
    @Query("UPDATE " +
            "T_SYUKKA_MEISAI " +
            "SET " +
            "BUNDLE_NO = :bundleNo " +
            "WHERE " +
            "HEAT_NO = :heatNo " +
            "AND " +
            "SOKUBAN = :sokuban " +
            "AND " +
            "(BUNDLE_NO IS NULL OR BUNDLE_NO = '')"
    )
    int updateBundleNoIfEmpty(String heatNo, String sokuban, String bundleNo);
}
