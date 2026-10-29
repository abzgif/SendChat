plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val appVersion = providers.gradleProperty("appVersion").orElse("1.1.6").get()
val appVersionCode = providers.gradleProperty("appVersionCode")
    .map { it.toIntOrNull() ?: 7 }
    .orElse(7)
    .get()

android {
    namespace = "com.bytegif.sendchat.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bytegif.sendchat"
        minSdk = 24
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersion.removePrefix("v")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            debugSymbolLevel = "SYMBOL_TABLE"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
        }
    }

    packaging {
        resources {
            excludes += listOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/INDEX.LIST",
                "META-INF/*.kotlin_module",
                "**/*.dll",
                "**/*.jnilib"
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
