package com.example.myapplication.model;

import java.util.ArrayList;
import java.util.List;

//============================================================
//　処理概要　:　SyougoDataクラス
//　機　能　　:　照合データ（ヘッダ／明細）を保持するデータモデル
//============================================================
public class SyougoData {

    //================================================================
    //　項　目　:　syougoHeader
    //　内　容　:　照合ヘッダ情報一覧
    //================================================================
    public List<SyougoHeader> syougoHeader = new ArrayList<>();

    //================================================================
    //　項　目　:　syogoDtl
    //　内　容　:　照合明細情報一覧
    //================================================================
    public List<SyougoDtl> syogoDtl = new ArrayList<>();
}