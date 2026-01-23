plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.myapplication"

    // あなたの書き方に合わせて維持（カタログ側で解決できている前提）
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 29
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

    // ===== バーコードSDK（app/SDK/ 配下の aar）=====
    implementation(files("SDK/bhtsdk_r200050000_v2.00.05.aar"))

    // CameraX（必須）
    implementation("androidx.camera:camera-core:1.3.2")
    implementation("androidx.camera:camera-camera2:1.3.2")
    implementation("androidx.camera:camera-lifecycle:1.3.2")
    implementation("androidx.camera:camera-view:1.3.2")

    //DBのroom
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    implementation("com.github.evrencoskun:TableView:v0.8.9.4")

    // JUnit 5
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")

    // AndroidTest は従来どおり
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

// --- すべてのテストタスクで JUnit Platform (JUnit5) を使う設定 ---
tasks.withType<Test> {
    useJUnitPlatform()
}
