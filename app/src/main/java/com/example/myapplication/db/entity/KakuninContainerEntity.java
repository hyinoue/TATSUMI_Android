package com.example.myapplication.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;


//============================================================
//　処理概要　:　確認コンテナテーブル（T_KAKUNIN_CONTAINER）エンティティ
//　対　　象　:　照合処理用コンテナ情報を保持する
//　主キー　　:　CONTAINER_ID
//============================================================
@Entity(
        tableName = "T_KAKUNIN_CONTAINER",
        primaryKeys = {"CONTAINER_ID"}
)
public class KakuninContainerEntity {

    //============================================================
    //　項目概要　:　コンテナID（主キー）
    //　内　　容　:　確認コンテナを一意に識別するID
    //============================================================
    @NonNull
    @ColumnInfo(name = "CONTAINER_ID")
    public String containerId;
    // ・照合対象コンテナの識別子


    //============================================================
    //　項目概要　:　コンテナ番号
    //　内　　容　:　表示用コンテナ番号
    //============================================================
    @ColumnInfo(name = "CONTAINER_NO")
    public String containerNo;
    // ・ラベル表示や帳票出力用番号


    //============================================================
    //　項目概要　:　束数
    //　内　　容　:　コンテナ内の束数
    //============================================================
    @ColumnInfo(name = "BUNDLE_CNT")
    public Integer bundleCnt;
    // ・格納されている束の数量


    //============================================================
    //　項目概要　:　作業日
    //　内　　容　:　作業実施日（DATE → TEXT）
    //============================================================
    @ColumnInfo(name = "SAGYOU_YMD")
    public String sagyouYmd;
    // ・yyyyMMdd形式などで保存想定


    //============================================================
    //　項目概要　:　照合完了フラグ
    //　内　　容　:　照合完了 여부（BIT → INTEGER）
    //============================================================
    @ColumnInfo(name = "CONTAINER_SYOUGO_KANRYO")
    public Boolean containerSyougoKanryo;
    // ・true  : 照合完了
    // ・false : 未照合


    //============================================================
    //　項目概要　:　データ送信日時
    //　内　　容　:　送信完了日時（DATE → TEXT）
    //============================================================
    @ColumnInfo(name = "DATA_SEND_YMDHMS")
    public String dataSendYmdhms;
    // ・未送信の場合はNULL


    //============================================================
    //　項目概要　:　登録処理名
    //　内　　容　:　登録時の処理名称
    //============================================================
    @ColumnInfo(name = "INSERT_PROC_NAME")
    public String insertProcName;
    // ・登録元機能名などを保持


    //============================================================
    //　項目概要　:　登録日
    //　内　　容　:　登録日（DATE → TEXT）
    //============================================================
    @ColumnInfo(name = "INSERT_YMD")
    public String insertYmd;
    // ・yyyyMMdd形式などで保存想定


    //============================================================
    //　項目概要　:　更新処理名
    //　内　　容　:　最終更新時の処理名称
    //============================================================
    @ColumnInfo(name = "UPDATE_PROC_NAME")
    public String updateProcName;
    // ・更新元機能名などを保持


    //============================================================
    //　項目概要　:　更新日
    //　内　　容　:　最終更新日（DATE → TEXT）
    //============================================================
    @ColumnInfo(name = "UPDATE_YMD")
    public String updateYmd;
    // ・yyyyMMdd形式などで保存想定

}