package com.example.myapplication.connector;

import com.example.myapplication.model.BunningData;
import com.example.myapplication.model.CollateData;
import com.example.myapplication.model.SyougoData;
import com.example.myapplication.model.SyukkaData;

import java.util.Date;


//============================================================
//　処理概要　:　SvcHandyRepositoryクラス
//　関　　数　:　getSysDate .......................... システム日時取得
//　　　　　　:　getSagyouYmd ....................... 作業日取得
//　　　　　　:　getUpdateYmdHms .................... 更新日時取得
//　　　　　　:　getSyukkaData ...................... 出荷データ取得
//　　　　　　:　sendSyukkaData ..................... 出荷データ送信
//　　　　　　:　getSyougoData ...................... 照合データ取得
//　　　　　　:　sendSyougoData ..................... 照合データ送信
//　　　　　　:　uploadBinaryFile ................... バイナリファイル送信
//　　　　　　:　getDownloadHandyExecuteFileNames ... 実行ファイル名一覧取得
//　　　　　　:　getDownloadHandyExecuteFile ........ 実行ファイル取得
//============================================================
public class SvcHandyRepository {

    // ASMXエンドポイント（※ ?wsdl は付けない）
    public static final String DEFAULT_ENDPOINT =
            "https://scstestvanningreport.azurewebsites.net/WebSvc/SvcHandy.asmx";

    private final SoapAsmxClient client; // SOAP通信クライアント

    //================================================================
    //　機　能　:　SvcHandyRepositoryを初期化する（デフォルト）
    //　引　数　:　なし
    //　戻り値　:　[SvcHandyRepository] ..... なし
    //================================================================
    public SvcHandyRepository() {
        this(DEFAULT_ENDPOINT);
    }

    //================================================================
    //　機　能　:　SvcHandyRepositoryを初期化する（エンドポイント指定）
    //　引　数　:　endpointUrl ..... String
    //　戻り値　:　[SvcHandyRepository] ..... なし
    //================================================================
    public SvcHandyRepository(String endpointUrl) {
        this.client = new SoapAsmxClient(endpointUrl);
    }

    //================================================================
    //　機　能　:　システム日時を取得する
    //　引　数　:　なし
    //　戻り値　:　[Date] ..... システム日時
    //================================================================
    public Date getSysDate() throws Exception {
        String req = SoapRequestBuilders.buildGetSysDate();
        String res = client.call(SoapActions.GET_SYS_DATE, req);
        SoapParsers.throwIfSoapFault(res);
        return SoapParsers.parseDateTimeResult(res, "GetSysDateResult");
    }

    //================================================================
    //　機　能　:　作業日を取得する
    //　引　数　:　なし
    //　戻り値　:　[Date] ..... 作業日
    //================================================================
    public Date getSagyouYmd() throws Exception {
        String req = SoapRequestBuilders.buildGetSagyouYmd();
        String res = client.call(SoapActions.GET_SAGYOU_YMD, req);
        SoapParsers.throwIfSoapFault(res);
        return SoapParsers.parseDateTimeResult(res, "GetSagyouYmdResult");
    }

    //================================================================
    //　機　能　:　更新日時を取得する
    //　引　数　:　sagyouYmd ..... Date
    //　戻り値　:　[Date] ..... 更新日時
    //================================================================
    public Date getUpdateYmdHms(Date sagyouYmd) throws Exception {
        String req = SoapRequestBuilders.buildGetUpdateYmdHms(sagyouYmd);
        String res = client.call(SoapActions.GET_UPDATE_YMD_HMS, req);
        SoapParsers.throwIfSoapFault(res);
        return SoapParsers.parseDateTimeResult(res, "GetUpdateYmdHmsResult");
    }

    //================================================================
    //　機　能　:　出荷データを取得する
    //　引　数　:　sagyouYmd ..... Date
    //　戻り値　:　[SyukkaData] ..... 出荷データ
    //================================================================
    public SyukkaData getSyukkaData(Date sagyouYmd) throws Exception {
        String req = SoapRequestBuilders.buildGetSyukkaData(sagyouYmd);
        String res = client.call(SoapActions.GET_SYUKKA_DATA, req);
        SoapParsers.throwIfSoapFault(res);
        return SoapParsers.parseSyukkaDataResult(res);
    }

    //================================================================
    //　機　能　:　出荷データを送信する
    //　引　数　:　data ..... BunningData
    //　戻り値　:　[boolean] ..... 送信結果（成功:true / 失敗:false）
    //================================================================
    public boolean sendSyukkaData(BunningData data) throws Exception {
        String req = SendSyukkaSoapBuilder.buildSendSyukkaData(data);
        String res = client.call(SoapActions.SEND_SYUKKA_DATA, req);
        SoapParsers.throwIfSoapFault(res);
        return SoapParsers.parseBooleanResult(res, "SendSyukkaDataResult");
    }

    //================================================================
    //　機　能　:　照合データを取得する
    //　引　数　:　なし
    //　戻り値　:　[SyougoData] ..... 照合データ
    //================================================================
    public SyougoData getSyougoData() throws Exception {
        String req = SoapRequestBuilders.buildGetSyougoData();
        String res = client.call(SoapActions.GET_SYOUGO_DATA, req);
        SoapParsers.throwIfSoapFault(res);
        return SoapParsers.parseSyougoDataResult(res);
    }

    //================================================================
    //　機　能　:　照合データを送信する
    //　引　数　:　data ..... CollateData
    //　戻り値　:　[boolean] ..... 送信結果（成功:true / 失敗:false）
    //================================================================
    public boolean sendSyougoData(CollateData data) throws Exception {
        String req = SendSyougoSoapBuilder.buildSendSyougoData(data);
        String res = client.call(SoapActions.SEND_SYOUGO_DATA, req);
        SoapParsers.throwIfSoapFault(res);
        return SoapParsers.parseBooleanResult(res, "SendSyougoDataResult");
    }

    //================================================================
    //　機　能　:　バイナリファイルを送信する
    //　引　数　:　fileName ..... String
    //　　　　　:　buffer ..... byte[]
    //　戻り値　:　[boolean] ..... 成功:true
    //================================================================
    public boolean uploadBinaryFile(String fileName, byte[] buffer) throws Exception {
        String req = SoapRequestBuilders.buildUploadBinaryFile(fileName, buffer);
        String res = client.call(SoapActions.UPLOAD_BINARY_FILE, req);
        SoapParsers.throwIfSoapFault(res);

        // WebMethodがvoidの場合はResultが返らない
        if (res.contains("UploadBinaryFileResult")) {
            return SoapParsers.parseBooleanResult(res, "UploadBinaryFileResult");
        }
        return true;
    }

    //================================================================
    //　機　能　:　実行ファイル名一覧を取得する
    //　引　数　:　なし
    //　戻り値　:　[String[]] ..... ファイル名一覧
    //================================================================
    public String[] getDownloadHandyExecuteFileNames() throws Exception {
        String req = SoapRequestBuilders.buildGetDownloadHandyExecuteFileNames();
        String res = client.call(SoapActions.GET_DOWNLOAD_HANDY_EXECUTE_FILE_NAMES, req);
        SoapParsers.throwIfSoapFault(res);
        return SoapParsers.parseStringArrayResult(res, "GetDownloadHandyExecuteFileNamesResult");
    }

    //================================================================
    //　機　能　:　実行ファイルを取得する
    //　引　数　:　fileName ..... String
    //　戻り値　:　[byte[]] ..... ファイルデータ
    //================================================================
    public byte[] getDownloadHandyExecuteFile(String fileName) throws Exception {
        String req = SoapRequestBuilders.buildGetDownloadHandyExecuteFile(fileName);
        String res = client.call(SoapActions.GET_DOWNLOAD_HANDY_EXECUTE_FILE, req);
        SoapParsers.throwIfSoapFault(res);
        return SoapParsers.parseBase64Result(res, "GetDownloadHandyExecuteFileResult");
    }
}
