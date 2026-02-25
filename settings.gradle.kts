// ============================================================
//  処理概要 : settings.gradle.kts（プロジェクト全体設定）
//  内容     : プラグイン管理 / 依存管理 / モジュール構成
// ============================================================

pluginManagement {
    repositories {
        // Google公式
        google()

        // Maven Central
        mavenCentral()

        // Gradle Plugin Portal
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {

    // 各モジュールで repositories を定義するとエラーにする
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        // Google公式
        google()

        // Maven Central
        mavenCentral()

        // JitPack（GitHubライブラリ用）
        maven(url = "https://jitpack.io")
    }
}

// ルートプロジェクト名
rootProject.name = "My Application"

// ビルド対象モジュール
include(":app")