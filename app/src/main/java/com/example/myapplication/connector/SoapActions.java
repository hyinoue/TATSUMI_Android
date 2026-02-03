package com.example.myapplication.connector;


//======================
//　処理概要　:　SoapActionsクラス
//======================

public final class SoapActions {
    //=============================
    //　機　能　:　SoapActionsの初期化処理
    //　引　数　:　なし
    //　戻り値　:　[SoapActions] ..... なし
    //=============================

    private SoapActions() {
    }

    public static final String GET_SYS_DATE = "http://tempuri.org/GetSysDate";
    public static final String GET_SAGYOU_YMD = "http://tempuri.org/GetSagyouYmd";
    public static final String GET_UPDATE_YMD_HMS = "http://tempuri.org/GetUpdateYmdHms";
    public static final String GET_SYUKKA_DATA = "http://tempuri.org/GetSyukkaData";
    public static final String SEND_SYUKKA_DATA = "http://tempuri.org/SendSyukkaData";
    public static final String UPLOAD_BINARY_FILE = "http://tempuri.org/UploadBinaryFile";
    public static final String GET_DOWNLOAD_HANDY_EXECUTE_FILE_NAMES = "http://tempuri.org/GetDownloadHandyExecuteFileNames";
    public static final String GET_DOWNLOAD_HANDY_EXECUTE_FILE = "http://tempuri.org/GetDownloadHandyExecuteFile";
    public static final String GET_SYOUGO_DATA = "http://tempuri.org/GetSyougoData";
    public static final String SEND_SYOUGO_DATA = "http://tempuri.org/SendSyougoData";
}

