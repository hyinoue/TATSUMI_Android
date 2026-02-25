// ============================================================
//  処理概要 : app/build.gradle.kts（アプリモジュール設定）
//  内容     : Androidビルド設定 / 依存ライブラリ管理
// ============================================================

plugins {
    alias(libs.plugins.android.application)
}

android {

    // アプリパッケージ名
    namespace = "com.example.myapplication"

    // コンパイルSDK
    compileSdk = 36

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// ============================================================
//  依存ライブラリ設定
// ============================================================

dependencies {

    // AndroidX
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // HTTP通信
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // バーコードSDK（app/SDK 配下）
    implementation(files("SDK/bhtsdk_r200050000_v2.00.05.aar"))

    // CameraX
    implementation("androidx.camera:camera-core:1.3.2")
    implementation("androidx.camera:camera-camera2:1.3.2")
    implementation("androidx.camera:camera-lifecycle:1.3.2")
    implementation("androidx.camera:camera-view:1.3.2")
    implementation("androidx.core:core:1.13.1")

    // Room（DB）
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    // TableView
    implementation("com.github.evrencoskun:TableView:v0.8.9.4")

    // JUnit5
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")

    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

// ============================================================
//  JUnit5設定
// ============================================================

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}