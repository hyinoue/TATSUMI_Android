package com.example.myapplication.connector;

import android.util.Base64;

import com.example.myapplication.time.XmlUtil;

import java.util.Date;


//============================================================
//　処理概要　:　SoapRequestBuildersクラス
//　関　　数　:　buildGetSysDate ................................. GetSysDate要求XML生成
//　　　　　　:　buildGetSagyouYmd .............................. GetSagyouYmd要求XML生成
//　　　　　　:　buildGetUpdateYmdHms ........................... GetUpdateYmdHms要求XML生成
//　　　　　　:　buildGetSyukkaData ............................. GetSyukkaData要求XML生成
//　　　　　　:　buildGetSyougoData ............................. GetSyougoData要求XML生成
//　　　　　　:　buildUploadBinaryFile .......................... UploadBinaryFile要求XML生成
//　　　　　　:　buildGetDownloadHandyExecuteFileNames .......... GetDownloadHandyExecuteFileNames要求XML生成
//　　　　　　:　buildGetDownloadHandyExecuteFile ............... GetDownloadHandyExecuteFile要求XML生成
//============================================================
public class SoapRequestBuilders {

    private static final String NS = "http://tempuri.org/";

    //================================================================
    //　機　能　:　SoapRequestBuildersの生成を禁止する（ユーティリティクラス化）
    //　引　数　:　なし
    //　戻り値　:　[SoapRequestBuilders] ..... なし
    //================================================================
    private SoapRequestBuilders() {
        // static専用クラスのためインスタンス化させない
    }

    //================================================================
    //　機　能　:　GetSysDate要求のSOAPメッセージを生成する
    //　引　数　:　なし
    //　戻り値　:　[String] ..... SOAPメッセージ（Envelope + Body）
    //================================================================
    public static String buildGetSysDate() {
        // 引数なしのため自己終了タグで生成
        String body = "<GetSysDate xmlns=\"" + NS + "\" />";
        return SoapEnvelope.wrapBody(body);
    }

    //================================================================
    //　機　能　:　GetSagyouYmd要求のSOAPメッセージを生成する
    //　引　数　:　なし
    //　戻り値　:　[String] ..... SOAPメッセージ（Envelope + Body）
    //================================================================
    public static String buildGetSagyouYmd() {
        String body = "<GetSagyouYmd xmlns=\"" + NS + "\" />";
        return SoapEnvelope.wrapBody(body);
    }

    //================================================================
    //　機　能　:　GetUpdateYmdHms要求のSOAPメッセージを生成する
    //　引　数　:　sagyouYmd ..... Date
    //　戻り値　:　[String] ..... SOAPメッセージ（Envelope + Body）
    //================================================================
    public static String buildGetUpdateYmdHms(Date sagyouYmd) {
        // Body内XMLを組み立てる
        StringBuilder inner = new StringBuilder();

        // ルート要素（メソッド名）＋名前空間
        inner.append("<GetUpdateYmdHms xmlns=\"").append(NS).append("\">");

        // 引数名は reference.cs のメソッド引数名に合わせる（sagyouYmd）
        XmlUtil.tag(inner, "sagyouYmd", XmlUtil.toXsdDateTime(sagyouYmd));

        // ルート要素終了
        inner.append("</GetUpdateYmdHms>");

        return SoapEnvelope.wrapBody(inner.toString());
    }

    //================================================================
    //　機　能　:　GetSyukkaData要求のSOAPメッセージを生成する
    //　引　数　:　sagyouYmd ..... Date
    //　戻り値　:　[String] ..... SOAPメッセージ（Envelope + Body）
    //================================================================
    public static String buildGetSyukkaData(Date sagyouYmd) {
        StringBuilder inner = new StringBuilder();

        inner.append("<GetSyukkaData xmlns=\"").append(NS).append("\">");

        // 引数名は sagyouYmd（サーバ側の定義に合わせる）
        XmlUtil.tag(inner, "sagyouYmd", XmlUtil.toXsdDateTime(sagyouYmd));

        inner.append("</GetSyukkaData>");

        return SoapEnvelope.wrapBody(inner.toString());
    }

    //================================================================
    //　機　能　:　GetSyougoData要求のSOAPメッセージを生成する
    //　引　数　:　なし
    //　戻り値　:　[String] ..... SOAPメッセージ（Envelope + Body）
    //================================================================
    public static String buildGetSyougoData() {
        String body = "<GetSyougoData xmlns=\"" + NS + "\" />";
        return SoapEnvelope.wrapBody(body);
    }

    //================================================================
    //　機　能　:　UploadBinaryFile要求のSOAPメッセージを生成する
    //　引　数　:　fileName ..... String
    //　　　　　:　buffer ..... byte[]
    //　戻り値　:　[String] ..... SOAPメッセージ（Envelope + Body）
    //================================================================
    public static String buildUploadBinaryFile(String fileName, byte[] buffer) {
        StringBuilder inner = new StringBuilder();

        // ルート要素（メソッド名）＋名前空間
        inner.append("<UploadBinaryFile xmlns=\"").append(NS).append("\">");

        // 引数（ファイル名）
        XmlUtil.tag(inner, "fileName", fileName);

        // 引数（バイナリ本体）：base64Binary
        // nullの場合は空文字で送る（サーバ側で許容される前提）
        String encoded = (buffer == null || buffer.length == 0)
                ? ""
                : Base64.encodeToString(buffer, Base64.NO_WRAP);

        // Base64はエスケープ不要のため tagRaw を使用
        XmlUtil.tagRaw(inner, "buffer", encoded);

        inner.append("</UploadBinaryFile>");

        return SoapEnvelope.wrapBody(inner.toString());
    }

    //================================================================
    //　機　能　:　GetDownloadHandyExecuteFileNames要求のSOAPメッセージを生成する
    //　引　数　:　なし
    //　戻り値　:　[String] ..... SOAPメッセージ（Envelope + Body）
    //================================================================
    public static String buildGetDownloadHandyExecuteFileNames() {
        String body = "<GetDownloadHandyExecuteFileNames xmlns=\"" + NS + "\" />";
        return SoapEnvelope.wrapBody(body);
    }

    //================================================================
    //　機　能　:　GetDownloadHandyExecuteFile要求のSOAPメッセージを生成する
    //　引　数　:　fileName ..... String
    //　戻り値　:　[String] ..... SOAPメッセージ（Envelope + Body）
    //================================================================
    public static String buildGetDownloadHandyExecuteFile(String fileName) {
        StringBuilder inner = new StringBuilder();

        inner.append("<GetDownloadHandyExecuteFile xmlns=\"").append(NS).append("\">");

        // 引数（ファイル名）
        XmlUtil.tag(inner, "fileName", fileName);

        inner.append("</GetDownloadHandyExecuteFile>");

        return SoapEnvelope.wrapBody(inner.toString());
    }
}
