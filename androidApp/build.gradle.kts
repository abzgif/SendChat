plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val appVersion = providers.gradleProperty("appVersion").orElse("1.1.0").get()
val appVersionParts = appVersion.removePrefix("v").substringBefore('-').split('.')
    .map { it.toIntOrNull() ?: 0 }
val appVersionCode = appVersionParts.getOrElse(0) { 0 } * 1_000_000 +
    appVersionParts.getOrElse(1) { 0 } * 1_000 +
    appVersionParts.getOrElse(2) { 0 }

android {
    namespace = "com.bytegif.sendchat.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bytegif.sendchat"
        minSdk = 24
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersion.removePrefix("v")

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.ui)
}
