package com.example.myapplication.connector;

import com.example.myapplication.model.BunningData;
import com.example.myapplication.model.CollateData;
import com.example.myapplication.model.SyougoData;
import com.example.myapplication.model.SyukkaData;

import java.io.Closeable;
import java.util.Date;


//================================
//　処理概要　:　SvcHandyWrapperクラス
//================================

public class SvcHandyWrapper implements Closeable {
    private final SvcHandyRepository repository;
    //=======================================
    //　機　能　:　SvcHandyWrapperの初期化処理
    //　引　数　:　なし
    //　戻り値　:　[SvcHandyWrapper] ..... なし
    //=======================================

    public SvcHandyWrapper() {
        this(new SvcHandyRepository());
    }
    //================================================
    //　機　能　:　SvcHandyWrapperの初期化処理
    //　引　数　:　repository ..... SvcHandyRepository
    //　戻り値　:　[SvcHandyWrapper] ..... なし
    //================================================

    public SvcHandyWrapper(SvcHandyRepository repository) {
        this.repository = repository;
    }
    //============================
    //　機　能　:　sagyou Ymdを取得する
    //　引　数　:　なし
    //　戻り値　:　[Date] ..... なし
    //============================

    public Date getSagyouYmd() throws Exception {
        return repository.getSagyouYmd();
    }
    //=================================
    //　機　能　:　update Ymd Hmsを取得する
    //　引　数　:　sagyouYmd ..... Date
    //　戻り値　:　[Date] ..... なし
    //=================================

    public Date getUpdateYmdHms(Date sagyouYmd) throws Exception {
        return repository.getUpdateYmdHms(sagyouYmd);
    }
    //==================================
    //　機　能　:　syukka Dataを取得する
    //　引　数　:　sagyouYmd ..... Date
    //　戻り値　:　[SyukkaData] ..... なし
    //==================================

    public SyukkaData getSyukkaData(Date sagyouYmd) throws Exception {
        return repository.getSyukkaData(sagyouYmd);
    }

    //===================================
    //　機　能　:　syukka Dataを送信する
    //　引　数　:　data ..... BunningData
    //　戻り値　:　[boolean] ..... なし
    //===================================
    public boolean sendSyukkaData(BunningData data) throws Exception {
        return repository.sendSyukkaData(data);
    }

    //==================================
    //　機　能　:　syougo Dataを取得する
    //　引　数　:　なし
    //　戻り値　:　[SyougoData] ..... なし
    //==================================
    public SyougoData getSyougoData() throws Exception {
        return repository.getSyougoData();
    }

    //===================================
    //　機　能　:　syougo Dataを送信する
    //　引　数　:　data ..... CollateData
    //　戻り値　:　[boolean] ..... なし
    //===================================
    public boolean sendSyougoData(CollateData data) throws Exception {
        return repository.sendSyougoData(data);
    }

    //====================================
    //　機　能　:　upload Binary Fileを送信する
    //　引　数　:　fileName ..... String
    //　　　　　:　buffer ..... byte[]
    //　戻り値　:　[boolean] ..... なし
    //====================================
    public boolean uploadBinaryFile(String fileName, byte[] buffer) throws Exception {
        return repository.uploadBinaryFile(fileName, buffer);
    }

    //==================================================
    //　機　能　:　download Handy Execute File Namesを取得する
    //　引　数　:　なし
    //　戻り値　:　[String[]] ..... なし
    //==================================================
    public String[] getDownloadHandyExecuteFileNames() throws Exception {
        return repository.getDownloadHandyExecuteFileNames();
    }

    //===============================================
    //　機　能　:　download Handy Execute Fileを取得する
    //　引　数　:　fileName ..... String
    //　戻り値　:　[byte[]] ..... なし
    //===============================================
    public byte[] getDownloadHandyExecuteFile(String fileName) throws Exception {
        return repository.getDownloadHandyExecuteFile(fileName);
    }

    //============================
    //　機　能　:　closeの処理
    //　引　数　:　なし
    //　戻り値　:　[void] ..... なし
    //============================
    @Override
    public void close() {
        // no-op (placeholder for future resources)
    }
}
