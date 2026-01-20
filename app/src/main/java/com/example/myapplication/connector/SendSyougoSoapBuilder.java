package com.example.myapplication.connector;

import com.example.myapplication.model.CollateData;
import com.example.myapplication.model.CollateDtl;

public class SendSyougoSoapBuilder {
    private static final String NS = "http://tempuri.org/";

    private SendSyougoSoapBuilder() {
    }

    public static String buildSendSyougoData(CollateData data) {
        StringBuilder inner = new StringBuilder();

        inner.append("<SendSyougoData xmlns=\"").append(NS).append("\">");
        inner.append("<data>");

        // reference.cs：containerID / syogoKanryo（どちらも先頭小文字）
        XmlUtil.tag(inner, "containerID", data.containerID);
        XmlUtil.tag(inner, "syogoKanryo", String.valueOf(data.syogoKanryo));

        // 配列：CollateDtls（親：先頭大文字）→ CollateDtl（子）
        inner.append("<CollateDtls>");
        for (CollateDtl d : data.collateDtls) {
            inner.append("<CollateDtl>");
            XmlUtil.tag(inner, "collateDtlheatNo", d.collateDtlheatNo);
            XmlUtil.tag(inner, "collateDtlsokuban", d.collateDtlsokuban);
            XmlUtil.tag(inner, "collateDtlsyougoKakunin", String.valueOf(d.collateDtlsyougoKakunin));
            inner.append("</CollateDtl>");
        }
        inner.append("</CollateDtls>");
        inner.append("</data>");
        inner.append("</SendSyougoData>");

        return SoapEnvelope.wrapBody(inner.toString());
    }
}
