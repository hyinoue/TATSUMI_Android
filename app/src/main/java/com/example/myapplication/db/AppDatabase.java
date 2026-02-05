package com.example.myapplication.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.myapplication.db.dao.CommHistoryDao;
import com.example.myapplication.db.dao.KakuninContainerDao;
import com.example.myapplication.db.dao.KakuninMeisaiDao;
import com.example.myapplication.db.dao.KakuninMeisaiWorkDao;
import com.example.myapplication.db.dao.SystemDao;
import com.example.myapplication.db.dao.SyukkaContainerDao;
import com.example.myapplication.db.dao.SyukkaMeisaiDao;
import com.example.myapplication.db.dao.SyukkaMeisaiWorkDao;
import com.example.myapplication.db.dao.YoteiDao;
import com.example.myapplication.db.entity.CommHistoryEntity;
import com.example.myapplication.db.entity.KakuninContainerEntity;
import com.example.myapplication.db.entity.KakuninMeisaiEntity;
import com.example.myapplication.db.entity.KakuninMeisaiWorkEntity;
import com.example.myapplication.db.entity.SystemEntity;
import com.example.myapplication.db.entity.SyukkaContainerEntity;
import com.example.myapplication.db.entity.SyukkaMeisaiEntity;
import com.example.myapplication.db.entity.SyukkaMeisaiWorkEntity;
import com.example.myapplication.db.entity.YoteiEntity;

@Database(
        entities = {

//============================
//　処理概要　:　AppDatabaseクラス
//============================

                SystemEntity.class,
                YoteiEntity.class,
                SyukkaContainerEntity.class,
                SyukkaMeisaiEntity.class,
                SyukkaMeisaiWorkEntity.class,
                KakuninContainerEntity.class,
                KakuninMeisaiEntity.class,
                KakuninMeisaiWorkEntity.class,
                CommHistoryEntity.class
        },
        version = 3,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    public static final String DB_NAME = "tatsumiDB_handy.sqlite";

    // DAO
    //=================================
    //　機　能　:　system Daoの処理
    //　引　数　:　なし
    //　戻り値　:　[SystemDao] ..... なし
    //=================================
    public abstract SystemDao systemDao();
    //================================
    //　機　能　:　yotei Daoの処理
    //　引　数　:　なし
    //　戻り値　:　[YoteiDao] ..... なし
    //================================

    public abstract YoteiDao yoteiDao();
    //==========================================
    //　機　能　:　syukka Container Daoの処理
    //　引　数　:　なし
    //　戻り値　:　[SyukkaContainerDao] ..... なし
    //==========================================

    public abstract SyukkaContainerDao syukkaContainerDao();
    //=======================================
    //　機　能　:　syukka Meisai Daoの処理
    //　引　数　:　なし
    //　戻り値　:　[SyukkaMeisaiDao] ..... なし
    //=======================================

    public abstract SyukkaMeisaiDao syukkaMeisaiDao();
    //===========================================
    //　機　能　:　syukka Meisai Work Daoの処理
    //　引　数　:　なし
    //　戻り値　:　[SyukkaMeisaiWorkDao] ..... なし
    //===========================================

    public abstract SyukkaMeisaiWorkDao syukkaMeisaiWorkDao();
    //===========================================
    //　機　能　:　kakunin Container Daoの処理
    //　引　数　:　なし
    //　戻り値　:　[KakuninContainerDao] ..... なし
    //===========================================

    public abstract KakuninContainerDao kakuninContainerDao();
    //========================================
    //　機　能　:　kakunin Meisai Daoの処理
    //　引　数　:　なし
    //　戻り値　:　[KakuninMeisaiDao] ..... なし
    //========================================

    public abstract KakuninMeisaiDao kakuninMeisaiDao();
    //============================================
    //　機　能　:　kakunin Meisai Work Daoの処理
    //　引　数　:　なし
    //　戻り値　:　[KakuninMeisaiWorkDao] ..... なし
    //============================================

    public abstract KakuninMeisaiWorkDao kakuninMeisaiWorkDao();
    //======================================
    //　機　能　:　comm History Daoの処理
    //　引　数　:　なし
    //　戻り値　:　[CommHistoryDao] ..... なし
    //======================================

    public abstract CommHistoryDao commHistoryDao();

    // Singleton
    private static volatile AppDatabase INSTANCE;
    //===================================
    //　機　能　:　instanceを取得する
    //　引　数　:　なし
    //　戻り値　:　[AppDatabase] ..... なし
    //===================================

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {

                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DB_NAME // ★拡張子.sqlite
                            )
                            .createFromAsset("databases/" + DB_NAME)
                            // 開発中はこれでOK。運用段階でMigrationを入れる
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
