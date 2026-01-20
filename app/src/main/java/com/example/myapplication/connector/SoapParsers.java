package com.example.myapplication.connector;

import com.example.myapplication.model.SyougoData;
import com.example.myapplication.model.SyougoDtl;
import com.example.myapplication.model.SyougoHeader;
import com.example.myapplication.model.SyukkaData;
import com.example.myapplication.model.SyukkaHeader;
import com.example.myapplication.model.SyukkaMeisai;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SoapParsers {

    private SoapParsers() {
    }

    private static XmlPullParser newParser(String xml) throws Exception {
        XmlPullParserFactory f = XmlPullParserFactory.newInstance();
        f.setNamespaceAware(true);
        XmlPullParser p = f.newPullParser();
        p.setInput(new StringReader(xml));
        return p;
    }

    public static void throwIfSoapFault(String responseXml) throws Exception {
        XmlPullParser p = newParser(responseXml);

        int e = p.getEventType();
        while (e != XmlPullParser.END_DOCUMENT) {
            if (e == XmlPullParser.START_TAG && "Fault".equals(p.getName())) {
                String faultString = null;
                int depth = p.getDepth();
                while (!(e == XmlPullParser.END_TAG && p.getDepth() == depth && "Fault".equals(p.getName()))) {
                    e = p.next();
                    if (e == XmlPullParser.START_TAG && "faultstring".equals(p.getName())) {
                        faultString = p.nextText();
                    }
                }
                throw new SoapFaultException(faultString != null ? faultString : "SOAP Fault", responseXml);
            }
            e = p.next();
        }
    }

    public static boolean parseBooleanResult(String responseXml, String resultTagName) throws Exception {
        XmlPullParser p = newParser(responseXml);
        int e = p.getEventType();
        while (e != XmlPullParser.END_DOCUMENT) {
            if (e == XmlPullParser.START_TAG && resultTagName.equals(p.getName())) {
                String t = p.nextText();
                return "true".equalsIgnoreCase(t.trim());
            }
            e = p.next();
        }
        throw new IllegalArgumentException("Result tag not found: " + resultTagName);
    }

    public static String parseTextResult(String responseXml, String resultTagName) throws Exception {
        XmlPullParser p = newParser(responseXml);
        int e = p.getEventType();
        while (e != XmlPullParser.END_DOCUMENT) {
            if (e == XmlPullParser.START_TAG && resultTagName.equals(p.getName())) {
                return p.nextText();
            }
            e = p.next();
        }
        throw new IllegalArgumentException("Result tag not found: " + resultTagName);
    }

    public static Date parseDateTimeResult(String responseXml, String resultTagName) throws Exception {
        String t = parseTextResult(responseXml, resultTagName);
        try {
            return XsdDateTime.parse(t);
        } catch (ParseException pe) {
            throw new IllegalArgumentException("Failed to parse xsd:dateTime: " + t, pe);
        }
    }

    // -------------------------
    // GetSyukkaDataResult → SyukkaData 完全パース
    // reference.cs:
    //   SyukkaData { SyukkaHeader[] Header; SyukkaMeisai[] Meisai; }
    //   SyukkaHeader: BookingNo, SyukkaYmd, ContainerCount, TotalBundole, TotalJyuryo,
    //                KanryoContainerCnt, KanryoBundleSum, KnaryoJyuryoSum, LastUpdYmdHms
    //   SyukkaMeisai: HeatNo, Sokuban, SyukkaSashizuNo, bundleNo(小文字), Jyuryo, BookingNo
    // -------------------------
    public static SyukkaData parseSyukkaDataResult(String responseXml) throws Exception {
        XmlPullParser p = newParser(responseXml);

        // <GetSyukkaDataResult> の中に <SyukkaData>（実際は型名が出ないこともある）
        // → ここでは <Header> と <Meisai> を見つけて読む方式にする（ASMXの実体に強い）
        SyukkaData data = new SyukkaData();

        int e = p.getEventType();
        boolean inResult = false;

        while (e != XmlPullParser.END_DOCUMENT) {
            if (e == XmlPullParser.START_TAG) {
                String name = p.getName();
                if ("GetSyukkaDataResult".equals(name)) {
                    inResult = true;
                } else if (inResult && "Header".equals(name)) {
                    data.header = readSyukkaHeaderArray(p); // pはHeader開始位置
                } else if (inResult && "Meisai".equals(name)) {
                    data.meisai = readSyukkaMeisaiArray(p); // pはMeisai開始位置
                }
            } else if (e == XmlPullParser.END_TAG && "GetSyukkaDataResult".equals(p.getName())) {
                break;
            }
            e = p.next();
        }

        return data;
    }

    private static List<SyukkaHeader> readSyukkaHeaderArray(XmlPullParser p) throws Exception {
        // ここに来た時点で <Header> の START_TAG 上
        List<SyukkaHeader> list = new ArrayList<>();
        int depth = p.getDepth();

        int e = p.next();
        while (!(e == XmlPullParser.END_TAG && p.getDepth() == depth && "Header".equals(p.getName()))) {
            if (e == XmlPullParser.START_TAG && "SyukkaHeader".equals(p.getName())) {
                list.add(readSyukkaHeader(p));
            }
            e = p.next();
        }
        return list;
    }

    private static SyukkaHeader readSyukkaHeader(XmlPullParser p) throws Exception {
        // <SyukkaHeader> START_TAG 上
        SyukkaHeader h = new SyukkaHeader();
        int depth = p.getDepth();

        int e = p.next();
        while (!(e == XmlPullParser.END_TAG && p.getDepth() == depth && "SyukkaHeader".equals(p.getName()))) {
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

    private static List<SyukkaMeisai> readSyukkaMeisaiArray(XmlPullParser p) throws Exception {
        // <Meisai> START_TAG 上
        List<SyukkaMeisai> list = new ArrayList<>();
        int depth = p.getDepth();

        int e = p.next();
        while (!(e == XmlPullParser.END_TAG && p.getDepth() == depth && "Meisai".equals(p.getName()))) {
            if (e == XmlPullParser.START_TAG && "SyukkaMeisai".equals(p.getName())) {
                list.add(readSyukkaMeisai(p));
            }
            e = p.next();
        }
        return list;
    }

    private static SyukkaMeisai readSyukkaMeisai(XmlPullParser p) throws Exception {
        // <SyukkaMeisai> START_TAG 上
        SyukkaMeisai m = new SyukkaMeisai();
        int depth = p.getDepth();

        int e = p.next();
        while (!(e == XmlPullParser.END_TAG && p.getDepth() == depth && "SyukkaMeisai".equals(p.getName()))) {
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

    // -------------------------
    // GetSyougoDataResult → SyougoData 完全パース
    // reference.cs:
    //   SyougoData { SyougoHeader[] syougoHeader; SyougoDtl[] syogoDtl; }
    //   SyougoHeader: containerID, containerNo, bundleCnt, sagyouYMD, syogoKanryo
    //   SyougoDtl: syogoDtlheatNo, syogoDtlsokuban, syougoDtlsyukkaSashizuNo, syougoDtlbundleNo,
    //             syougoDtljyuryo, syougoDtlcontainerID, syougoDtlsyougoKakunin
    // -------------------------
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

    private static List<SyougoHeader> readSyougoHeaderArray(XmlPullParser p) throws Exception {
        // <syougoHeader> START_TAG
        List<SyougoHeader> list = new ArrayList<>();
        int depth = p.getDepth();

        int e = p.next();
        while (!(e == XmlPullParser.END_TAG && p.getDepth() == depth && "syougoHeader".equals(p.getName()))) {
            if (e == XmlPullParser.START_TAG && "SyougoHeader".equals(p.getName())) {
                list.add(readSyougoHeader(p));
            }
            e = p.next();
        }
        return list;
    }

    private static SyougoHeader readSyougoHeader(XmlPullParser p) throws Exception {
        // <SyougoHeader>
        SyougoHeader h = new SyougoHeader();
        int depth = p.getDepth();

        int e = p.next();
        while (!(e == XmlPullParser.END_TAG && p.getDepth() == depth && "SyougoHeader".equals(p.getName()))) {
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

    private static List<SyougoDtl> readSyougoDtlArray(XmlPullParser p) throws Exception {
        // <syogoDtl> START_TAG
        List<SyougoDtl> list = new ArrayList<>();
        int depth = p.getDepth();

        int e = p.next();
        while (!(e == XmlPullParser.END_TAG && p.getDepth() == depth && "syogoDtl".equals(p.getName()))) {
            if (e == XmlPullParser.START_TAG && "SyougoDtl".equals(p.getName())) {
                list.add(readSyougoDtl(p));
            }
            e = p.next();
        }
        return list;
    }

    private static SyougoDtl readSyougoDtl(XmlPullParser p) throws Exception {
        // <SyougoDtl>
        SyougoDtl d = new SyougoDtl();
        int depth = p.getDepth();

        int e = p.next();
        while (!(e == XmlPullParser.END_TAG && p.getDepth() == depth && "SyougoDtl".equals(p.getName()))) {
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

    // ------- helpers -------
    private static int safeParseInt(String t) {
        try {
            return Integer.parseInt(t.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private static boolean safeParseBool(String t) {
        if (t == null) return false;
        String s = t.trim();
        return "true".equalsIgnoreCase(s) || "1".equals(s);
    }

    private static Date safeParseDate(String t) {
        try {
            return XsdDateTime.parse(t);
        } catch (Exception e) {
            return null;
        }
    }
}

