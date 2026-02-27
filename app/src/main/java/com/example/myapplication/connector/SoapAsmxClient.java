package com.example.myapplication.connector;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


//========================================================================================
//　処理概要　:　.asmx向けSOAPリクエスト送信とレスポンス受信を担当するHTTPクラス
//　関　　数　:　SoapAsmxClient .......................... 初期化（OkHttp設定）
//　　　　　　:　call .................................... SOAP呼び出し（POST送信→レスポンス取得）
//========================================================================================
public class SoapAsmxClient {

    // SOAP(ASMX)送信用 Content-Type
    private static final MediaType SOAP_XML = MediaType.parse("text/xml; charset=utf-8");

    private final OkHttpClient http; // HTTPクライアント
    private final String endpointUrl; // 接続先エンドポイントURL

    //============================================================
    //　機　能　:　SoapAsmxClientを初期化する
    //　引　数　:　endpointUrl ..... 接続先URL
    //　戻り値　:　[SoapAsmxClient] ..... なし
    //============================================================
    public SoapAsmxClient(String endpointUrl) {
        this.endpointUrl = endpointUrl;

        // 通信タイムアウト設定
        this.http = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .build();
    }

    //============================================================
    //　機　能　:　SOAP(ASMX)を呼び出してレスポンスXMLを取得する
    //　引　数　:　soapAction ..... SOAPアクション名
    //　　　　　:　soapEnvelopeXml ..... XML文字列
    //　戻り値　:　[String] ..... SOAPレスポンスXML
    //============================================================
    public String call(String soapAction, String soapEnvelopeXml) throws IOException {

        // 送信XMLをRequestBodyへ設定
        RequestBody body = RequestBody.create(soapEnvelopeXml, SOAP_XML);

        // ASMXは SOAPAction をダブルクォート付きで送ると安定しやすい
        Request req = new Request.Builder()
                .url(endpointUrl)
                .post(body)
                .addHeader("Content-Type", "text/xml; charset=utf-8")
                .addHeader("SOAPAction", "\"" + soapAction + "\"")
                .build();

        // HTTP呼び出し（try-with-resourcesでResponseを必ずcloseする）
        try (Response res = http.newCall(req).execute()) {

            // ボディを文字列として取得（nullの場合は空文字）
            String payload = (res.body() != null) ? res.body().string() : "";

            // HTTPステータスが失敗の場合は例外化（本文も付与して原因調査しやすくする）
            if (!res.isSuccessful()) {
                throw new IOException("HTTP " + res.code() + " " + res.message() + "\n" + payload);
            }

            // 成功時：SOAPレスポンスXMLを返却
            return payload;
        }
    }
}
