package com.example.myapplication.connector;

import android.util.Base64;

import com.example.myapplication.model.BunningData;
import com.example.myapplication.model.SyukkaMeisai;
import com.example.myapplication.time.XmlUtil;


//============================================================
//　処理概要　:　サーバー通信とSOAPデータ処理を行うクラス
//　関　　数　:　buildSendSyukkaData ..... SendSyukkaData用SOAPボディ生成
//============================================================
public class SendSyukkaSoapBuilder {

    private static final String NS = "http://tempuri.org/";

    //============================================================
    //　機　能　:　SendSyukkaSoapBuilderの生成を禁止する（ユーティリティクラス化）
    //　引　数　:　なし
    //　戻り値　:　[SendSyukkaSoapBuilder] ..... なし
    //============================================================
    private SendSyukkaSoapBuilder() {
        // static専用クラスのためインスタンス化させない
    }

    //============================================================
    //　機　能　:　出荷データ送信用（SendSyukkaData）のSOAPメッセージを生成する
    //　引　数　:　data ..... データ
    //　戻り値　:　[String] ..... SOAPメッセージ（Envelope + Body）
    //============================================================
    public static String buildSendSyukkaData(BunningData data) {

        // Body内に埋め込むXML文字列を構築する
        StringBuilder inner = new StringBuilder();

        // ルート要素（メソッド名）＋名前空間
        inner.append("<SendSyukkaData xmlns=\"").append(NS).append("\">");

        // 引数dataの開始タグ
        inner.append("<data>");

        // reference.cs のプロパティ名（大小文字含む）に合わせて出力
        XmlUtil.tag(inner, "SyukkaYmd", XmlUtil.toXsdDateTime(data.syukkaYmd));
        XmlUtil.tag(inner, "ContainerNo", data.containerNo);
        XmlUtil.tag(inner, "ContainerJyuryo", String.valueOf(data.containerJyuryo));
        XmlUtil.tag(inner, "DunnageJyuryo", String.valueOf(data.dunnageJyuryo));
        XmlUtil.tag(inner, "SealNo", data.sealNo);

        //============================================================
        // 明細配列：Bundles
        //============================================================
        inner.append("<Bundles>");

        // 束（SyukkaMeisai）を1件ずつXMLへ変換
        for (SyukkaMeisai b : data.bundles) {

            // 明細1件の開始
            inner.append("<SyukkaMeisai>");

            // 各項目をタグとして出力（プロパティ名に厳密一致させる）
            XmlUtil.tag(inner, "HeatNo", b.heatNo);
            XmlUtil.tag(inner, "Sokuban", b.sokuban);
            XmlUtil.tag(inner, "SyukkaSashizuNo", b.syukkaSashizuNo);

            // ★ bundleNo は先頭小文字（reference.cs仕様）
            XmlUtil.tag(inner, "bundleNo", b.bundleNo);

            XmlUtil.tag(inner, "Jyuryo", String.valueOf(b.jyuryo));
            XmlUtil.tag(inner, "BookingNo", b.bookingNo);

            // 明細1件の終了
            inner.append("</SyukkaMeisai>");
        }

        inner.append("</Bundles>");

        //============================================================
        // 画像（base64Binary）
        //============================================================

        // コンテナ写真が存在する場合のみ出力
        if (data.containerPhoto != null && data.containerPhoto.length > 0) {

            // 改行なしでBase64エンコード
            String b64 = Base64.encodeToString(data.containerPhoto, Base64.NO_WRAP);

            // 画像はエスケープ不要のため tagRaw を使用
            XmlUtil.tagRaw(inner, "ContainerPhoto", b64);
        }

        // シール写真が存在する場合のみ出力
        if (data.sealPhoto != null && data.sealPhoto.length > 0) {

            String b64 = Base64.encodeToString(data.sealPhoto, Base64.NO_WRAP);
            XmlUtil.tagRaw(inner, "SealPhoto", b64);
        }

        // 引数data終了
        inner.append("</data>");

        // メソッドルート要素終了
        inner.append("</SendSyukkaData>");

        // SOAP Envelopeでラップして返却
        return SoapEnvelope.wrapBody(inner.toString());
    }
}
