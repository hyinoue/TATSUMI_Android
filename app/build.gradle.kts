plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.myapplication"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

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
        // JUnit5 は Java 8 以降対応。ここでは既存設定そのまま Java 11 を利用
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // CameraX（必須）
    implementation("androidx.camera:camera-core:1.3.2")
    implementation("androidx.camera:camera-camera2:1.3.2")
    implementation("androidx.camera:camera-lifecycle:1.3.2")
    implementation("androidx.camera:camera-view:1.3.2")

    // --- JUnit 5 (Jupiter) 用依存関係 ---
    // Aggregator モジュール: API / Engine / Params をまとめて提供
    // Java 11 で使える 5.11 系を指定
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")

    // インストルメンテーションテスト / UI テストは従来どおり JUnit4 系
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

// --- すべてのテストタスクで JUnit Platform (JUnit5) を使う設定 ---
tasks.withType<Test> {
    useJUnitPlatform()
}
