package com.example.myapplication.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myapplication.db.entity.KakuninMeisaiWorkEntity;

import java.util.List;


//============================================================
//　処理概要　:　確認明細ワークテーブル（W_KAKUNIN_MEISAI）に対するDAO
//　関　　数　:　findAll           ..... 全件取得
//　　　　　　:　findAllOrdered    ..... 並び順指定全件取得
//　　　　　　:　findOne           ..... 明細単一取得
//　　　　　　:　upsert            ..... 追加／更新
//　　　　　　:　deleteOne         ..... 単一削除
//　　　　　　:　deleteAll         ..... 全件削除
//============================================================
@Dao
public interface KakuninMeisaiWorkDao {

    //============================================================
    //　機　能　:　ワーク明細データを全件取得する
    //　引　数　:　なし
    //　戻り値　:　[List<KakuninMeisaiWorkEntity>] ..... 全件データ
    //============================================================
    @Query(
            "SELECT * FROM " +
                    "W_KAKUNIN_MEISAI"
    )
    List<KakuninMeisaiWorkEntity> findAll();
    // ・W_KAKUNIN_MEISAIテーブルの全レコードを取得
    // ・並び順の指定なし


    //==============================================================
    //　機　能　:　指定順でワーク明細データを全件取得する
    //　引　数　:　なし
    //　戻り値　:　[List<KakuninMeisaiWorkEntity>] ..... 並び替え済データ
    //==============================================================
    @Query(
            "SELECT * FROM " +
                    "W_KAKUNIN_MEISAI " +
                    "ORDER BY " +
                    "SYUKKA_SASHIZU_NO, " +
                    "BUNDLE_NO, " +
                    "SOKUBAN"
    )
    List<KakuninMeisaiWorkEntity> findAllOrdered();
    // ・出荷指図No. → バンドルNo. → 束番 の順で昇順ソート
    // ・画面表示や帳票出力用の並び順想定


    //======================================================================
    //　機　能　:　HEAT_NOとSOKUBANを指定してワーク明細を1件取得する
    //　引　数　:　heatNo  ..... 鋼番
    //　　　　　:　sokuban ..... 束番
    //　戻り値　:　[KakuninMeisaiWorkEntity] ..... 該当データ（存在しない場合はnull）
    //======================================================================
    @Query(
            "SELECT * FROM " +
                    "W_KAKUNIN_MEISAI " +
                    "WHERE " +
                    "TRIM(HEAT_NO) = TRIM(:heatNo) " +
                    "AND " +
                    "TRIM(SOKUBAN) = TRIM(:sokuban)"
    )
    KakuninMeisaiWorkEntity findOne(String heatNo, String sokuban);
    // ・HEAT_NOとSOKUBANの複合条件検索
    // ・前後空白を除去して一致判定
    // ・該当なしの場合はnullを返却


    //============================================================
    //　機　能　:　ワーク明細データを追加または更新する（Upsert処理）
    //　引　数　:　entity ..... エンティティ情報
    //　戻り値　:　[void]
    //============================================================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(KakuninMeisaiWorkEntity entity);
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
                    "W_KAKUNIN_MEISAI " +
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
    @Query(
            "DELETE FROM " +
                    "W_KAKUNIN_MEISAI"
    )
    void deleteAll();
    // ・テーブル内の全レコードを削除
    // ・初期化や再取込前のクリア処理などで使用

}
