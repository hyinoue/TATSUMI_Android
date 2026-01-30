package com.example.myapplication.connector;

import com.example.myapplication.model.BunningData;
import com.example.myapplication.model.CollateData;
import com.example.myapplication.model.SyougoData;
import com.example.myapplication.model.SyukkaData;

import java.util.Date;


//============================================================
//　処理概要　:　SvcHandyRepositoryクラス
//============================================================

public class SvcHandyRepository {

    // ASMXエンドポイント（※ ?wsdl は付けない）
    public static final String DEFAULT_ENDPOINT =
            "https://scstestvanningreport.azurewebsites.net/WebSvc/SvcHandy.asmx";

    private final SoapAsmxClient client;

    // ★追加：デフォルトエンドポイント用コンストラクタ
    public SvcHandyRepository() {
        this(DEFAULT_ENDPOINT);
    }

    public SvcHandyRepository(String endpointUrl) {
        this.client = new SoapAsmxClient(endpointUrl);
    }

    // GetSysDate(): DateTime
    public Date getSysDate() throws Exception {
        String req = SoapRequestBuilders.buildGetSysDate();
        String res = client.call(SoapActions.GET_SYS_DATE, req);
        SoapParsers.throwIfSoapFault(res);
        return SoapParsers.parseDateTimeResult(res, "GetSysDateResult");
    }

    // GetSagyouYmd(): DateTime
    public Date getSagyouYmd() throws Exception {
        String req = SoapRequestBuilders.buildGetSagyouYmd();
        String res = client.call(SoapActions.GET_SAGYOU_YMD, req);
        SoapParsers.throwIfSoapFault(res);
        return SoapParsers.parseDateTimeResult(res, "GetSagyouYmdResult");
    }

    // GetUpdateYmdHms(DateTime): DateTime
    public Date getUpdateYmdHms(Date sagyouYmd) throws Exception {
        String req = SoapRequestBuilders.buildGetUpdateYmdHms(sagyouYmd);
        String res = client.call(SoapActions.GET_UPDATE_YMD_HMS, req);
        SoapParsers.throwIfSoapFault(res);
        return SoapParsers.parseDateTimeResult(res, "GetUpdateYmdHmsResult");
    }

    // GetSyukkaData(DateTime): SyukkaData
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
    public SyougoData getSyougoData() throws Exception {
        String req = SoapRequestBuilders.buildGetSyougoData();
        String res = client.call(SoapActions.GET_SYOUGO_DATA, req);
        SoapParsers.throwIfSoapFault(res);
        return SoapParsers.parseSyougoDataResult(res);
    }

    // SendSyougoData(CollateData): bool
    public boolean sendSyougoData(CollateData data) throws Exception {
        String req = SendSyougoSoapBuilder.buildSendSyougoData(data);
        String res = client.call(SoapActions.SEND_SYOUGO_DATA, req);
        SoapParsers.throwIfSoapFault(res);
        return SoapParsers.parseBooleanResult(res, "SendSyougoDataResult");
    }
}
