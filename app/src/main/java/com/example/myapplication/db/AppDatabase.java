package com.example.myapplication.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

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

//============================================================
//　処理概要　:　AppDatabaseクラス
//============================================================

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

    // DAO
    public abstract SystemDao systemDao();

    public abstract YoteiDao yoteiDao();

    public abstract SyukkaContainerDao syukkaContainerDao();

    public abstract SyukkaMeisaiDao syukkaMeisaiDao();

    public abstract SyukkaMeisaiWorkDao syukkaMeisaiWorkDao();

    public abstract KakuninContainerDao kakuninContainerDao();

    public abstract KakuninMeisaiDao kakuninMeisaiDao();

    public abstract KakuninMeisaiWorkDao kakuninMeisaiWorkDao();

    public abstract CommHistoryDao commHistoryDao();

    // Singleton
    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {

                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "tatsumi_handy.sqlite" // ★拡張子.sqlite
                            )
                            .addCallback(new Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    insertDefaultSystem(db);
                                }
                            })
                            // 開発中はこれでOK。運用段階でMigrationを入れる
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    //============================================================
    //　機　能　:　M_SYSTEM 初期値登録（DB初回作成時のみ）
    //　備　考　:　ID=1 の1レコード運用想定
    //　　　　　:　INSERT OR IGNORE にして二重登録事故を回避
    //============================================================
    private static void insertDefaultSystem(@NonNull SupportSQLiteDatabase db) {
        // ★ 固定値（ここをあなたの要件値に合わせて変更）
        final int defaultContainerJyuryo = 2400;
        final int defaultDunnageJyuryo = 32;

        db.execSQL(
                "INSERT OR IGNORE INTO M_SYSTEM (RENBAN, DEFAULT_CONTAINER_JYURYO, DEFAULT_DUNNAGE_JYURYO) " +
                        "VALUES (?, ?, ?)",
                new Object[]{1, defaultContainerJyuryo, defaultDunnageJyuryo}
        );
    }
}
