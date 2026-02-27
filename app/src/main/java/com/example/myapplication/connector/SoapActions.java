package com.example.myapplication.connector;


//==========================================================================
//　処理概要　:　SOAPActionヘッダーに使うアクション名定数を定義するクラス
//　関　　数　:　（定数クラスのためメソッドなし）
//　　　　　　:　GET_SYS_DATE ............................. システム日付取得
//　　　　　　:　GET_SAGYOU_YMD ............................ 作業日取得
//　　　　　　:　GET_UPDATE_YMD_HMS ........................ 更新日時取得
//　　　　　　:　GET_SYUKKA_DATA ........................... 出荷データ取得
//　　　　　　:　SEND_SYUKKA_DATA .......................... 出荷データ送信
//　　　　　　:　UPLOAD_BINARY_FILE ........................ バイナリファイル送信
//　　　　　　:　GET_DOWNLOAD_HANDY_EXECUTE_FILE_NAMES ..... 実行ファイル名一覧取得
//　　　　　　:　GET_DOWNLOAD_HANDY_EXECUTE_FILE ........... 実行ファイル取得
//　　　　　　:　GET_SYOUGO_DATA ........................... 照合データ取得
//　　　　　　:　SEND_SYOUGO_DATA .......................... 照合データ送信
//==========================================================================
public final class SoapActions {

    //============================================================
    //　機　能　:　SoapActionsの生成を禁止する（定数専用クラス）
    //　引　数　:　なし
    //　戻り値　:　[SoapActions] ..... なし
    //============================================================
    private SoapActions() {
        // 定数のみを保持するクラスのためインスタンス化させない
    }

    //============================================================
    //　機　能　:　システム日付取得用SOAP Action
    //　引　数　:　なし
    //　戻り値　:　[String] ..... SOAP Action文字列
    //============================================================
    public static final String GET_SYS_DATE = "http://tempuri.org/GetSysDate";

    //============================================================
    //　機　能　:　作業日取得用SOAP Action
    //　引　数　:　なし
    //　戻り値　:　[String] ..... SOAP Action文字列
    //============================================================
    public static final String GET_SAGYOU_YMD = "http://tempuri.org/GetSagyouYmd";

    //============================================================
    //　機　能　:　更新日時取得用SOAP Action
    //　引　数　:　なし
    //　戻り値　:　[String] ..... SOAP Action文字列
    //============================================================
    public static final String GET_UPDATE_YMD_HMS = "http://tempuri.org/GetUpdateYmdHms";

    //============================================================
    //　機　能　:　出荷データ取得用SOAP Action
    //　引　数　:　なし
    //　戻り値　:　[String] ..... SOAP Action文字列
    //============================================================
    public static final String GET_SYUKKA_DATA = "http://tempuri.org/GetSyukkaData";

    //============================================================
    //　機　能　:　出荷データ送信用SOAP Action
    //　引　数　:　なし
    //　戻り値　:　[String] ..... SOAP Action文字列
    //============================================================
    public static final String SEND_SYUKKA_DATA = "http://tempuri.org/SendSyukkaData";

    //============================================================
    //　機　能　:　バイナリファイル送信用SOAP Action
    //　引　数　:　なし
    //　戻り値　:　[String] ..... SOAP Action文字列
    //============================================================
    public static final String UPLOAD_BINARY_FILE = "http://tempuri.org/UploadBinaryFile";

    //============================================================
    //　機　能　:　実行ファイル名一覧取得用SOAP Action
    //　引　数　:　なし
    //　戻り値　:　[String] ..... SOAP Action文字列
    //============================================================
    public static final String GET_DOWNLOAD_HANDY_EXECUTE_FILE_NAMES =
            "http://tempuri.org/GetDownloadHandyExecuteFileNames";

    //============================================================
    //　機　能　:　実行ファイル取得用SOAP Action
    //　引　数　:　なし
    //　戻り値　:　[String] ..... SOAP Action文字列
    //============================================================
    public static final String GET_DOWNLOAD_HANDY_EXECUTE_FILE =
            "http://tempuri.org/GetDownloadHandyExecuteFile";

    //============================================================
    //　機　能　:　照合データ取得用SOAP Action
    //　引　数　:　なし
    //　戻り値　:　[String] ..... SOAP Action文字列
    //============================================================
    public static final String GET_SYOUGO_DATA = "http://tempuri.org/GetSyougoData";

    //============================================================
    //　機　能　:　照合データ送信用SOAP Action
    //　引　数　:　なし
    //　戻り値　:　[String] ..... SOAP Action文字列
    //============================================================
    public static final String SEND_SYOUGO_DATA = "http://tempuri.org/SendSyougoData";
}
