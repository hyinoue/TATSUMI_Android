package com.example.myapplication.connector;

public class SoapFaultException extends Exception {
    private final String rawXml;

    public SoapFaultException(String message, String rawXml) {
        super(message);
        this.rawXml = rawXml;
    }

    public String getRawXml() {
        return rawXml;
    }
}

