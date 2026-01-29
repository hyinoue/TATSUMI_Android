package com.example.myapplication.grid;

public class VanningCollationRow {
    public final String pNo;
    public final String bNo;
    public final String index;
    public final String jyuryo;
    public final String confirmed;

    public VanningCollationRow(String pNo, String bNo, String index, String jyuryo, String confirmed) {
        this.pNo = pNo;
        this.bNo = bNo;
        this.index = index;
        this.jyuryo = jyuryo;
        this.confirmed = confirmed;
    }
}