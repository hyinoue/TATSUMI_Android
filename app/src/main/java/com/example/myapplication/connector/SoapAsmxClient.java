package com.example.myapplication.connector;

import android.util.Log;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SoapAsmxClient {
    private static final MediaType SOAP_XML = MediaType.parse("text/xml; charset=utf-8");
    private static final String TAG_RAW = "SOAP_RAW";
    private static final String TAG = "SOAP";

    private final OkHttpClient http;
    private final String endpointUrl;

    public SoapAsmxClient(String endpointUrl) {
        this.endpointUrl = endpointUrl;
        this.http = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .build();
    }

    public String call(String soapAction, String soapEnvelopeXml) throws IOException {
        RequestBody body = RequestBody.create(soapEnvelopeXml, SOAP_XML);

        Request req = new Request.Builder()
                .url(endpointUrl)
                .post(body)
                .addHeader("Content-Type", "text/xml; charset=utf-8")
                // ★ASMXは SOAPAction をダブルクォート付きで送るのが安定
                .addHeader("SOAPAction", "\"" + soapAction + "\"")
                .build();

        // ★リクエスト側も一応出す（長いので必要なら）
        Log.d(TAG, "call: " + soapAction);

        try (Response res = http.newCall(req).execute()) {
            String payload = (res.body() != null) ? res.body().string() : "";

            // ★ここが切り分けの本命：SOAPレスポンス生XMLを出す
            // （GetSyukkaDataResult の中に明細があるか？タグ名は何か？が分かる）
            Log.d(TAG_RAW, "---- action ----\n" + soapAction + "\n---- response ----\n" + payload);

            if (!res.isSuccessful()) {
                throw new IOException("HTTP " + res.code() + " " + res.message() + "\n" + payload);
            }
            return payload;
        }
    }
}


//ログ出力用
//package com.example.myapplication.soap;
//
//import android.util.Log;
//
//import java.io.IOException;
//import java.util.concurrent.TimeUnit;
//
//import okhttp3.MediaType;
//import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.RequestBody;
//import okhttp3.Response;
//
/// **
// * ASMX（SOAP 1.1）用クライアント
// * ・送信XML（SOAPリクエスト）
// * ・受信XML（SOAPレスポンス）
// * を logcat に出力する
// */
//public class SoapAsmxClient {
//
//    private static final MediaType SOAP_XML =
//            MediaType.parse("text/xml; charset=utf-8");
//
//    // logcat 用タグ
//    private static final String TAG_REQ = "SOAP_REQ";
//    private static final String TAG_RES = "SOAP_RAW";
//    private static final String TAG = "SOAP";
//
//    private final OkHttpClient http;
//    private final String endpointUrl;
//
//    public SoapAsmxClient(String endpointUrl) {
//        this.endpointUrl = endpointUrl;
//        this.http = new OkHttpClient.Builder()
//                .connectTimeout(20, TimeUnit.SECONDS)
//                .readTimeout(120, TimeUnit.SECONDS)
//                .writeTimeout(120, TimeUnit.SECONDS)
//                .build();
//    }
//
//    /**
//     * SOAP 呼び出し
//     *
//     * @param soapAction      例: http://tempuri.org/GetSyukkaData
//     * @param soapEnvelopeXml SOAP Envelope 全文
//     * @return SOAPレスポンスXML
//     */
//    public String call(String soapAction, String soapEnvelopeXml) throws IOException {
//
//        // ===== ① 送信するSOAPリクエストXMLを出力 =====
//        Log.d(TAG_REQ,
//                "---- action ----\n"
//                        + soapAction
//                        + "\n---- request ----\n"
//                        + soapEnvelopeXml
//        );
//
//        RequestBody body = RequestBody.create(soapEnvelopeXml, SOAP_XML);
//
//        Request req = new Request.Builder()
//                .url(endpointUrl)
//                .post(body)
//                .addHeader("Content-Type", "text/xml; charset=utf-8")
//                // ASMXは SOAPAction をダブルクォート付きで送るのが安定
//                .addHeader("SOAPAction", "\"" + soapAction + "\"")
//                .build();
//
//        try (Response res = http.newCall(req).execute()) {
//
//            String payload = (res.body() != null)
//                    ? res.body().string()
//                    : "";
//
//            // ===== ② 受信したSOAPレスポンスXMLを出力 =====
//            Log.d(TAG_RES,
//                    "---- action ----\n"
//                            + soapAction
//                            + "\n---- response ----\n"
//                            + payload
//            );
//
//            if (!res.isSuccessful()) {
//                Log.e(TAG,
//                        "HTTP ERROR: "
//                                + res.code()
//                                + " "
//                                + res.message()
//                );
//                throw new IOException(
//                        "HTTP " + res.code() + " " + res.message() + "\n" + payload
//                );
//            }
//
//            return payload;
//        }
//    }
//}

