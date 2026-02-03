package com.example.myapplication.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RoomWarnings;

import com.example.myapplication.db.entity.SyukkaMeisaiEntity;

import java.util.List;


@Dao

//================================
//　処理概要　:　SyukkaMeisaiDaoクラス
//================================

public interface SyukkaMeisaiDao {

    @Query("SELECT * FROM " +
            "T_SYUKKA_MEISAI " +
            "WHERE " +
            "TRIM(HEAT_NO) = TRIM(:heatNo) " +
            "AND " +
            "TRIM(SOKUBAN) = TRIM(:sokuban) LIMIT 1"
    )
        //==========================================
        //　機　能　:　find Oneの処理
        //　引　数　:　heatNo ..... String
        //　　　　　:　sokuban ..... String
        //　戻り値　:　[SyukkaMeisaiEntity] ..... なし
        //==========================================
    SyukkaMeisaiEntity findOne(String heatNo, String sokuban);

    @Query("SELECT * FROM " +
            "T_SYUKKA_MEISAI " +
            "WHERE " +
            "TRIM(BOOKING_NO) = TRIM(:bookingNo)"
    )
        //================================================
        //　機　能　:　find By Booking Noの処理
        //　引　数　:　bookingNo ..... String
        //　戻り値　:　[List<SyukkaMeisaiEntity>] ..... なし
        //================================================
    List<SyukkaMeisaiEntity> findByBookingNo(String bookingNo);

    @Query("UPDATE " +
            "T_SYUKKA_MEISAI " +
            "SET " +
            "BUNDLE_NO = :bundleNo " +
            "WHERE " +
            "TRIM(HEAT_NO) = TRIM(:heatNo) " +
            "AND " +
            "TRIM(SOKUBAN) = TRIM(:sokuban)"
    )
        //==================================
        //　機　能　:　bundle Noを更新する
        //　引　数　:　heatNo ..... String
        //　　　　　:　sokuban ..... String
        //　　　　　:　bundleNo ..... String
        //　戻り値　:　[int] ..... なし
        //==================================
    int updateBundleNo(String heatNo, String sokuban, String bundleNo);

    @Query("UPDATE " +
            "T_SYUKKA_MEISAI " +
            "SET " +
            "SYUKKA_SASHIZU_NO = :syukkaSashizuNo, " +
            "BUNDLE_NO = :bundleNo, " +
            "JYURYO = :jyuryo, " +
            "BOOKING_NO = :bookingNo " +
            "WHERE " +
            "TRIM(HEAT_NO) = TRIM(:heatNo) " +
            "AND " +
            "TRIM(SOKUBAN) = TRIM(:sokuban)"
    )
        //=========================================
        //　機　能　:　from Receiveを更新する
        //　引　数　:　heatNo ..... String
        //　　　　　:　sokuban ..... String
        //　　　　　:　syukkaSashizuNo ..... String
        //　　　　　:　bundleNo ..... String
        //　　　　　:　jyuryo ..... Integer
        //　　　　　:　bookingNo ..... String
        //　戻り値　:　[int] ..... なし
        //=========================================
    int updateFromReceive(String heatNo,
                          String sokuban,
                          String syukkaSashizuNo,
                          String bundleNo,
                          Integer jyuryo,
                          String bookingNo);

    //============================================
    //　機　能　:　insertの処理
    //　引　数　:　entity ..... SyukkaMeisaiEntity
    //　戻り値　:　[void] ..... なし
    //============================================
    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insert(SyukkaMeisaiEntity entity);

    @Query("SELECT * FROM " +
            "T_SYUKKA_MEISAI " +
            "WHERE " +
            "CONTAINER_ID = :containerId"
    )
        //================================================
        //　機　能　:　find By Container Idの処理
        //　引　数　:　containerId ..... int
        //　戻り値　:　[List<SyukkaMeisaiEntity>] ..... なし
        //================================================
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
        //=============================
        //　機　能　:　sent Linkedを削除する
        //　引　数　:　なし
        //　戻り値　:　[void] ..... なし
        //=============================
    void deleteSentLinked();

    @Query("UPDATE T_SYUKKA_MEISAI SET CONTAINER_ID = :containerId " +
            "WHERE " +
            "EXISTS(" +
            "SELECT 1 FROM " +
            "W_SYUKKA_MEISAI W " +
            "WHERE " +
            "TRIM(W.HEAT_NO) = TRIM(T_SYUKKA_MEISAI.HEAT_NO) " +
            "AND " +
            "TRIM(W.SOKUBAN) = TRIM(T_SYUKKA_MEISAI.SOKUBAN))"
    )
        //=======================================
        //　機　能　:　container Id For Workを更新する
        //　引　数　:　containerId ..... int
        //　戻り値　:　[int] ..... なし
        //=======================================
    int updateContainerIdForWork(int containerId);

    @Query("DELETE FROM " +
            "T_SYUKKA_MEISAI"
    )
        //============================
        //　機　能　:　allを削除する
        //　引　数　:　なし
        //　戻り値　:　[void] ..... なし
        //============================
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
    //=================================================================================================
    //　機　能　:　find One For Checkの処理
    //　引　数　:　heatNo ..... RoomWarnings.CURSOR_MISMATCH) SyukkaMeisaiEntity findOneForCheck(String
    //　　　　　:　sokuban ..... String
    //　戻り値　:　[(RoomWarnings.CURSOR_MISMATCH)] ..... なし
    //=================================================================================================
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
    //==========================================
    //　機　能　:　find One For Addの処理
    //　引　数　:　heatNo ..... String
    //　　　　　:　sokuban ..... String
    //　戻り値　:　[SyukkaMeisaiEntity] ..... なし
    //==========================================
    SyukkaMeisaiEntity findOneForAdd(String heatNo, String sokuban);


    // AddBundleNo用（BUNDLE_NOが空の時だけ更新）
    @Query("UPDATE " +
            "T_SYUKKA_MEISAI " +
            "SET " +
            "BUNDLE_NO = :bundleNo " +
            "WHERE " +
            "TRIM(HEAT_NO) = TRIM(:heatNo) " +
            "AND " +
            "TRIM(SOKUBAN) = TRIM(:sokuban) " +
            "AND " +
            "(BUNDLE_NO IS NULL OR BUNDLE_NO = '')"
    )
    //====================================
    //　機　能　:　bundle No If Emptyを更新する
    //　引　数　:　heatNo ..... String
    //　　　　　:　sokuban ..... String
    //　　　　　:　bundleNo ..... String
    //　戻り値　:　[int] ..... なし
    //====================================
    int updateBundleNoIfEmpty(String heatNo, String sokuban, String bundleNo);
}
