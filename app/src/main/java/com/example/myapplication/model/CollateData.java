package com.example.myapplication.model;

import java.util.ArrayList;
import java.util.List;


//======================
//　処理概要　:　CollateDataクラス
//======================

public class CollateData {
    public String containerID;     // ★要素名 containerID（先頭小文字）
    public boolean syogoKanryo;    // ★要素名 syogoKanryo（先頭小文字）

    // ★要素名 CollateDtls（先頭大文字C）
    public List<CollateDtl> collateDtls = new ArrayList<>();
}

