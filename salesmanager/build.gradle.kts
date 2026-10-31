import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    jacoco
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) load(keystorePropertiesFile.inputStream())
}

android {
    namespace  = "com.trader.salesmanager"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.trader.salesmanager"
        minSdk = 24; targetSdk = 35
        versionCode = 10205; versionName = "1.2.5"
    }
    signingConfigs {
        create("release") {
            storeFile = (System.getenv("KEYSTORE_PATH") ?: keystoreProperties["storeFile"] as String?)?.let { file(it) }
            storePassword = System.getenv("KEYSTORE_PASSWORD")    ?: keystoreProperties["storePassword"] as String?
            keyAlias    = System.getenv("KEY_ALIAS")              ?: keystoreProperties["keyAlias"]     as String?
            keyPassword = System.getenv("KEY_PASSWORD")           ?: keystoreProperties["keyPassword"]  as String?
        }
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        // ── نسخة الاختبار: تُنصَّب بجانب البرودكشن على نفس الجهاز ──
        create("staging") {
            applicationIdSuffix = ".test"           // com.trader.salesmanager.test
            manifestPlaceholders["appName"] = "Sales Test"
            resValue("string", "app_name", "مدير المبيعات TEST")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Without signing inputs, produce an explicitly unsigned QA artifact.
            if (signingConfigs.getByName("release").storeFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        release {
            isMinifyEnabled = true; isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Without signing inputs, produce an explicitly unsigned QA artifact.
            if (signingConfigs.getByName("release").storeFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    testOptions { unitTests.isIncludeAndroidResources = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_11; targetCompatibility = JavaVersion.VERSION_11 }
    kotlinOptions { jvmTarget = "11" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    implementation(project(":app"))
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.activity.compose)
    // ── ML Kit Barcode ────────────────────────────────────────
    implementation(libs.mlkit.barcode)
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)
    debugImplementation(libs.compose.ui.tooling)
}

apply(from = rootProject.file("gradle/qa-coverage.gradle"))
