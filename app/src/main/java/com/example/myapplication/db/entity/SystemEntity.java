package com.example.myapplication.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;


//============================================================
//　処理概要　:　システムマスタ（M_SYSTEM）エンティティ
//　対　　象　:　アプリケーション共通設定情報を保持する
//　主キー　　:　RENBAN
//============================================================
@Entity(
        tableName = "M_SYSTEM",
        primaryKeys = {"RENBAN"}
)
public class SystemEntity {

    //============================================================
    //　項目概要　:　連番（主キー）
    //　内　　容　:　システム設定を識別するキー
    //============================================================
    @ColumnInfo(name = "RENBAN")
    @NonNull
    public Integer renban;
    // ・通常は1固定で運用想定


    //============================================================
    //　項目概要　:　WebサービスURL
    //　内　　容　:　接続先Webサービスのエンドポイント
    //============================================================
    @ColumnInfo(name = "WEB_SVC_URL")
    public String webSvcUrl;
    // ・API接続先URL


    //============================================================
    //　項目概要　:　標準コンテナ自重
    //　内　　容　:　コンテナの初期重量
    //============================================================
    @ColumnInfo(name = "DEFAULT_CONTAINER_JYURYO")
    public Integer defaultContainerJyuryo;
    // ・計算時の基準重量


    //============================================================
    //　項目概要　:　標準ダンネージ重量
    //　内　　容　:　梱包材の初期重量
    //============================================================
    @ColumnInfo(name = "DEFAULT_DUNNAGE_JYURYO")
    public Integer defaultDunnageJyuryo;
    // ・梱包材重量の基準値


    //============================================================
    //　項目概要　:　最大コンテナ重量（DB非保持）
    //　内　　容　:　計算用最大重量値
    //============================================================
    @Ignore
    public Integer maxContainerJyuryo;
    // ・Roomには保存されない
    // ・アプリ内部計算用


    //============================================================
    //　項目概要　:　データ確認日時
    //　内　　容　:　最終データ確認日時（ISO形式TEXT）
    //============================================================
    @ColumnInfo(name = "DATA_CONF_YMDHMS")
    public String dataConfYmdhms;
    // ・yyyy-MM-dd'T'HH:mm:ss


    //============================================================
    //　項目概要　:　データ受信日時
    //　内　　容　:　最終データ同期日時（ISO形式TEXT）
    //============================================================
    @ColumnInfo(name = "DATA_RECV_YMDHMS")
    public String dataRecvYmdhms;
    // ・yyyy-MM-dd'T'HH:mm:ss
    // ・同期完了時に更新


    //============================================================
    //　項目概要　:　更新処理名
    //　内　　容　:　最終更新時の処理名称
    //============================================================
    @ColumnInfo(name = "UPDATE_PROC_NAME")
    public String updateProcName;
    // ・更新元機能名などを保持


    //============================================================
    //　項目概要　:　更新年月日
    //　内　　容　:　最終更新日（ISO形式TEXT）
    //============================================================
    @ColumnInfo(name = "UPDATE_YMD")
    public String updateYmd;
    // ・yyyy-MM-dd'T'HH:mm:ss

}