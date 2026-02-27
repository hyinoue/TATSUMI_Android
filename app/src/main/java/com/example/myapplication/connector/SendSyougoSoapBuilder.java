package com.example.myapplication.connector;

import com.example.myapplication.model.CollateData;
import com.example.myapplication.model.CollateDtl;
import com.example.myapplication.time.XmlUtil;


//============================================================
//　処理概要　:　照合データ送信用（SendSyougoData）のSOAP本文を組み立てるクラス
//　関　　数　:　buildSendSyougoData ..... SendSyougoData用SOAPボディ生成
//============================================================
public class SendSyougoSoapBuilder {
    private static final String NS = "http://tempuri.org/";

    //============================================================
    //　機　能　:　SendSyougoSoapBuilderの生成を禁止する（ユーティリティクラス化）
    //　引　数　:　なし
    //　戻り値　:　[SendSyougoSoapBuilder] ..... なし
    //============================================================
    private SendSyougoSoapBuilder() {
        // インスタンス化不要のため、コンストラクタはprivate
    }

    //============================================================
    //　機　能　:　照合データ送信用（SendSyougoData）のSOAPメッセージを生成する
    //　引　数　:　data ..... データ
    //　戻り値　:　[String] ..... SOAPメッセージ（Envelope + Body）
    //============================================================
    public static String buildSendSyougoData(CollateData data) {
        // Body内に入れるXMLを組み立てる
        StringBuilder inner = new StringBuilder();

        // ルート要素（メソッド名）と名前空間
        inner.append("<SendSyougoData xmlns=\"").append(NS).append("\">");

        // 引数dataの開始
        inner.append("<data>");

        // reference.csに合わせた要素名で出力：
        // containerID / syogoKanryo（どちらも先頭小文字）
        XmlUtil.tag(inner, "containerID", data.containerID);
        XmlUtil.tag(inner, "syogoKanryo", String.valueOf(data.syogoKanryo));

        // 配列：CollateDtls（親：先頭大文字）→ CollateDtl（子）
        inner.append("<CollateDtls>");
        for (CollateDtl d : data.collateDtls) {
            // 1明細分の開始
            inner.append("<CollateDtl>");

            // 明細項目をタグとして出力（reference.csのプロパティ名に合わせる）
            XmlUtil.tag(inner, "collateDtlheatNo", d.collateDtlheatNo);
            XmlUtil.tag(inner, "collateDtlsokuban", d.collateDtlsokuban);
            XmlUtil.tag(inner, "collateDtlsyougoKakunin", String.valueOf(d.collateDtlsyougoKakunin));

            // 1明細分の終了
            inner.append("</CollateDtl>");
        }
        inner.append("</CollateDtls>");

        // 引数dataの終了
        inner.append("</data>");

        // ルート要素（メソッド名）の終了
        inner.append("</SendSyougoData>");

        // SOAP Envelope/Bodyでラップして返却
        return SoapEnvelope.wrapBody(inner.toString());
    }
}
