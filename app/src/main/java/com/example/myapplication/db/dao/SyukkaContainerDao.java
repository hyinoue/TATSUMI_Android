package com.example.myapplication.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myapplication.db.entity.SyukkaContainerEntity;

import java.util.List;


//============================================================
//　処理概要　:　出荷コンテナテーブル（T_SYUKKA_CONTAINER）に対するDAO
//　関　　数　:　findById            ..... コンテナID検索
//　　　　　　:　findByBookingNo     ..... 予約No.検索
//　　　　　　:　upsert              ..... 追加／更新
//　　　　　　:　findUnsent          ..... 未送信データ取得
//　　　　　　:　getMaxContainerId   ..... 最大コンテナID取得
//　　　　　　:　markSent            ..... 送信済更新
//　　　　　　:　deleteSent          ..... 送信済削除
//　　　　　　:　deleteAll           ..... 全件削除
//============================================================
@Dao
public interface SyukkaContainerDao {

    //================================================================
    //　機　能　:　コンテナIDを指定してデータを取得する
    //　引　数　:　containerId ..... コンテナID
    //　戻り値　:　[SyukkaContainerEntity] ..... 該当データ（存在しない場合はnull）
    //================================================================
    @Query(
            "SELECT * FROM " +
                    "T_SYUKKA_CONTAINER " +
                    "WHERE " +
                    "CONTAINER_ID = :containerId"
    )
    SyukkaContainerEntity findById(int containerId);
    // ・主キー検索
    // ・該当なしの場合はnullを返却


    //================================================================
    //　機　能　:　予約No.に紐づくコンテナを取得する
    //　引　数　:　bookingNo ..... 予約No.
    //　戻り値　:　[List<SyukkaContainerEntity>] ..... 該当コンテナ一覧
    //================================================================
    @Query(
            "SELECT C.* FROM T_SYUKKA_CONTAINER C " +
                    "INNER JOIN T_SYUKKA_MEISAI M " +
                    " ON C.CONTAINER_ID = M.CONTAINER_ID " +
                    "WHERE TRIM(M.BOOKING_NO) = TRIM(:bookingNo)"
    )
    List<SyukkaContainerEntity> findByBookingNo(String bookingNo);
    // ・明細テーブルと内部結合
    // ・BOOKING_NO一致データを抽出
    // ・前後空白を除去して比較
    // ・該当するコンテナを複数件取得


    //================================================================
    //　機　能　:　出荷コンテナデータを追加または更新する（Upsert処理）
    //　引　数　:　entity ..... SyukkaContainerEntity
    //　戻り値　:　[void]
    //================================================================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(SyukkaContainerEntity entity);
    // ・主キー重複時は既存データを置換
    // ・存在しない場合は新規追加


    //================================================================
    //　機　能　:　未送信データを取得する
    //　引　数　:　なし
    //　戻り値　:　[List<SyukkaContainerEntity>] ..... 未送信データ一覧
    //================================================================
    @Query(
            "SELECT * FROM " +
                    "T_SYUKKA_CONTAINER " +
                    "WHERE " +
                    "DATA_SEND_YMDHMS IS NULL " +
                    "ORDER BY CONTAINER_ID"
    )
    List<SyukkaContainerEntity> findUnsent();
    // ・送信日時未設定データを抽出
    // ・コンテナID昇順で取得
    // ・送信対象抽出用


    //================================================================
    //　機　能　:　最大コンテナIDを取得する
    //　引　数　:　なし
    //　戻り値　:　[Integer] ..... 最大コンテナID（データなしの場合はnull）
    //================================================================
    @Query(
            "SELECT " +
                    "MAX(CONTAINER_ID) " +
                    "FROM T_SYUKKA_CONTAINER"
    )
    Integer getMaxContainerId();
    // ・現在登録されている最大IDを取得
    // ・新規採番処理で使用想定


    //================================================================
    //　機　能　:　コンテナを送信済みに更新する
    //　引　数　:　containerId   ..... コンテナID
    //　　　　　:　dataSendYmdhms ..... 送信日時
    //　戻り値　:　[int] ..... 更新件数
    //================================================================
    @Query(
            "UPDATE " +
                    "T_SYUKKA_CONTAINER " +
                    "SET " +
                    "DATA_SEND_YMDHMS = :dataSendYmdhms " +
                    "WHERE " +
                    "CONTAINER_ID = :containerId"
    )
    int markSent(int containerId, String dataSendYmdhms);
    // ・送信日時を更新
    // ・更新件数を返却（0の場合は該当なし）


    //================================================================
    //　機　能　:　送信済データを削除する
    //　引　数　:　なし
    //　戻り値　:　[void]
    //================================================================
    @Query(
            "DELETE FROM " +
                    "T_SYUKKA_CONTAINER " +
                    "WHERE " +
                    "DATA_SEND_YMDHMS IS NOT NULL"
    )
    void deleteSent();
    // ・送信済データのみ削除
    // ・送信後の履歴整理用途


    //================================================================
    //　機　能　:　出荷コンテナデータを全件削除する
    //　引　数　:　なし
    //　戻り値　:　[void]
    //================================================================
    @Query(
            "DELETE FROM " +
                    "T_SYUKKA_CONTAINER"
    )
    void deleteAll();
    // ・テーブル内全データ削除
    // ・初期化処理などで使用

}