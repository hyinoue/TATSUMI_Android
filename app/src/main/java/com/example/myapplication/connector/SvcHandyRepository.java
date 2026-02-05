package com.example.myapplication.connector;

import com.example.myapplication.model.BunningData;
import com.example.myapplication.model.CollateData;
import com.example.myapplication.model.SyougoData;
import com.example.myapplication.model.SyukkaData;

import java.util.Date;


//===================================
//　処理概要　:　SvcHandyRepositoryクラス
//===================================

public class SvcHandyRepository {

    // ASMXエンドポイント（※ ?wsdl は付けない）
    public static final String DEFAULT_ENDPOINT =
            "https://scstestvanningreport.azurewebsites.net/WebSvc/SvcHandy.asmx";

    private final SoapAsmxClient client;

    // ★追加：デフォルトエンドポイント用コンストラクタ
    //==========================================
    //　機　能　:　SvcHandyRepositoryの初期化処理
    //　引　数　:　なし
    //　戻り値　:　[SvcHandyRepository] ..... なし
    //==========================================
    public SvcHandyRepository() {
        this(DEFAULT_ENDPOINT);
    }
    //==========================================
    //　機　能　:　SvcHandyRepositoryの初期化処理
    //　引　数　:　endpointUrl ..... String
    //　戻り値　:　[SvcHandyRepository] ..... なし
    //==========================================

    public SvcHandyRepository(String endpointUrl) {
        this.client = new SoapAsmxClient(endpointUrl);
    }

    // GetSysDate(): DateTime
    //============================
    //　機　能　:　sys Dateを取得する
    //　引　数　:　なし
    //　戻り値　:　[Date] ..... なし
    //============================
    public Date getSysDate() throws Exception {
        String req = SoapRequestBuilders.buildGetSysDate();
        String res = client.call(SoapActions.GET_SYS_DATE, req);
        SoapParsers.throwIfSoapFault(res);
        return SoapParsers.parseDateTimeResult(res, "GetSysDateResult");
    }

    // GetSagyouYmd(): DateTime
    //============================
    //　機　能　:　sagyou Ymdを取得する
    //　引　数　:　なし
    //　戻り値　:　[Date] ..... なし
    //============================
    public Date getSagyouYmd() throws Exception {
        String req = SoapRequestBuilders.buildGetSagyouYmd();
        String res = client.call(SoapActions.GET_SAGYOU_YMD, req);
        SoapParsers.throwIfSoapFault(res);
        return SoapParsers.parseDateTimeResult(res, "GetSagyouYmdResult");
    }

    // GetUpdateYmdHms(DateTime): DateTime
    //=================================
    //　機　能　:　update Ymd Hmsを取得する
    //　引　数　:　sagyouYmd ..... Date
    //　戻り値　:　[Date] ..... なし
    //=================================
    public Date getUpdateYmdHms(Date sagyouYmd) throws Exception {
        String req = SoapRequestBuilders.buildGetUpdateYmdHms(sagyouYmd);
        String res = client.call(SoapActions.GET_UPDATE_YMD_HMS, req);
        SoapParsers.throwIfSoapFault(res);
        return SoapParsers.parseDateTimeResult(res, "GetUpdateYmdHmsResult");
    }

    // GetSyukkaData(DateTime): SyukkaData
    //==================================
    //　機　能　:　syukka Dataを取得する
    //　引　数　:　sagyouYmd ..... Date
    //　戻り値　:　[SyukkaData] ..... なし
    //==================================
    public SyukkaData getSyukkaData(Date sagyouYmd) throws Exception {
        String req = SoapRequestBuilders.buildGetSyukkaData(sagyouYmd);
        String res = client.call(SoapActions.GET_SYUKKA_DATA, req);
        SoapParsers.throwIfSoapFault(res);

        return SoapParsers.parseSyukkaDataResult(res);
    }

    // SendSyukkaData(BunningData): bool
//    public boolean sendSyukkaData(BunningData data) throws Exception {
//        String req = SendSyukkaSoapBuilder.buildSendSyukkaData(data);
//        String res = client.call(SoapActions.SEND_SYUKKA_DATA, req);
//        SoapParsers.throwIfSoapFault(res);
//        return SoapParsers.parseBooleanResult(res, "SendSyukkaDataResult");
//    }
    // SendSyukkaData(BunningData): bool  ログ出力版
    //===================================
    //　機　能　:　syukka Dataを送信する
    //　引　数　:　data ..... BunningData
    //　戻り値　:　[boolean] ..... なし
    //===================================
    public boolean sendSyukkaData(BunningData data) throws Exception {
        String action = "http://tempuri.org/SendSyukkaData";

        // ★ リクエストXML作成
        String req = SendSyukkaSoapBuilder.buildSendSyukkaData(data);

        // ===== 送信SOAPログ =====
        android.util.Log.i("SOAP_REQ", "===== SendSyukkaData REQUEST START =====");
        android.util.Log.i("SOAP_REQ", req);
        android.util.Log.i("SOAP_REQ", "===== SendSyukkaData REQUEST END =====");

        // ★ SOAP送信
        String res = client.call(action, req);

        // ===== レスポンスSOAPログ =====
        android.util.Log.i("SOAP_RES", "===== SendSyukkaData RESPONSE START =====");
        android.util.Log.i("SOAP_RES", res);
        android.util.Log.i("SOAP_RES", "===== SendSyukkaData RESPONSE END =====");

        // ★ SOAP Faultチェック
        SoapParsers.throwIfSoapFault(res);

        // ★ 結果判定
        boolean result = SoapParsers.parseBooleanResult(res, "SendSyukkaDataResult");
        android.util.Log.i("SOAP_RES", "SendSyukkaDataResult=" + result);

        return result;
    }

    // GetSyougoData(): SyougoData
    //==================================
    //　機　能　:　syougo Dataを取得する
    //　引　数　:　なし
    //　戻り値　:　[SyougoData] ..... なし
    //==================================
    public SyougoData getSyougoData() throws Exception {
        String req = SoapRequestBuilders.buildGetSyougoData();
        String res = client.call(SoapActions.GET_SYOUGO_DATA, req);
        SoapParsers.throwIfSoapFault(res);
        return SoapParsers.parseSyougoDataResult(res);
    }

    // SendSyougoData(CollateData): bool
    //===================================
    //　機　能　:　syougo Dataを送信する
    //　引　数　:　data ..... CollateData
    //　戻り値　:　[boolean] ..... なし
    //===================================
    public boolean sendSyougoData(CollateData data) throws Exception {
        String req = SendSyougoSoapBuilder.buildSendSyougoData(data);
        String res = client.call(SoapActions.SEND_SYOUGO_DATA, req);
        SoapParsers.throwIfSoapFault(res);
        return SoapParsers.parseBooleanResult(res, "SendSyougoDataResult");
    }

    // UploadBinaryFile(string fileName, byte[] buffer): bool
    //===========================================
    //　機　能　:　upload Binary Fileを送信する
    //　引　数　:　fileName ..... String
    //　　　　　:　buffer ..... byte[]
    //　戻り値　:　[boolean] ..... なし
    //===========================================
    public boolean uploadBinaryFile(String fileName, byte[] buffer) throws Exception {
        String req = SoapRequestBuilders.buildUploadBinaryFile(fileName, buffer);
        String res = client.call(SoapActions.UPLOAD_BINARY_FILE, req);
        SoapParsers.throwIfSoapFault(res);

        // WebMethod が void の場合、UploadBinaryFileResult は返らない。
        // Fault が無ければ成功扱い。
        if (res.contains("UploadBinaryFileResult")) {
            return SoapParsers.parseBooleanResult(res, "UploadBinaryFileResult");
        }
        return true;
    }

    // GetDownloadHandyExecuteFileNames(): string[]
    //====================================================
    //　機　能　:　download Handy Execute File Namesを取得する
    //　引　数　:　なし
    //　戻り値　:　[String[]] ..... なし
    //====================================================
    public String[] getDownloadHandyExecuteFileNames() throws Exception {
        String req = SoapRequestBuilders.buildGetDownloadHandyExecuteFileNames();
        String res = client.call(SoapActions.GET_DOWNLOAD_HANDY_EXECUTE_FILE_NAMES, req);
        SoapParsers.throwIfSoapFault(res);
        return SoapParsers.parseStringArrayResult(res, "GetDownloadHandyExecuteFileNamesResult");
    }

    // GetDownloadHandyExecuteFile(string fileName): byte[]
    //================================================
    //　機　能　:　download Handy Execute Fileを取得する
    //　引　数　:　fileName ..... String
    //　戻り値　:　[byte[]] ..... なし
    //================================================
    public byte[] getDownloadHandyExecuteFile(String fileName) throws Exception {
        String req = SoapRequestBuilders.buildGetDownloadHandyExecuteFile(fileName);
        String res = client.call(SoapActions.GET_DOWNLOAD_HANDY_EXECUTE_FILE, req);
        SoapParsers.throwIfSoapFault(res);
        return SoapParsers.parseBase64Result(res, "GetDownloadHandyExecuteFileResult");
    }
}
