package com.example.myapplication.connector;

import com.example.myapplication.model.BunningData;
import com.example.myapplication.model.CollateData;
import com.example.myapplication.model.SyougoData;
import com.example.myapplication.model.SyukkaData;

import java.io.Closeable;
import java.util.Date;


//============================================================
//　処理概要　:　SvcHandyWrapperクラス
//============================================================

public class SvcHandyWrapper implements Closeable {
    private final SvcHandyRepository repository;

    public SvcHandyWrapper() {
        this(new SvcHandyRepository());
    }

    public SvcHandyWrapper(SvcHandyRepository repository) {
        this.repository = repository;
    }

    public Date getSagyouYmd() throws Exception {
        return repository.getSagyouYmd();
    }

    public Date getUpdateYmdHms(Date sagyouYmd) throws Exception {
        return repository.getUpdateYmdHms(sagyouYmd);
    }

    public SyukkaData getSyukkaData(Date sagyouYmd) throws Exception {
        return repository.getSyukkaData(sagyouYmd);
    }

    public boolean sendSyukkaData(BunningData data) throws Exception {
        return repository.sendSyukkaData(data);
    }

    public SyougoData getSyougoData() throws Exception {
        return repository.getSyougoData();
    }

    public boolean sendSyougoData(CollateData data) throws Exception {
        return repository.sendSyougoData(data);
    }

    @Override
    public void close() {
        // no-op (placeholder for future resources)
    }
}
