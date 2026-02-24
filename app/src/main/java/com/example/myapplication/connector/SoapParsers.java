package com.example.myapplication.connector;

import android.util.Base64;

import com.example.myapplication.model.SyougoData;
import com.example.myapplication.model.SyougoDtl;
import com.example.myapplication.model.SyougoHeader;
import com.example.myapplication.model.SyukkaData;
import com.example.myapplication.model.SyukkaHeader;
import com.example.myapplication.model.SyukkaMeisai;
import com.example.myapplication.time.XsdDateTime;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


//============================================================
//　処理概要　:　SoapParsersクラス
//　関　　数　:　throwIfSoapFault ......................... SOAP Fault検出時に例外送出
//　　　　　　:　parseBooleanResult ...................... bool結果の取得
//　　　　　　:　parseTextResult ......................... 文字列結果の取得
//　　　　　　:　parseDateTimeResult ..................... DateTime結果の取得
//　　　　　　:　parseStringArrayResult .................. string配列結果の取得
//　　　　　　:　parseBase64Result ....................... base64Binary結果の取得
//　　　　　　:　parseSyukkaDataResult ................... 出荷データ結果の完全パース
//　　　　　　:　parseSyougoDataResult ................... 照合データ結果の完全パース
//　　　　　　:　newParser ............................... XmlPullParser生成
//　　　　　　:　readSyukkaHeaderArray ................... 出荷ヘッダ配列パース
//　　　　　　:　readSyukkaHeader ........................ 出荷ヘッダ1件パース
//　　　　　　:　readSyukkaMeisaiArray ................... 出荷明細配列パース
//　　　　　　:　readSyukkaMeisai ........................ 出荷明細1件パース
//　　　　　　:　readSyougoHeaderArray ................... 照合ヘッダ配列パース
//　　　　　　:　readSyougoHeader ........................ 照合ヘッダ1件パース
//　　　　　　:　readSyougoDtlArray ...................... 照合明細配列パース
//　　　　　　:　readSyougoDtl ........................... 照合明細1件パース
//　　　　　　:　safeParseInt ............................ int安全変換
//　　　　　　:　safeParseBool ........................... boolean安全変換
//　　　　　　:　safeParseDate ........................... Date安全変換
//============================================================
public class SoapParsers {

    //================================================================
    //　機　能　:　SoapParsersの生成を禁止する（ユーティリティクラス化）
    //　引　数　:　なし
    //　戻り値　:　[SoapParsers] ..... なし
    //================================================================
    private SoapParsers() {
        // static専用クラスのためインスタンス化させない
    }

    //================================================================
    //　機　能　:　XmlPullParserを生成する（namespace aware）
    //　引　数　:　xml ..... String
    //　戻り値　:　[XmlPullParser] ..... パーサ
    //================================================================
    private static XmlPullParser newParser(String xml) throws Exception {
        XmlPullParserFactory f = XmlPullParserFactory.newInstance();
        f.setNamespaceAware(true);

        XmlPullParser p = f.newPullParser();
        p.setInput(new StringReader(xml));
        return p;
    }

    //================================================================
    //　機　能　:　SOAP Faultを検出した場合に例外を送出する
    //　引　数　:　responseXml ..... String
    //　戻り値　:　[void] ..... なし
    //================================================================
    public static void throwIfSoapFault(String responseXml) throws Exception {
        XmlPullParser p = newParser(responseXml);

        int e = p.getEventType();
        while (e != XmlPullParser.END_DOCUMENT) {

            // <Fault> を検出したら faultstring を取りに行く
            if (e == XmlPullParser.START_TAG && "Fault".equals(p.getName())) {
                String faultString = null;
                int depth = p.getDepth();

                // Fault要素の範囲内を走査して faultstring を取得する
                while (!(e == XmlPullParser.END_TAG
                        && p.getDepth() == depth
                        && "Fault".equals(p.getName()))) {

                    e = p.next();

                    if (e == XmlPullParser.START_TAG && "faultstring".equals(p.getName())) {
                        faultString = p.nextText();
                    }
                }

                // Faultを例外として扱う（レスポンスXMLも保持）
                throw new SoapFaultException(
                        faultString != null ? faultString : "SOAP Fault",
                        responseXml
                );
            }

            e = p.next();
        }
    }

    //================================================================
    //　機　能　:　boolean結果（Resultタグ内）を解析する
    //　引　数　:　responseXml ..... String
    //　　　　　:　resultTagName ..... String
    //　戻り値　:　[boolean] ..... 解析結果
    //================================================================
    public static boolean parseBooleanResult(String responseXml, String resultTagName) throws Exception {
        String t = parseTextResult(responseXml, resultTagName);

        // "true"/"false" を想定（余分な空白は除去）
        return t != null && "true".equalsIgnoreCase(t.trim());
    }

    //================================================================
    //　機　能　:　テキスト結果（Resultタグ内）を解析する
    //　引　数　:　responseXml ..... String
    //　　　　　:　resultTagName ..... String
    //　戻り値　:　[String] ..... 解析結果テキスト
    //================================================================
    public static String parseTextResult(String responseXml, String resultTagName) throws Exception {
        XmlPullParser p = newParser(responseXml);

        int e = p.getEventType();
        while (e != XmlPullParser.END_DOCUMENT) {
            if (e == XmlPullParser.START_TAG && resultTagName.equals(p.getName())) {
                return p.nextText();
            }
            e = p.next();
        }

        // 結果タグが無い場合は異常
        throw new IllegalArgumentException("Result tag not found: " + resultTagName);
    }

    //================================================================
    //　機　能　:　xsd:dateTime結果（Resultタグ内）を解析する
    //　引　数　:　responseXml ..... String
    //　　　　　:　resultTagName ..... String
    //　戻り値　:　[Date] ..... 解析結果
    //================================================================
    public static Date parseDateTimeResult(String responseXml, String resultTagName) throws Exception {
        String t = parseTextResult(responseXml, resultTagName);
        try {
            return XsdDateTime.parse(t);
        } catch (ParseException pe) {
            throw new IllegalArgumentException("Failed to parse xsd:dateTime: " + t, pe);
        }
    }

    //================================================================
    //　機　能　:　string配列結果（Resultタグ内の<string>要素）を解析する
    //　引　数　:　responseXml ..... String
    //　　　　　:　resultTagName ..... String
    //　戻り値　:　[String[]] ..... 解析結果
    //================================================================
    public static String[] parseStringArrayResult(String responseXml, String resultTagName) throws Exception {
        XmlPullParser p = newParser(responseXml);

        List<String> values = new ArrayList<>();
        int e = p.getEventType();

        boolean inResult = false;
        int resultDepth = 0;

        while (e != XmlPullParser.END_DOCUMENT) {

            if (e == XmlPullParser.START_TAG) {
                String name = p.getName();

                // Result要素に入ったら範囲を記録
                if (resultTagName.equals(name)) {
                    inResult = true;
                    resultDepth = p.getDepth();

                    // Result配下の <string> を拾う
                } else if (inResult && "string".equals(name)) {
                    values.add(p.nextText());
                }

            } else if (e == XmlPullParser.END_TAG
                    && inResult
                    && p.getDepth() == resultDepth
                    && resultTagName.equals(p.getName())) {
                // Result要素を抜けたら終了
                break;
            }

            e = p.next();
        }

        return values.toArray(new String[0]);
    }

    //================================================================
    //　機　能　:　base64Binary結果（Resultタグ内）を解析してbyte配列に変換する
    //　引　数　:　responseXml ..... String
    //　　　　　:　resultTagName ..... String
    //　戻り値　:　[byte[]] ..... 解析結果（空の場合は0バイト配列）
    //================================================================
    public static byte[] parseBase64Result(String responseXml, String resultTagName) throws Exception {
        String t = parseTextResult(responseXml, resultTagName);

        // null/空白は空配列
        if (t == null) {
            return new byte[0];
        }
        String trimmed = t.trim();
        if (trimmed.isEmpty()) {
            return new byte[0];
        }

        // Base64文字列をデコード
        return Base64.decode(trimmed, Base64.DEFAULT);
    }

    //================================================================
    //　機　能　:　GetSyukkaDataResult を SyukkaData として完全パースする
    //　引　数　:　responseXml ..... String
    //　戻り値　:　[SyukkaData] ..... 出荷データ
    //================================================================
    public static SyukkaData parseSyukkaDataResult(String responseXml) throws Exception {
        XmlPullParser p = newParser(responseXml);

        // <GetSyukkaDataResult> の中に <Header> と <Meisai> が来る想定で読む
        SyukkaData data = new SyukkaData();

        int e = p.getEventType();
        boolean inResult = false;

        while (e != XmlPullParser.END_DOCUMENT) {

            if (e == XmlPullParser.START_TAG) {
                String name = p.getName();

                if ("GetSyukkaDataResult".equals(name)) {
                    inResult = true;

                } else if (inResult && "Header".equals(name)) {
                    // <Header>配下の配列を読み取る（読み取り後は</Header>位置まで進む）
                    data.header = readSyukkaHeaderArray(p);

                } else if (inResult && "Meisai".equals(name)) {
                    // <Meisai>配下の配列を読み取る
                    data.meisai = readSyukkaMeisaiArray(p);
                }

            } else if (e == XmlPullParser.END_TAG && "GetSyukkaDataResult".equals(p.getName())) {
                // Result終了で抜ける
                break;
            }

            e = p.next();
        }

        return data;
    }

    //================================================================
    //　機　能　:　出荷ヘッダ配列（<Header>配下）を読み取る
    //　引　数　:　p ..... XmlPullParser（<Header>のSTART_TAG上）
    //　戻り値　:　[List<SyukkaHeader>] ..... ヘッダ一覧
    //================================================================
    private static List<SyukkaHeader> readSyukkaHeaderArray(XmlPullParser p) throws Exception {
        List<SyukkaHeader> list = new ArrayList<>();
        int depth = p.getDepth();

        int e = p.next();
        while (!(e == XmlPullParser.END_TAG
                && p.getDepth() == depth
                && "Header".equals(p.getName()))) {

            if (e == XmlPullParser.START_TAG && "SyukkaHeader".equals(p.getName())) {
                list.add(readSyukkaHeader(p));
            }

            e = p.next();
        }

        return list;
    }

    //================================================================
    //　機　能　:　出荷ヘッダ1件（<SyukkaHeader>）を読み取る
    //　引　数　:　p ..... XmlPullParser（<SyukkaHeader>のSTART_TAG上）
    //　戻り値　:　[SyukkaHeader] ..... ヘッダ1件
    //================================================================
    private static SyukkaHeader readSyukkaHeader(XmlPullParser p) throws Exception {
        SyukkaHeader h = new SyukkaHeader();
        int depth = p.getDepth();

        int e = p.next();
        while (!(e == XmlPullParser.END_TAG
                && p.getDepth() == depth
                && "SyukkaHeader".equals(p.getName()))) {

            if (e == XmlPullParser.START_TAG) {
                String n = p.getName();
                String t = p.nextText();

                if ("BookingNo".equals(n)) h.bookingNo = t;
                else if ("SyukkaYmd".equals(n)) h.syukkaYmd = safeParseDate(t);
                else if ("ContainerCount".equals(n)) h.containerCount = safeParseInt(t);
                else if ("TotalBundole".equals(n)) h.totalBundole = safeParseInt(t);
                else if ("TotalJyuryo".equals(n)) h.totalJyuryo = safeParseInt(t);
                else if ("KanryoContainerCnt".equals(n)) h.kanryoContainerCnt = safeParseInt(t);
                else if ("KanryoBundleSum".equals(n)) h.kanryoBundleSum = safeParseInt(t);
                else if ("KnaryoJyuryoSum".equals(n)) h.knaryoJyuryoSum = safeParseInt(t);
                else if ("LastUpdYmdHms".equals(n)) h.lastUpdYmdHms = safeParseDate(t);
            }

            e = p.next();
        }

        return h;
    }

    //================================================================
    //　機　能　:　出荷明細配列（<Meisai>配下）を読み取る
    //　引　数　:　p ..... XmlPullParser（<Meisai>のSTART_TAG上）
    //　戻り値　:　[List<SyukkaMeisai>] ..... 明細一覧
    //================================================================
    private static List<SyukkaMeisai> readSyukkaMeisaiArray(XmlPullParser p) throws Exception {
        List<SyukkaMeisai> list = new ArrayList<>();
        int depth = p.getDepth();

        int e = p.next();
        while (!(e == XmlPullParser.END_TAG
                && p.getDepth() == depth
                && "Meisai".equals(p.getName()))) {

            if (e == XmlPullParser.START_TAG && "SyukkaMeisai".equals(p.getName())) {
                list.add(readSyukkaMeisai(p));
            }

            e = p.next();
        }

        return list;
    }

    //================================================================
    //　機　能　:　出荷明細1件（<SyukkaMeisai>）を読み取る
    //　引　数　:　p ..... XmlPullParser（<SyukkaMeisai>のSTART_TAG上）
    //　戻り値　:　[SyukkaMeisai] ..... 明細1件
    //================================================================
    private static SyukkaMeisai readSyukkaMeisai(XmlPullParser p) throws Exception {
        SyukkaMeisai m = new SyukkaMeisai();
        int depth = p.getDepth();

        int e = p.next();
        while (!(e == XmlPullParser.END_TAG
                && p.getDepth() == depth
                && "SyukkaMeisai".equals(p.getName()))) {

            if (e == XmlPullParser.START_TAG) {
                String n = p.getName();
                String t = p.nextText();

                if ("HeatNo".equals(n)) m.heatNo = t;
                else if ("Sokuban".equals(n)) m.sokuban = t;
                else if ("SyukkaSashizuNo".equals(n)) m.syukkaSashizuNo = t;
                else if ("bundleNo".equals(n)) m.bundleNo = t; // ★先頭小文字
                else if ("Jyuryo".equals(n)) m.jyuryo = safeParseInt(t);
                else if ("BookingNo".equals(n)) m.bookingNo = t;
            }

            e = p.next();
        }

        return m;
    }

    //================================================================
    //　機　能　:　GetSyougoDataResult を SyougoData として完全パースする
    //　引　数　:　responseXml ..... String
    //　戻り値　:　[SyougoData] ..... 照合データ
    //================================================================
    public static SyougoData parseSyougoDataResult(String responseXml) throws Exception {
        XmlPullParser p = newParser(responseXml);
        SyougoData data = new SyougoData();

        int e = p.getEventType();
        boolean inResult = false;

        while (e != XmlPullParser.END_DOCUMENT) {

            if (e == XmlPullParser.START_TAG) {
                String name = p.getName();

                if ("GetSyougoDataResult".equals(name)) {
                    inResult = true;

                } else if (inResult && "syougoHeader".equals(name)) {
                    data.syougoHeader = readSyougoHeaderArray(p);

                } else if (inResult && "syogoDtl".equals(name)) {
                    data.syogoDtl = readSyougoDtlArray(p);
                }

            } else if (e == XmlPullParser.END_TAG && "GetSyougoDataResult".equals(p.getName())) {
                break;
            }

            e = p.next();
        }

        return data;
    }

    //================================================================
    //　機　能　:　照合ヘッダ配列（<syougoHeader>配下）を読み取る
    //　引　数　:　p ..... XmlPullParser（<syougoHeader>のSTART_TAG上）
    //　戻り値　:　[List<SyougoHeader>] ..... ヘッダ一覧
    //================================================================
    private static List<SyougoHeader> readSyougoHeaderArray(XmlPullParser p) throws Exception {
        List<SyougoHeader> list = new ArrayList<>();
        int depth = p.getDepth();

        int e = p.next();
        while (!(e == XmlPullParser.END_TAG
                && p.getDepth() == depth
                && "syougoHeader".equals(p.getName()))) {

            if (e == XmlPullParser.START_TAG && "SyougoHeader".equals(p.getName())) {
                list.add(readSyougoHeader(p));
            }

            e = p.next();
        }

        return list;
    }

    //================================================================
    //　機　能　:　照合ヘッダ1件（<SyougoHeader>）を読み取る
    //　引　数　:　p ..... XmlPullParser（<SyougoHeader>のSTART_TAG上）
    //　戻り値　:　[SyougoHeader] ..... ヘッダ1件
    //================================================================
    private static SyougoHeader readSyougoHeader(XmlPullParser p) throws Exception {
        SyougoHeader h = new SyougoHeader();
        int depth = p.getDepth();

        int e = p.next();
        while (!(e == XmlPullParser.END_TAG
                && p.getDepth() == depth
                && "SyougoHeader".equals(p.getName()))) {

            if (e == XmlPullParser.START_TAG) {
                String n = p.getName();
                String t = p.nextText();

                if ("containerID".equals(n)) h.containerID = t;
                else if ("containerNo".equals(n)) h.containerNo = t;
                else if ("bundleCnt".equals(n)) h.bundleCnt = safeParseInt(t);
                else if ("sagyouYMD".equals(n)) h.sagyouYMD = safeParseDate(t);
                else if ("syogoKanryo".equals(n)) h.syogoKanryo = safeParseBool(t);
            }

            e = p.next();
        }

        return h;
    }

    //================================================================
    //　機　能　:　照合明細配列（<syogoDtl>配下）を読み取る
    //　引　数　:　p ..... XmlPullParser（<syogoDtl>のSTART_TAG上）
    //　戻り値　:　[List<SyougoDtl>] ..... 明細一覧
    //================================================================
    private static List<SyougoDtl> readSyougoDtlArray(XmlPullParser p) throws Exception {
        List<SyougoDtl> list = new ArrayList<>();
        int depth = p.getDepth();

        int e = p.next();
        while (!(e == XmlPullParser.END_TAG
                && p.getDepth() == depth
                && "syogoDtl".equals(p.getName()))) {

            if (e == XmlPullParser.START_TAG && "SyougoDtl".equals(p.getName())) {
                list.add(readSyougoDtl(p));
            }

            e = p.next();
        }

        return list;
    }

    //================================================================
    //　機　能　:　照合明細1件（<SyougoDtl>）を読み取る
    //　引　数　:　p ..... XmlPullParser（<SyougoDtl>のSTART_TAG上）
    //　戻り値　:　[SyougoDtl] ..... 明細1件
    //================================================================
    private static SyougoDtl readSyougoDtl(XmlPullParser p) throws Exception {
        SyougoDtl d = new SyougoDtl();
        int depth = p.getDepth();

        int e = p.next();
        while (!(e == XmlPullParser.END_TAG
                && p.getDepth() == depth
                && "SyougoDtl".equals(p.getName()))) {

            if (e == XmlPullParser.START_TAG) {
                String n = p.getName();
                String t = p.nextText();

                if ("syogoDtlheatNo".equals(n)) d.syogoDtlheatNo = t;
                else if ("syogoDtlsokuban".equals(n)) d.syogoDtlsokuban = t;
                else if ("syougoDtlsyukkaSashizuNo".equals(n)) d.syougoDtlsyukkaSashizuNo = t;
                else if ("syougoDtlbundleNo".equals(n)) d.syougoDtlbundleNo = t;
                else if ("syougoDtljyuryo".equals(n)) d.syougoDtljyuryo = safeParseInt(t);
                else if ("syougoDtlcontainerID".equals(n)) d.syougoDtlcontainerID = t;
                else if ("syougoDtlsyougoKakunin".equals(n))
                    d.syougoDtlsyougoKakunin = safeParseBool(t);
            }

            e = p.next();
        }

        return d;
    }

    //================================================================
    //　機　能　:　数値文字列をintへ安全に変換する（失敗時は0）
    //　引　数　:　t ..... String
    //　戻り値　:　[int] ..... 変換結果
    //================================================================
    private static int safeParseInt(String t) {
        try {
            return Integer.parseInt(t.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    //================================================================
    //　機　能　:　真偽値文字列をbooleanへ安全に変換する（null/不正はfalse）
    //　引　数　:　t ..... String
    //　戻り値　:　[boolean] ..... 変換結果
    //================================================================
    private static boolean safeParseBool(String t) {
        if (t == null) {
            return false;
        }
        String s = t.trim();
        return "true".equalsIgnoreCase(s) || "1".equals(s);
    }

    //================================================================
    //　機　能　:　xsd:dateTime文字列をDateへ安全に変換する（失敗時はnull）
    //　引　数　:　t ..... String
    //　戻り値　:　[Date] ..... 変換結果
    //================================================================
    private static Date safeParseDate(String t) {
        try {
            return XsdDateTime.parse(t);
        } catch (Exception e) {
            return null;
        }
    }
}
