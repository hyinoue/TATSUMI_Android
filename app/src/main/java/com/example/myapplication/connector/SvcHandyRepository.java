package com.example.myapplication.connector;

import android.util.Log;

import com.example.myapplication.model.BunningData;
import com.example.myapplication.model.CollateData;
import com.example.myapplication.model.SyougoData;
import com.example.myapplication.model.SyukkaData;

import java.util.Date;

public class SvcHandyRepository {
    public static final String DEFAULT_ENDPOINT =
            "https://scstestvanningreport.azurewebsites.net/WebSvc/SvcHandy.asmx";

    private final SoapAsmxClient client;

    public SvcHandyRepository() {
        this(DEFAULT_ENDPOINT);
    }

    public SvcHandyRepository(String endpointUrl) {
        this.client = new SoapAsmxClient(endpointUrl);
    }

    public Date getSysDate() throws Exception {
        String res = invoke(SoapActions.GET_SYS_DATE, SoapRequestBuilders.buildGetSysDate());
        return SoapParsers.parseDateTimeResult(res, "GetSysDateResult");
    }

    public Date getSagyouYmd() throws Exception {
        String res = invoke(SoapActions.GET_SAGYOU_YMD, SoapRequestBuilders.buildGetSagyouYmd());
        return SoapParsers.parseDateTimeResult(res, "GetSagyouYmdResult");
    }

    public Date getUpdateYmdHms(Date sagyouYmd) throws Exception {
        String res = invoke(SoapActions.GET_UPDATE_YMD_HMS, SoapRequestBuilders.buildGetUpdateYmdHms(sagyouYmd));
        return SoapParsers.parseDateTimeResult(res, "GetUpdateYmdHmsResult");
    }

    public SyukkaData getSyukkaData(Date sagyouYmd) throws Exception {
        String res = invoke(SoapActions.GET_SYUKKA_DATA, SoapRequestBuilders.buildGetSyukkaData(sagyouYmd));
        return SoapParsers.parseSyukkaDataResult(res);
    }

    public boolean sendSyukkaData(BunningData data) throws Exception {
        String action = SoapActions.SEND_SYUKKA_DATA;
        String req = SendSyukkaSoapBuilder.buildSendSyukkaData(data);
        Log.i("SOAP_REQ", "===== SendSyukkaData REQUEST START =====");
        Log.i("SOAP_REQ", req);
        Log.i("SOAP_REQ", "===== SendSyukkaData REQUEST END =====");

        String res = invoke(action, req);

        Log.i("SOAP_RES", "===== SendSyukkaData RESPONSE START =====");
        Log.i("SOAP_RES", res);
        Log.i("SOAP_RES", "===== SendSyukkaData RESPONSE END =====");

        boolean result = SoapParsers.parseBooleanResult(res, "SendSyukkaDataResult");
        Log.i("SOAP_RES", "SendSyukkaDataResult=" + result);
        return result;
    }

    public SyougoData getSyougoData() throws Exception {
        String res = invoke(SoapActions.GET_SYOUGO_DATA, SoapRequestBuilders.buildGetSyougoData());
        return SoapParsers.parseSyougoDataResult(res);
    }

    public boolean sendSyougoData(CollateData data) throws Exception {
        String res = invoke(SoapActions.SEND_SYOUGO_DATA, SendSyougoSoapBuilder.buildSendSyougoData(data));
        return SoapParsers.parseBooleanResult(res, "SendSyougoDataResult");
    }

    public boolean uploadBinaryFile(String fileName, byte[] buffer) throws Exception {
        String res = invoke(SoapActions.UPLOAD_BINARY_FILE, SoapRequestBuilders.buildUploadBinaryFile(fileName, buffer));
        if (res.contains("UploadBinaryFileResult")) {
            return SoapParsers.parseBooleanResult(res, "UploadBinaryFileResult");
        }
        return true;
    }

    public String[] getDownloadHandyExecuteFileNames() throws Exception {
        String res = invoke(
                SoapActions.GET_DOWNLOAD_HANDY_EXECUTE_FILE_NAMES,
                SoapRequestBuilders.buildGetDownloadHandyExecuteFileNames()
        );
        return SoapParsers.parseStringArrayResult(res, "GetDownloadHandyExecuteFileNamesResult");
    }

    public byte[] getDownloadHandyExecuteFile(String fileName) throws Exception {
        String res = invoke(
                SoapActions.GET_DOWNLOAD_HANDY_EXECUTE_FILE,
                SoapRequestBuilders.buildGetDownloadHandyExecuteFile(fileName)
        );
        return SoapParsers.parseBase64Result(res, "GetDownloadHandyExecuteFileResult");
    }

    private String invoke(String action, String requestXml) throws Exception {
        String res = client.call(action, requestXml);
        SoapParsers.throwIfSoapFault(res);
        return res;
    }
}
