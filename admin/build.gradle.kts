import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
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
    compileSdk = 36
    defaultConfig {
        applicationId = "com.trader.admin"
        minSdk = 24; targetSdk = 36
        versionCode = 1; versionName = "1.0.0"
    }
    signingConfigs {
        create("release") {
            storeFile   = (System.getenv("KEYSTORE_PATH")       ?: keystoreProperties["storeFile"] as String?)?.let { file(it) }
            storePassword = System.getenv("KEYSTORE_PASSWORD")    ?: keystoreProperties["storePassword"] as String?
            keyAlias    = System.getenv("KEY_ALIAS")              ?: keystoreProperties["keyAlias"]     as String?
            keyPassword = System.getenv("KEY_PASSWORD")           ?: keystoreProperties["keyPassword"]  as String?
        }
    }
    buildTypes {
        debug { applicationIdSuffix = ".debug" }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Without signing inputs (QA jobs), produce an explicitly unsigned artifact, as :salesmanager does.
            if (signingConfigs.getByName("release").storeFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_11; targetCompatibility = JavaVersion.VERSION_11 }
    buildFeatures { compose = true }
    testOptions { unitTests.isIncludeAndroidResources = true }
    // google-services.json has no "com.trader.admin.debug" client, so only release builds;
    // host (unit) tests therefore run against the release variant.
    testBuildType = "release"
    // Test-only helpers shared with :salesmanager (never part of an APK).
    sourceSets.getByName("test").kotlin.directories.add("../testing/recomposition")
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    implementation(project(":app"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.auth)                  
    
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.test.compose.junit)
    testImplementation(libs.test.robolectric)
    testImplementation(libs.test.androidx.core)
    testImplementation(libs.test.junit)
    testImplementation(libs.test.mockk)
    testImplementation(libs.test.koin)
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11) } }

// Compose compiler reports/metrics for recomposition audits: ./gradlew -PcomposeReports=true assembleDebug
if (providers.gradleProperty("composeReports").orNull == "true") {
    composeCompiler {
        reportsDestination.set(layout.buildDirectory.dir("compose_compiler"))
        metricsDestination.set(layout.buildDirectory.dir("compose_compiler"))
    }
}
