package com.example.myapplication.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RoomWarnings;

import com.example.myapplication.db.entity.SyukkaMeisaiEntity;

import java.util.List;


//============================================================
//　処理概要　:　出荷明細テーブル（T_SYUKKA_MEISAI）に対するDAO
//　関　　数　:　findOne                  ..... 明細単一取得
//　　　　　　:　findByBookingNo          ..... 予約No.検索
//　　　　　　:　updateBundleNo           ..... バンドルNo.更新
//　　　　　　:　updateFromReceive        ..... 受信データによる更新
//　　　　　　:　insert                   ..... 新規登録
//　　　　　　:　findByContainerId        ..... コンテナID検索
//　　　　　　:　deleteSentLinked         ..... 送信済コンテナ紐付データ削除
//　　　　　　:　updateContainerIdForWork ..... ワーク明細に存在するものへコンテナID一括更新
//　　　　　　:　deleteAll                ..... 全件削除
//　　　　　　:　findOneForCheck          ..... Controller用（存在チェック）
//　　　　　　:　findOneForAdd            ..... Controller用（AddBundle用取得）
//　　　　　　:　updateBundleNoIfEmpty    ..... Controller用（空の場合のみバンドル更新）
//============================================================
@Dao
public interface SyukkaMeisaiDao {

    //================================================================
    //　機　能　:　HEAT_NOとSOKUBANを指定して明細を1件取得する
    //　引　数　:　heatNo  ..... 鋼番
    //　　　　　:　sokuban ..... 束番
    //　戻り値　:　[SyukkaMeisaiEntity] ..... 該当データ（存在しない場合はnull）
    //================================================================
    @Query(
            "SELECT * FROM " +
                    "T_SYUKKA_MEISAI " +
                    "WHERE " +
                    "TRIM(HEAT_NO) = TRIM(:heatNo) " +
                    "AND " +
                    "TRIM(SOKUBAN) = TRIM(:sokuban) " +
                    "LIMIT 1"
    )
    SyukkaMeisaiEntity findOne(String heatNo, String sokuban);
    // ・複合条件（HEAT_NO/SOKUBAN）で検索
    // ・前後空白を除去して比較
    // ・LIMIT 1 により先頭1件のみ取得


    //================================================================
    //　機　能　:　予約No.を指定して明細一覧を取得する
    //　引　数　:　bookingNo ..... 予約No.
    //　戻り値　:　[List<SyukkaMeisaiEntity>] ..... 該当明細一覧
    //================================================================
    @Query(
            "SELECT * FROM " +
                    "T_SYUKKA_MEISAI " +
                    "WHERE " +
                    "TRIM(BOOKING_NO) = TRIM(:bookingNo)"
    )
    List<SyukkaMeisaiEntity> findByBookingNo(String bookingNo);
    // ・BOOKING_NO一致で抽出
    // ・前後空白を除去して比較


    //================================================================
    //　機　能　:　バンドルNo.を更新する
    //　引　数　:　heatNo   ..... 鋼番
    //　　　　　:　sokuban  ..... 束番
    //　　　　　:　bundleNo ..... バンドルNo.
    //　戻り値　:　[int] ..... 更新件数
    //================================================================
    @Query(
            "UPDATE " +
                    "T_SYUKKA_MEISAI " +
                    "SET " +
                    "BUNDLE_NO = :bundleNo " +
                    "WHERE " +
                    "TRIM(HEAT_NO) = TRIM(:heatNo) " +
                    "AND " +
                    "TRIM(SOKUBAN) = TRIM(:sokuban)"
    )
    int updateBundleNo(String heatNo, String sokuban, String bundleNo);
    // ・対象明細のBUNDLE_NOを更新
    // ・更新件数を返却（0の場合は該当なし）


    //================================================================
    //　機　能　:　受信データに基づき明細情報を更新する
    //　引　数　:　heatNo           ..... 鋼番
    //　　　　　:　sokuban          ..... 束番
    //　　　　　:　syukkaSashizuNo  ..... 出荷指図No.
    //　　　　　:　bundleNo         ..... バンドルNo.
    //　　　　　:　jyuryo           ..... 重量
    //　　　　　:　bookingNo        ..... 予約No.
    //　戻り値　:　[int] ..... 更新件数
    //================================================================
    @Query(
            "UPDATE " +
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
    int updateFromReceive(String heatNo,
                          String sokuban,
                          String syukkaSashizuNo,
                          String bundleNo,
                          Integer jyuryo,
                          String bookingNo);
    // ・受信処理で取得した情報を一括反映
    // ・該当明細（HEAT_NO/SOKUBAN）を更新
    // ・更新件数を返却


    //================================================================
    //　機　能　:　出荷明細データを新規登録する
    //　引　数　:　entity ..... SyukkaMeisaiEntity
    //　戻り値　:　[void]
    //================================================================
    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insert(SyukkaMeisaiEntity entity);
    // ・競合時はABORT（挿入失敗）
    // ・重複登録を許容しない前提の登録処理


    //================================================================
    //　機　能　:　コンテナIDを指定して明細一覧を取得する
    //　引　数　:　containerId ..... コンテナID
    //　戻り値　:　[List<SyukkaMeisaiEntity>] ..... 該当明細一覧
    //================================================================
    @Query(
            "SELECT * FROM " +
                    "T_SYUKKA_MEISAI " +
                    "WHERE " +
                    "CONTAINER_ID = :containerId"
    )
    List<SyukkaMeisaiEntity> findByContainerId(int containerId);
    // ・指定コンテナに紐づく明細を取得


    //================================================================
    //　機　能　:　送信済コンテナに紐づく明細を削除する
    //　引　数　:　なし
    //　戻り値　:　[void]
    //================================================================
    @Query(
            "DELETE FROM " +
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
    // ・明細側にCONTAINER_IDが設定されているものが対象
    // ・紐付くコンテナが送信済（DATA_SEND_YMDHMSがNOT NULL）の場合に削除
    // ・送信後の整理用途


    //================================================================
    //　機　能　:　ワーク明細（W_SYUKKA_MEISAI）に存在する明細へコンテナIDを一括設定する
    //　引　数　:　containerId ..... 設定するコンテナID
    //　戻り値　:　[int] ..... 更新件数
    //================================================================
    @Query(
            "UPDATE T_SYUKKA_MEISAI SET CONTAINER_ID = :containerId " +
                    "WHERE " +
                    "EXISTS(" +
                    "SELECT 1 FROM " +
                    "W_SYUKKA_MEISAI W " +
                    "WHERE " +
                    "TRIM(W.HEAT_NO) = TRIM(T_SYUKKA_MEISAI.HEAT_NO) " +
                    "AND " +
                    "TRIM(W.SOKUBAN) = TRIM(T_SYUKKA_MEISAI.SOKUBAN))"
    )
    int updateContainerIdForWork(int containerId);
    // ・ワーク側に存在する（HEAT_NO/SOKUBAN一致）明細を対象に更新
    // ・一括でCONTAINER_IDを付与（コンテナ紐付け確定処理などで使用）
    // ・更新件数を返却


    //================================================================
    //　機　能　:　出荷明細データを全件削除する
    //　引　数　:　なし
    //　戻り値　:　[void]
    //================================================================
    @Query(
            "DELETE FROM " +
                    "T_SYUKKA_MEISAI"
    )
    void deleteAll();
    // ・テーブル内全レコード削除
    // ・初期化処理などで使用


    //================================================================
    //　機　能　:　（Controller用）存在チェック用に必要項目のみ取得する
    //　引　数　:　heatNo  ..... 鋼番
    //　　　　　:　sokuban ..... 束番
    //　戻り値　:　[SyukkaMeisaiEntity] ..... 該当データ（存在しない場合はnull）
    //================================================================
    @Query(
            "SELECT HEAT_NO, SOKUBAN, JYURYO, CONTAINER_ID, BOOKING_NO " +
                    "FROM " +
                    "T_SYUKKA_MEISAI " +
                    "WHERE " +
                    "TRIM(HEAT_NO) = TRIM(:heatNo) " +
                    "AND " +
                    "TRIM(SOKUBAN) = TRIM(:sokuban) " +
                    "LIMIT 1"
    )
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    SyukkaMeisaiEntity findOneForCheck(String heatNo, String sokuban);
    // ・Entityの全カラムとSELECT対象カラムが一致しないため警告抑制
    // ・存在確認／最低限の表示用情報取得を目的とする
    // ・LIMIT 1 により先頭1件のみ取得


    //================================================================
    //　機　能　:　（Controller用）AddBundle用に明細を1件取得する
    //　引　数　:　heatNo  ..... 鋼番
    //　　　　　:　sokuban ..... 束番
    //　戻り値　:　[SyukkaMeisaiEntity] ..... 該当データ（存在しない場合はnull）
    //================================================================
    @Query(
            "SELECT * FROM " +
                    "T_SYUKKA_MEISAI " +
                    "WHERE " +
                    "TRIM(HEAT_NO) = TRIM(:heatNo) " +
                    "AND " +
                    "TRIM(SOKUBAN) = TRIM(:sokuban)"
    )
    SyukkaMeisaiEntity findOneForAdd(String heatNo, String sokuban);
    // ・AddBundle処理で対象明細を取得
    // ・（コメント上の意図としてCONTAINER_ID IS NULL条件等が必要ならSQL側に追加する想定）
    // ・前後空白を除去して比較


    //================================================================
    //　機　能　:　（Controller用）バンドルNo.が未設定の場合のみ更新する
    //　引　数　:　heatNo   ..... 鋼番
    //　　　　　:　sokuban  ..... 束番
    //　　　　　:　bundleNo ..... バンドルNo.
    //　戻り値　:　[int] ..... 更新件数
    //================================================================
    @Query(
            "UPDATE " +
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
    int updateBundleNoIfEmpty(String heatNo, String sokuban, String bundleNo);
    // ・BUNDLE_NOがNULLまたは空文字の場合のみ更新
    // ・既に値がある場合は上書きしない
    // ・更新件数を返却

}