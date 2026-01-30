package com.example.myapplication.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


//============================================================
//　処理概要　:　BunningDataクラス
//============================================================

public class BunningData {
    public Date syukkaYmd;
    public String containerNo;
    public int containerJyuryo;
    public int dunnageJyuryo;
    public String sealNo;

    public byte[] containerPhoto; // optional
    public byte[] sealPhoto;      // optional

    public List<SyukkaMeisai> bundles = new ArrayList<>();
}

