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


//============================================================
//　処理概要　:　Roomデータベース定義（AppDatabase）
//　対　　象　:　アプリ内SQLite（Room）へのアクセス窓口
//　内　　容　:　Entity定義、DAO提供、Singleton生成を行う
//　関　　数　:　systemDao             ..... SystemDao取得
//　　　　　　:　yoteiDao              ..... YoteiDao取得
//　　　　　　:　syukkaContainerDao     ..... SyukkaContainerDao取得
//　　　　　　:　syukkaMeisaiDao        ..... SyukkaMeisaiDao取得
//　　　　　　:　syukkaMeisaiWorkDao    ..... SyukkaMeisaiWorkDao取得
//　　　　　　:　kakuninContainerDao    ..... KakuninContainerDao取得
//　　　　　　:　kakuninMeisaiDao       ..... KakuninMeisaiDao取得
//　　　　　　:　kakuninMeisaiWorkDao   ..... KakuninMeisaiWorkDao取得
//　　　　　　:　commHistoryDao         ..... CommHistoryDao取得
//　　　　　　:　getInstance            ..... DBインスタンス取得（Singleton）
//============================================================
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
        version = 8,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    //============================================================
    //　定数定義　:　DBファイル名
    //============================================================
    public static final String DB_NAME = "tatsumiDB_handy.sqlite";
    // ・assets/databases 配下からcreateFromAssetで読み込み
    // ・拡張子.sqliteで統一


    //================================================================
    //　機　能　:　SystemDaoを取得する
    //　引　数　:　なし
    //　戻り値　:　[SystemDao] ..... SystemDao
    //================================================================
    public abstract SystemDao systemDao();
    // ・システム設定（M_SYSTEM）へのアクセス


    //================================================================
    //　機　能　:　YoteiDaoを取得する
    //　引　数　:　なし
    //　戻り値　:　[YoteiDao] ..... YoteiDao
    //================================================================
    public abstract YoteiDao yoteiDao();
    // ・予定（T_YOTEI）へのアクセス


    //================================================================
    //　機　能　:　SyukkaContainerDaoを取得する
    //　引　数　:　なし
    //　戻り値　:　[SyukkaContainerDao] ..... SyukkaContainerDao
    //================================================================
    public abstract SyukkaContainerDao syukkaContainerDao();
    // ・出荷コンテナ（T_SYUKKA_CONTAINER）へのアクセス


    //================================================================
    //　機　能　:　SyukkaMeisaiDaoを取得する
    //　引　数　:　なし
    //　戻り値　:　[SyukkaMeisaiDao] ..... SyukkaMeisaiDao
    //================================================================
    public abstract SyukkaMeisaiDao syukkaMeisaiDao();
    // ・出荷明細（T_SYUKKA_MEISAI）へのアクセス


    //================================================================
    //　機　能　:　SyukkaMeisaiWorkDaoを取得する
    //　引　数　:　なし
    //　戻り値　:　[SyukkaMeisaiWorkDao] ..... SyukkaMeisaiWorkDao
    //================================================================
    public abstract SyukkaMeisaiWorkDao syukkaMeisaiWorkDao();
    // ・出荷明細ワーク（W_SYUKKA_MEISAI）へのアクセス


    //================================================================
    //　機　能　:　KakuninContainerDaoを取得する
    //　引　数　:　なし
    //　戻り値　:　[KakuninContainerDao] ..... KakuninContainerDao
    //================================================================
    public abstract KakuninContainerDao kakuninContainerDao();
    // ・確認コンテナ（T_KAKUNIN_CONTAINER）へのアクセス


    //================================================================
    //　機　能　:　KakuninMeisaiDaoを取得する
    //　引　数　:　なし
    //　戻り値　:　[KakuninMeisaiDao] ..... KakuninMeisaiDao
    //================================================================
    public abstract KakuninMeisaiDao kakuninMeisaiDao();
    // ・確認明細（T_KAKUNIN_MEISAI）へのアクセス


    //================================================================
    //　機　能　:　KakuninMeisaiWorkDaoを取得する
    //　引　数　:　なし
    //　戻り値　:　[KakuninMeisaiWorkDao] ..... KakuninMeisaiWorkDao
    //================================================================
    public abstract KakuninMeisaiWorkDao kakuninMeisaiWorkDao();
    // ・確認明細ワーク（W_KAKUNIN_MEISAI）へのアクセス


    //================================================================
    //　機　能　:　CommHistoryDaoを取得する
    //　引　数　:　なし
    //　戻り値　:　[CommHistoryDao] ..... CommHistoryDao
    //================================================================
    public abstract CommHistoryDao commHistoryDao();
    // ・通信履歴（C_COMM_HISTORY）へのアクセス


    //============================================================
    //　処理概要　:　DBインスタンス（Singleton）
    //============================================================
    private static volatile AppDatabase INSTANCE;


    //================================================================
    //　機　能　:　DBインスタンスを取得する（Singleton）
    //　引　数　:　context ..... Context
    //　戻り値　:　[AppDatabase] ..... AppDatabaseインスタンス
    //================================================================
    public static AppDatabase getInstance(Context context) {

        // 既に生成済みならそのまま返す
        if (INSTANCE == null) {

            // 多重生成防止（ダブルチェックロッキング）
            synchronized (AppDatabase.class) {

                // synchronized内でも再チェック
                if (INSTANCE == null) {

                    // RoomDatabaseを生成
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DB_NAME  // ★拡張子.sqlite
                            )
                            // assets/databases/ 配下のDBを初期DBとして展開
                            .createFromAsset("databases/" + DB_NAME)

                            // 開発中は破壊的マイグレーションでOK
                            // ※運用段階ではMigrationを実装してデータ維持する
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }

        return INSTANCE;
    }
}