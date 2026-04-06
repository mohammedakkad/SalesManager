import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) load(keystorePropertiesFile.inputStream())
}

android {
    namespace  = "com.trader.admin"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.trader.admin"
        minSdk = 24; targetSdk = 35
        versionCode = 1; versionName = "1.0.0"
    }
    signingConfigs {
        create("release") {
            storeFile   = file(System.getenv("KEYSTORE_PATH")     ?: keystoreProperties["storeFile"]    as String)
            storePassword = System.getenv("KEYSTORE_PASSWORD")    ?: keystoreProperties["storePassword"] as String
            keyAlias    = System.getenv("KEY_ALIAS")              ?: keystoreProperties["keyAlias"]     as String
            keyPassword = System.getenv("KEY_PASSWORD")           ?: keystoreProperties["keyPassword"]  as String
        }
    }
    buildTypes {
        debug { applicationIdSuffix = ".debug" }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_11; targetCompatibility = JavaVersion.VERSION_11 }
    kotlinOptions { jvmTarget = "11" }
    buildFeatures { compose = true }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    implementation(project(":app"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.firebase.messaging)
    debugImplementation(libs.compose.ui.tooling)
}
