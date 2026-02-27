package com.example.myapplication.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myapplication.db.entity.CommHistoryEntity;

import java.util.List;


//========================================================================
//　処理概要　:　通信履歴テーブル（C_COMM_HISTORY）に対するDAO
//　関　　数　:　findAllDesc                 ..... 全件取得（降順）
//　　　　　　:　findMaxLogIdByDatePrefix   ..... 日付接頭辞による最大LOG_ID取得
//　　　　　　:　upsert                      ..... 追加／更新
//　　　　　　:　deleteBefore               ..... 指定日時以前削除
//　　　　　　:　deleteAll                  ..... 全件削除
//========================================================================
@Dao
public interface CommHistoryDao {

    //============================================================
    //　機　能　:　C_COMM_HISTORYテーブルの全件を降順で取得する
    //　引　数　:　なし
    //　戻り値　:　[List<CommHistoryEntity>] ..... 通信履歴一覧
    //============================================================
    @Query(
            "SELECT * FROM " +
                    "C_COMM_HISTORY " +
                    "ORDER BY " +
                    "START_YMDHMS DESC, END_YMDHMS DESC"
    )
    List<CommHistoryEntity> findAllDesc();
    // ・開始日時の降順で並び替え
    // ・開始日時が同一の場合は終了日時の降順で並び替え
    // ・最新の通信履歴が先頭に来る


    //============================================================
    //　機　能　:　指定日付接頭辞に一致するLOG_IDの最大値を取得する
    //　引　数　:　ymdPrefix ..... 日付接頭辞（例：yyyyMMdd）
    //　戻り値　:　[String] ..... 最大LOG_ID（該当なしの場合はnull）
    //============================================================
    @Query(
            "SELECT MAX(LOG_ID) FROM " +
                    "C_COMM_HISTORY " +
                    "WHERE " +
                    "LOG_ID LIKE :ymdPrefix || '%'"
    )
    String findMaxLogIdByDatePrefix(String ymdPrefix);
    // ・LOG_IDが指定日付（接頭辞）で始まるデータを検索
    // ・その中で最大のLOG_IDを取得
    // ・新規採番処理などで使用する想定


    //============================================================
    //　機　能　:　通信履歴を追加または更新する（Upsert処理）
    //　引　数　:　entity ..... エンティティ情報
    //　戻り値　:　[void]
    //============================================================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(CommHistoryEntity entity);
    // ・主キーが重複した場合は既存データを置換（REPLACE）
    // ・存在しない場合は新規追加
    // ・1件単位での登録／更新処理


    //============================================================
    //　機　能　:　指定日時より前の通信履歴を削除する
    //　引　数　:　threshold ..... 削除基準日時（START_YMDHMS）
    //　戻り値　:　[void]
    //============================================================
    @Query(
            "DELETE FROM " +
                    "C_COMM_HISTORY " +
                    "WHERE " +
                    "START_YMDHMS < :threshold"
    )
    void deleteBefore(String threshold);
    // ・開始日時がthreshold未満のデータを削除
    // ・履歴の世代管理／容量制御目的で使用


    //============================================================
    //　機　能　:　通信履歴を全件削除する
    //　引　数　:　なし
    //　戻り値　:　[void]
    //============================================================
    @Query(
            "DELETE FROM " +
                    "C_COMM_HISTORY"
    )
    void deleteAll();
    // ・テーブル内の全レコードを削除
    // ・初期化処理などで使用

}
