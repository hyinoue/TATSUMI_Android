package com.example.myapplication.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myapplication.db.entity.SystemEntity;


//============================================================
//　処理概要　:　システム管理テーブル（M_SYSTEM）に対するDAO
//　関　　数　:　insert           ..... 新規登録
//　　　　　　:　get              ..... 固定レコード取得（RENBAN=1）
//　　　　　　:　findById         ..... RENBAN指定取得
//　　　　　　:　upsert           ..... 追加／更新
//　　　　　　:　updateDataConf   ..... データ確認日時更新
//　　　　　　:　updateDataSync   ..... データ同期日時更新
//　　　　　　:　deleteAll        ..... 全件削除
//============================================================
@Dao
public interface SystemDao {

    //================================================================
    //　機　能　:　システム管理データを新規登録する
    //　引　数　:　entity ..... SystemEntity
    //　戻り値　:　[void]
    //================================================================
    @Insert
    void insert(SystemEntity entity);
    // ・新規追加のみ（競合時の置換指定なし）
    // ・通常は初期データ登録時に使用


    //================================================================
    //　機　能　:　RENBAN=1のシステムデータを取得する
    //　引　数　:　なし
    //　戻り値　:　[SystemEntity] ..... 該当データ
    //================================================================
    @Query(
            "SELECT * FROM " +
                    "M_SYSTEM " +
                    "WHERE " +
                    "RENBAN = 1"
    )
    SystemEntity get();
    // ・固定レコード取得用
    // ・単一レコード管理前提


    //================================================================
    //　機　能　:　RENBANを指定してシステムデータを取得する
    //　引　数　:　renban ..... 連番
    //　戻り値　:　[SystemEntity] ..... 該当データ（存在しない場合はnull）
    //================================================================
    @Query(
            "SELECT * FROM " +
                    "M_SYSTEM " +
                    "WHERE " +
                    "RENBAN = :renban"
    )
    SystemEntity findById(int renban);
    // ・主キー（RENBAN）検索
    // ・該当なしの場合はnullを返却


    //================================================================
    //　機　能　:　システム管理データを追加または更新する（Upsert処理）
    //　引　数　:　entity ..... SystemEntity
    //　戻り値　:　[void]
    //================================================================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(SystemEntity entity);
    // ・RENBAN重複時は既存データを置換
    // ・存在しない場合は新規追加


    //================================================================
    //　機　能　:　データ確認日時を更新する
    //　引　数　:　renban           ..... 連番
    //　　　　　:　dataConfYmdhms  ..... データ確認日時
    //　　　　　:　updateProcName  ..... 更新処理名
    //　　　　　:　updateYmd       ..... 更新日
    //　戻り値　:　[int] ..... 更新件数
    //================================================================
    @Query(
            "UPDATE " +
                    "M_SYSTEM " +
                    "SET " +
                    "DATA_CONF_YMDHMS = :dataConfYmdhms, " +
                    "UPDATE_PROC_NAME = :updateProcName, " +
                    "UPDATE_YMD = :updateYmd " +
                    "WHERE " +
                    "RENBAN = :renban"
    )
    int updateDataConf(int renban, String dataConfYmdhms, String updateProcName, String updateYmd);
    // ・データ確認日時を更新
    // ・更新処理名および更新日も同時更新
    // ・更新件数を返却（0の場合は該当なし）


    //================================================================
    //　機　能　:　データ同期日時を更新する
    //　引　数　:　renban           ..... 連番
    //　　　　　:　dataConfYmdhms  ..... データ確認日時
    //　　　　　:　dataRecvYmdhms  ..... データ受信日時
    //　　　　　:　updateProcName  ..... 更新処理名
    //　　　　　:　updateYmd       ..... 更新日
    //　戻り値　:　[int] ..... 更新件数
    //================================================================
    @Query(
            "UPDATE " +
                    "M_SYSTEM " +
                    "SET " +
                    "DATA_CONF_YMDHMS = :dataConfYmdhms, " +
                    "DATA_RECV_YMDHMS = :dataRecvYmdhms, " +
                    "UPDATE_PROC_NAME = :updateProcName, " +
                    "UPDATE_YMD = :updateYmd " +
                    "WHERE " +
                    "RENBAN = :renban"
    )
    int updateDataSync(int renban, String dataConfYmdhms, String dataRecvYmdhms, String updateProcName, String updateYmd);
    // ・確認日時と受信日時を同時更新
    // ・更新処理名および更新日も更新
    // ・データ同期完了時に使用


    //================================================================
    //　機　能　:　システム管理データを全件削除する
    //　引　数　:　なし
    //　戻り値　:　[void]
    //================================================================
    @Query(
            "DELETE FROM " +
                    "M_SYSTEM"
    )
    void deleteAll();
    // ・テーブル内の全レコードを削除
    // ・初期化処理などで使用

}