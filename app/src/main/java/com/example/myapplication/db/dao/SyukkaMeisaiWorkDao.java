package com.example.myapplication.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myapplication.db.entity.SyukkaMeisaiWorkEntity;

import java.util.List;


//============================================================
//　処理概要　:　出荷明細ワークテーブル（W_SYUKKA_MEISAI）に対するDAO
//　関　　数　:　findAll           ..... 全件取得
//　　　　　　:　findOne           ..... 明細単一取得
//　　　　　　:　upsert            ..... 追加／更新
//　　　　　　:　deleteOne         ..... 単一削除
//　　　　　　:　deleteAll         ..... 全件削除
//　　　　　　:　getWorkSummary    ..... ワーク集計取得
//　　　　　　:　selectWorkJoined  ..... ワーク＋本体JOIN取得（一覧）
//============================================================
@Dao
public interface SyukkaMeisaiWorkDao {

    //============================================================
    //　機　能　:　ワーク明細データを全件取得する
    //　引　数　:　なし
    //　戻り値　:　[List<SyukkaMeisaiWorkEntity>] ..... 全件データ
    //============================================================
    @Query(
            "SELECT * FROM " +
                    "W_SYUKKA_MEISAI"
    )
    List<SyukkaMeisaiWorkEntity> findAll();
    // ・W_SYUKKA_MEISAIテーブルの全レコードを取得
    // ・並び順の指定なし


    //============================================================
    //　機　能　:　HEAT_NOとSOKUBANを指定してワーク明細を1件取得する
    //　引　数　:　heatNo  ..... 鋼番
    //　　　　　:　sokuban ..... 束番
    //　戻り値　:　[SyukkaMeisaiWorkEntity] ..... 該当データ（存在しない場合はnull）
    //============================================================
    @Query(
            "SELECT * FROM " +
                    "W_SYUKKA_MEISAI " +
                    "WHERE " +
                    "TRIM(HEAT_NO) = TRIM(:heatNo) " +
                    "AND " +
                    "TRIM(SOKUBAN) = TRIM(:sokuban) " +
                    "LIMIT 1"
    )
    SyukkaMeisaiWorkEntity findOne(String heatNo, String sokuban);
    // ・複合条件（HEAT_NO/SOKUBAN）で検索
    // ・前後空白を除去して比較
    // ・LIMIT 1 により先頭1件のみ取得


    //============================================================
    //　機　能　:　ワーク明細データを追加または更新する（Upsert処理）
    //　引　数　:　entity ..... エンティティ情報
    //　戻り値　:　[void]
    //============================================================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(SyukkaMeisaiWorkEntity entity);
    // ・主キー重複時は既存データを置換
    // ・存在しない場合は新規追加
    // ・1件単位で登録／更新


    //============================================================
    //　機　能　:　指定したHEAT_NOとSOKUBANのデータを削除する
    //　引　数　:　heatNo  ..... 鋼番
    //　　　　　:　sokuban ..... 束番
    //　戻り値　:　[int] ..... 削除件数
    //============================================================
    @Query(
            "DELETE FROM " +
                    "W_SYUKKA_MEISAI " +
                    "WHERE " +
                    "TRIM(HEAT_NO) = TRIM(:heatNo) " +
                    "AND " +
                    "TRIM(SOKUBAN) = TRIM(:sokuban)"
    )
    int deleteOne(String heatNo, String sokuban);
    // ・複合キー一致データを削除
    // ・削除件数を返却（0の場合は該当なし）


    //============================================================
    //　機　能　:　ワーク明細データを全件削除する
    //　引　数　:　なし
    //　戻り値　:　[void]
    //============================================================
    @Query("DELETE FROM W_SYUKKA_MEISAI")
    void deleteAll();
    // ・テーブル内の全レコードを削除
    // ・初期化／作業開始前のクリア処理などで使用


    //============================================================
    //　機　能　:　ワーク明細の集計情報（束数・重量・予約No.）を取得する
    //　引　数　:　なし
    //　戻り値　:　[WorkSummary] ..... 集計結果
    //============================================================
    @Query(
            "SELECT " +
                    " count(W.HEAT_NO) AS sokusu, " +
                    " sum(T.JYURYO) AS jyuryo, " +
                    " max(T.BOOKING_NO) AS bookingNo " +
                    "FROM " +
                    "W_SYUKKA_MEISAI W " +
                    "INNER JOIN " +
                    "T_SYUKKA_MEISAI T " +
                    " ON " +
                    "TRIM(W.HEAT_NO) = TRIM(T.HEAT_NO) " +
                    "AND " +
                    "TRIM(W.SOKUBAN) = TRIM(T.SOKUBAN)"
    )
    WorkSummary getWorkSummary();
    // ・ワーク（W）に登録されている明細を対象に集計
    // ・本体（T）とJOINして重量／予約No.を取得
    // ・sokusu    : ワーク件数（束数の扱い想定）
    // ・jyuryo    : 重量合計（T.JYURYOの合計）
    // ・bookingNo : 予約No.（複数ある場合はmaxで代表値取得）


    //============================================================
    //　機　能　:　ワークと本体をJOINした一覧を取得する
    //　引　数　:　なし
    //　戻り値　:　[List<WorkJoinRow>] ..... JOIN結果一覧
    //============================================================
    @Query(
            "SELECT " +
                    " W.HEAT_NO AS heatNo, " +
                    " W.SOKUBAN AS sokuban, " +
                    " T.SYUKKA_SASHIZU_NO AS packingNo, " +
                    " T.BUNDLE_NO AS bundleNo, " +
                    " T.JYURYO AS jyuryo, " +
                    " T.BOOKING_NO AS bookingNo " +
                    "FROM " +
                    "W_SYUKKA_MEISAI W " +
                    "INNER JOIN " +
                    "T_SYUKKA_MEISAI T " +
                    " ON " +
                    "TRIM(W.HEAT_NO) = TRIM(T.HEAT_NO) " +
                    "AND " +
                    "TRIM(W.SOKUBAN) = TRIM(T.SOKUBAN) " +
                    "ORDER BY " +
                    "W.INSERT_YMD"
    )
    List<WorkJoinRow> selectWorkJoined();
    // ・ワーク（W）に存在する明細のみを対象に本体（T）から付随情報を取得
    // ・ASでWorkJoinRowのフィールド名に合わせてマッピング
    // ・INSERT_YMD昇順で取得（作業投入順の表示想定）


    //============================================================
    //　処理概要　:　ワークJOIN結果（一覧表示用）
    //============================================================
    class WorkJoinRow {
        public String heatNo;     // 鋼番
        public String sokuban;    // 束番
        public String packingNo;  // 出荷指図No.（SQL上はSYUKKA_SASHIZU_NO）
        public String bundleNo;   // バンドルNo.
        public Integer jyuryo;    // 重量
        public String bookingNo;  // 予約No.
    }

    //============================================================
    //　処理概要　:　ワーク集計結果（束数・重量・予約No.）
    //============================================================
    class WorkSummary {
        public int sokusu;        // 束数（ワーク件数）
        public Integer jyuryo;    // 重量合計（NULLの可能性あり）
        public String bookingNo;  // 予約No.（代表値）
    }

}
