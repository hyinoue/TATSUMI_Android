package com.example.myapplication.connector;

import android.util.Base64;

import com.example.myapplication.model.BunningData;
import com.example.myapplication.model.SyukkaMeisai;
import com.example.myapplication.time.XmlUtil;

public class SendSyukkaSoapBuilder {
    private static final String NS = "http://tempuri.org/";

    private SendSyukkaSoapBuilder() {
    }

    public static String buildSendSyukkaData(BunningData data) {
        StringBuilder inner = new StringBuilder();

        inner.append("<SendSyukkaData xmlns=\"").append(NS).append("\">");
        inner.append("<data>");

        // reference.cs のプロパティ名（大小文字含む）に合わせる
        XmlUtil.tag(inner, "SyukkaYmd", XmlUtil.toXsdDateTime(data.syukkaYmd));
        XmlUtil.tag(inner, "ContainerNo", data.containerNo);
        XmlUtil.tag(inner, "ContainerJyuryo", String.valueOf(data.containerJyuryo));
        XmlUtil.tag(inner, "DunnageJyuryo", String.valueOf(data.dunnageJyuryo));
        XmlUtil.tag(inner, "SealNo", data.sealNo);

        // 配列：Bundles（親）→ SyukkaMeisai（子）
        inner.append("<Bundles>");
        for (SyukkaMeisai b : data.bundles) {
            inner.append("<SyukkaMeisai>");
            XmlUtil.tag(inner, "HeatNo", b.heatNo);
            XmlUtil.tag(inner, "Sokuban", b.sokuban);
            XmlUtil.tag(inner, "SyukkaSashizuNo", b.syukkaSashizuNo);
            XmlUtil.tag(inner, "bundleNo", b.bundleNo); // ★先頭小文字
            XmlUtil.tag(inner, "Jyuryo", String.valueOf(b.jyuryo));
            XmlUtil.tag(inner, "BookingNo", b.bookingNo);
            inner.append("</SyukkaMeisai>");
        }
        inner.append("</Bundles>");

        // base64Binary（必要時のみ）
        if (data.containerPhoto != null && data.containerPhoto.length > 0) {
            String b64 = Base64.encodeToString(data.containerPhoto, Base64.NO_WRAP);
            XmlUtil.tagRaw(inner, "ContainerPhoto", b64);
        }
        if (data.sealPhoto != null && data.sealPhoto.length > 0) {
            String b64 = Base64.encodeToString(data.sealPhoto, Base64.NO_WRAP);
            XmlUtil.tagRaw(inner, "SealPhoto", b64);
        }

        inner.append("</data>");
        inner.append("</SendSyukkaData>");

        return SoapEnvelope.wrapBody(inner.toString());
    }
}
