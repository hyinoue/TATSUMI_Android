package com.example.myapplication.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myapplication.db.entity.YoteiEntity;

import java.util.List;


//============================================================
//　処理概要　:　作業予定テーブル（T_YOTEI）に対するDAO
//　関　　数　:　findAll               ..... 全件取得
//　　　　　　:　findFirst             ..... 先頭1件取得
//　　　　　　:　findWithNullLastUpd   ..... 最終更新日時がNULLのデータ取得
//　　　　　　:　findByBookingNo       ..... 予約No.検索
//　　　　　　:　incrementKanryo       ..... 完了数（コンテナ／束／重量）加算
//　　　　　　:　upsert                ..... 追加／更新
//　　　　　　:　deleteAll             ..... 全件削除
//============================================================
@Dao
public interface YoteiDao {

    //================================================================
    //　機　能　:　予定データを全件取得する
    //　引　数　:　なし
    //　戻り値　:　[List<YoteiEntity>] ..... 全件データ
    //================================================================
    @Query(
            "SELECT * FROM " +
                    "T_YOTEI"
    )
    List<YoteiEntity> findAll();
    // ・T_YOTEIテーブルの全レコードを取得
    // ・並び順の指定なし


    //================================================================
    //　機　能　:　予定データの先頭1件を取得する
    //　引　数　:　なし
    //　戻り値　:　[YoteiEntity] ..... 先頭データ（存在しない場合はnull）
    //================================================================
    @Query(
            "SELECT * FROM " +
                    "T_YOTEI " +
                    "LIMIT 1"
    )
    YoteiEntity findFirst();
    // ・先頭1件のみ取得
    // ・特定用途（初期表示など）で利用想定
    // ・ORDER BYが無いため取得順はDB実装に依存


    //================================================================
    //　機　能　:　最終更新日時（LAST_UPD_YMDHMS）がNULLのデータを取得する
    //　引　数　:　なし
    //　戻り値　:　[List<YoteiEntity>] ..... 対象データ一覧
    //================================================================
    @Query(
            "SELECT * FROM " +
                    "T_YOTEI " +
                    "WHERE " +
                    "LAST_UPD_YMDHMS IS NULL"
    )
    List<YoteiEntity> findWithNullLastUpd();
    // ・未更新／未同期などの判定に使用想定


    //================================================================
    //　機　能　:　予約No.を指定して予定データを取得する
    //　引　数　:　bookingNo ..... 予約No.
    //　戻り値　:　[YoteiEntity] ..... 該当データ（存在しない場合はnull）
    //================================================================
    @Query(
            "SELECT * FROM " +
                    "T_YOTEI " +
                    "WHERE " +
                    "TRIM(BOOKING_NO) = TRIM(:bookingNo)"
    )
    YoteiEntity findByBookingNo(String bookingNo);
    // ・BOOKING_NO一致で検索
    // ・前後空白を除去して比較


    //================================================================
    //　機　能　:　完了数（コンテナ／束／重量）を加算更新する
    //　引　数　:　bookingNo    ..... 予約No.
    //　　　　　:　bundleCount ..... 加算する束数
    //　　　　　:　jyuryo       ..... 加算する重量
    //　戻り値　:　[int] ..... 更新件数
    //================================================================
    @Query(
            "UPDATE " +
                    "T_YOTEI " +
                    "SET " +
                    "KANRYO_CONTAINER = KANRYO_CONTAINER + 1, " +
                    "KANRYO_BUNDLE = KANRYO_BUNDLE + :bundleCount, " +
                    "KANRYO_JYURYO = KANRYO_JYURYO + :jyuryo " +
                    "WHERE " +
                    "TRIM(BOOKING_NO) = TRIM(:bookingNo)"
    )
    int incrementKanryo(String bookingNo, int bundleCount, int jyuryo);
    // ・完了コンテナ数は常に +1
    // ・完了束数／重量は引数分を加算
    // ・予約No.一致のレコードを更新
    // ・更新件数を返却（0の場合は該当なし）


    //================================================================
    //　機　能　:　予定データを追加または更新する（Upsert処理）
    //　引　数　:　entity ..... YoteiEntity
    //　戻り値　:　[void]
    //================================================================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(YoteiEntity entity);
    // ・主キー重複時は既存データを置換
    // ・存在しない場合は新規追加


    //================================================================
    //　機　能　:　予定データを全件削除する
    //　引　数　:　なし
    //　戻り値　:　[void]
    //================================================================
    @Query(
            "DELETE FROM " +
                    "T_YOTEI"
    )
    void deleteAll();
    // ・テーブル内全レコード削除
    // ・初期化処理などで使用

}