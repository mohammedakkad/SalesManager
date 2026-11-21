plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library)     apply false
    alias(libs.plugins.kotlin.android)      apply false
    alias(libs.plugins.kotlin.compose)      apply false
    alias(libs.plugins.ksp)                 apply false
    alias(libs.plugins.google.services)     apply false
    alias(libs.plugins.firebase.crashlytics) apply false
}

subprojects {
    pluginManager.withPlugin("com.android.library") {
        extensions.configure<com.android.build.gradle.LibraryExtension> {
            buildTypes {
                maybeCreate("staging").apply {
                    matchingFallbacks += listOf("release")
                }
            }
        }
    }
}