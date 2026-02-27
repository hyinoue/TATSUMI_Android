package com.example.myapplication.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myapplication.db.entity.KakuninContainerEntity;

import java.util.List;


//============================================================
//　処理概要　:　確認コンテナテーブル（T_KAKUNIN_CONTAINER）に対するDAO
//　関　　数　:　findByContainerId      ..... コンテナID検索
//　　　　　　:　upsert                 ..... 追加／更新
//　　　　　　:　findUnsentCompleted    ..... 未送信かつ照合完了データ取得
//　　　　　　:　markSent               ..... 送信済更新
//　　　　　　:　deleteAll              ..... 全件削除
//　　　　　　:　findUncollated         ..... 未照合データ取得
//============================================================
@Dao
public interface KakuninContainerDao {

    //============================================================
    //　機　能　:　コンテナIDを指定してデータを取得する
    //　引　数　:　containerId ..... コンテナID
    //　戻り値　:　[KakuninContainerEntity] ..... 該当データ（存在しない場合はnull）
    //============================================================
    @Query(
            "SELECT * FROM " +
                    "T_KAKUNIN_CONTAINER " +
                    "WHERE " +
                    "TRIM(CONTAINER_ID) = TRIM(:containerId)"
    )
    KakuninContainerEntity findByContainerId(String containerId);
    // ・前後の空白を除去して比較
    // ・完全一致検索
    // ・該当がなければnullを返す


    //============================================================
    //　機　能　:　確認コンテナデータを追加または更新する（Upsert処理）
    //　引　数　:　entity ..... エンティティ情報
    //　戻り値　:　[void]
    //============================================================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(KakuninContainerEntity entity);
    // ・主キー重複時は既存データを置換
    // ・存在しない場合は新規追加
    // ・1件単位での登録／更新処理


    //============================================================
    //　機　能　:　照合完了かつ未送信データを取得する
    //　引　数　:　なし
    //　戻り値　:　[List<KakuninContainerEntity>] ..... 対象データ一覧
    //============================================================
    @Query(
            "SELECT * FROM " +
                    "T_KAKUNIN_CONTAINER " +
                    "WHERE " +
                    "CONTAINER_SYOUGO_KANRYO = 1 " +
                    "AND " +
                    "DATA_SEND_YMDHMS IS NULL " +
                    "ORDER BY " +
                    "CONTAINER_ID"
    )
    List<KakuninContainerEntity> findUnsentCompleted();
    // ・照合完了フラグが1（完了）
    // ・送信日時が未設定（未送信）
    // ・コンテナID昇順で取得
    // ・送信対象データ抽出用


    //============================================================
    //　機　能　:　コンテナを送信済みに更新する
    //　引　数　:　containerId   ..... コンテナID
    //　　　　　:　dataSendYmdhms ..... 送信日時
    //　戻り値　:　[int] ..... 更新件数
    //============================================================
    @Query(
            "UPDATE " +
                    "T_KAKUNIN_CONTAINER " +
                    "SET " +
                    "DATA_SEND_YMDHMS = :dataSendYmdhms " +
                    "WHERE " +
                    "TRIM(CONTAINER_ID) = TRIM(:containerId)"
    )
    int markSent(String containerId, String dataSendYmdhms);
    // ・指定コンテナの送信日時を更新
    // ・空白除去して一致判定
    // ・更新件数を返却（0の場合は該当なし）


    //============================================================
    //　機　能　:　確認コンテナデータを全件削除する
    //　引　数　:　なし
    //　戻り値　:　[void]
    //============================================================
    @Query(
            "DELETE FROM " +
                    "T_KAKUNIN_CONTAINER"
    )
    void deleteAll();
    // ・テーブル内の全データを削除
    // ・初期化処理等で使用


    //============================================================
    //　機　能　:　未照合データを取得する
    //　引　数　:　なし
    //　戻り値　:　[List<KakuninContainerEntity>] ..... 未照合データ一覧
    //============================================================
    @Query(
            "SELECT * FROM " +
                    "T_KAKUNIN_CONTAINER " +
                    "WHERE " +
                    "CONTAINER_SYOUGO_KANRYO = 0 " +
                    "ORDER BY " +
                    "CONTAINER_ID"
    )
    List<KakuninContainerEntity> findUncollated();
    // ・照合完了フラグが0（未照合）
    // ・コンテナID昇順で取得
    // ・未処理データ確認用

}
