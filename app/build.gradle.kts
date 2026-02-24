//============================================================
//　処理概要　:　app/build.gradle.kts（アプリモジュール設定）
//　対　　象　:　Androidアプリのビルド設定／依存関係定義
//　内　　容　:　・plugins設定（Android Application）
//　　　　　　:　・android設定（SDK、BuildType、Java互換）
//　　　　　　:　・dependencies（ライブラリ依存）
//　　　　　　:　・JUnit5有効化（JUnit Platform設定）
//============================================================

plugins {
    //============================================================
    //　処理概要　:　プラグイン適用
    //　内　　容　:　Version Catalog の alias を使用して適用
    //============================================================
    alias(libs.plugins.android.application)
}

android {
    //============================================================
    //　処理概要　:　namespace設定
    //　内　　容　:　Rクラス等の生成に使用されるパッケージ名
    //============================================================
    namespace = "com.example.myapplication"

    //============================================================
    //　処理概要　:　BuildConfig出力設定
    //　内　　容　:　BuildConfigクラスを生成する
    //============================================================
    buildFeatures {
        buildConfig = true
    }

    //============================================================
    //　処理概要　:　compileSdk設定
    //　内　　容　:　コンパイルに使用するAndroid SDKバージョン
    //　備　　考　:　記載形式は現状の書き方を維持（カタログ側で解決できている前提）
    //============================================================
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        //============================================================
        //　処理概要　:　基本アプリ設定
        //　内　　容　:　アプリID、対応SDK、バージョン情報等を設定
        //============================================================
        applicationId = "com.example.myapplication"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        //============================================================
        //　処理概要　:　Instrumentationテストランナー設定
        //============================================================
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        //============================================================
        //　処理概要　:　リリースビルド設定
        //　内　　容　:　難読化／最適化設定（Proguard/R8）
        //============================================================
        release {
            // ・現状は難読化OFF（デバッグ優先）
            // ・運用段階でONにする場合はルール整備が必要
            isMinifyEnabled = false

            // ・Proguard設定ファイル
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        //============================================================
        //　処理概要　:　Java互換設定
        //　内　　容　:　Java 11 でコンパイルする
        //============================================================
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    //============================================================
    //　処理概要　:　基本UI／AndroidX依存
    //============================================================
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    //============================================================
    //　処理概要　:　HTTP通信ライブラリ（OkHttp）
    //============================================================
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    //============================================================
    //　処理概要　:　バーコードSDK（ローカルAAR）
    //　内　　容　:　app/SDK 配下の aar を参照
    //============================================================
    implementation(files("SDK/bhtsdk_r200050000_v2.00.05.aar"))

    //============================================================
    //　処理概要　:　CameraX（カメラ機能）
    //　内　　容　:　camera2 / lifecycle / view を含めて導入
    //============================================================
    implementation("androidx.camera:camera-core:1.3.2")
    implementation("androidx.camera:camera-camera2:1.3.2")
    implementation("androidx.camera:camera-lifecycle:1.3.2")
    implementation("androidx.camera:camera-view:1.3.2")

    //============================================================
    //　処理概要　:　AndroidX Core（拡張機能）
    //============================================================
    implementation("androidx.core:core:1.13.1")

    //============================================================
    //　処理概要　:　Room（DB）
    //　内　　容　:　ランタイム＋アノテーションプロセッサ
    //============================================================
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    //============================================================
    //　処理概要　:　TableView（表表示）
    //============================================================
    implementation("com.github.evrencoskun:TableView:v0.8.9.4")

    //============================================================
    //　処理概要　:　UnitTest（JUnit5）
    //　内　　容　:　JUnit Jupiter を使用
    //============================================================
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")

    //============================================================
    //　処理概要　:　Android Instrumentation Test（従来）
    //============================================================
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

//============================================================
//　処理概要　:　JUnit Platform設定（JUnit5有効化）
//　対　　象　:　すべてのテストタスク（Test）
//============================================================
tasks.withType<Test> {
    // ・JUnit4ではなくJUnit Platform（JUnit5）で実行する
    useJUnitPlatform()
}