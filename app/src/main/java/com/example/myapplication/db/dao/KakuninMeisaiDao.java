package com.example.myapplication.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myapplication.db.entity.KakuninMeisaiEntity;

import java.util.List;


//============================================================
//　処理概要　:　確認明細テーブル（T_KAKUNIN_MEISAI）に対するDAO
//　関　　数　:　findOne               ..... 明細単一取得
//　　　　　　:　findByContainerId     ..... コンテナID検索
//　　　　　　:　upsert                ..... 追加／更新
//　　　　　　:　deleteAll             ..... 全件削除
//============================================================
@Dao
public interface KakuninMeisaiDao {

    //================================================================
    //　機　能　:　HEAT_NOとSOKUBANを指定して明細を1件取得する
    //　引　数　:　heatNo  ..... 鋼番
    //　　　　　:　sokuban ..... 束番
    //　戻り値　:　[KakuninMeisaiEntity] ..... 該当データ（存在しない場合はnull）
    //================================================================
    @Query(
            "SELECT * FROM " +
                    "T_KAKUNIN_MEISAI " +
                    "WHERE " +
                    "TRIM(HEAT_NO) = TRIM(:heatNo) " +
                    "AND " +
                    "TRIM(SOKUBAN) = TRIM(:sokuban)"
    )
    KakuninMeisaiEntity findOne(String heatNo, String sokuban);
    // ・HEAT_NOとSOKUBANの複合条件で検索
    // ・前後の空白を除去して比較
    // ・該当なしの場合はnullを返却


    //================================================================
    //　機　能　:　コンテナIDを指定して明細一覧を取得する
    //　引　数　:　containerId ..... コンテナID
    //　戻り値　:　[List<KakuninMeisaiEntity>] ..... 明細一覧
    //================================================================
    @Query(
            "SELECT * FROM " +
                    "T_KAKUNIN_MEISAI " +
                    "WHERE " +
                    "TRIM(CONTAINER_ID) = TRIM(:containerId)"
    )
    List<KakuninMeisaiEntity> findByContainerId(String containerId);
    // ・指定コンテナに紐づく明細を全件取得
    // ・空白除去して一致判定
    // ・複数件存在する前提


    //================================================================
    //　機　能　:　確認明細データを追加または更新する（Upsert処理）
    //　引　数　:　entity ..... KakuninMeisaiEntity
    //　戻り値　:　[void]
    //================================================================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(KakuninMeisaiEntity entity);
    // ・主キー重複時は既存データを置換
    // ・存在しない場合は新規追加
    // ・1件単位での登録／更新処理


    //================================================================
    //　機　能　:　確認明細データを全件削除する
    //　引　数　:　なし
    //　戻り値　:　[void]
    //================================================================
    @Query(
            "DELETE FROM " +
                    "T_KAKUNIN_MEISAI"
    )
    void deleteAll();
    // ・テーブル内の全レコードを削除
    // ・初期化処理などで使用

}