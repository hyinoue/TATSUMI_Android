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
                            // 開発中はこれでOK。運用段階でMigrationを入れる
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
