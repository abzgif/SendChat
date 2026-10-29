plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
}

val appVersion = providers.gradleProperty("appVersion").orElse("1.1.6").get()
tasks.withType<Jar>().configureEach {
    manifest.attributes["Implementation-Version"] = appVersion
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    jvm()
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            implementation(libs.ktor.server.core)
            implementation(libs.ktor.server.cio)
            implementation(libs.ktor.server.content.negotiation)
            implementation(libs.ktor.server.cors)
            implementation(libs.ktor.serialization.kotlinx.json)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.content.negotiation)

            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines.extensions)

            implementation(libs.zxing.core)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.sqldelight.sqlite.driver)
            implementation(libs.jmdns)
            implementation(libs.bouncycastle.bcprov)
            implementation(libs.bouncycastle.bcpkix)
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.sqldelight.android.driver)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.compose)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

android {
    namespace = "com.bytegif.sendchat.shared"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

sqldelight {
    databases {
        create("SendChatDatabase") {
            packageName.set("com.bytegif.sendchat.data.db")
        }
    }
}
