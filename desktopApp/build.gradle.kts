plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    jvm()

    sourceSets {
        jvmMain.dependencies {
            implementation(project(":shared"))
            implementation(compose.desktop.currentOs)
            implementation(compose.desktop.macos_arm64)
            implementation(compose.desktop.macos_x64)
            implementation(compose.desktop.windows_x64)
            implementation(compose.desktop.linux_x64)
            implementation(compose.desktop.linux_arm64)
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.bytegif.sendchat.desktop.MainKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Pkg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Rpm
            )
            packageName = "SendChat"
            packageVersion = "1.0.0"
            description = "SendChat - Cross-Platform Local Network Sharing"
            copyright = "© 2026 ByteGif"
            vendor = "ByteGif"

            includeAllModules = true

            macOS {
                bundleID = "com.bytegif.sendchat"
                iconFile.set(project.file("src/jvmMain/resources/icon.icns"))
                packageBuildVersion = "1.0.0"
                minimumSystemVersion = "11.0"
            }

            windows {
                iconFile.set(project.file("src/jvmMain/resources/icon.ico"))
                packageVersion = "1.0.0"
                menuGroup = "SendChat"
                upgradeUuid = "6b4b4553-7393-4e31-89d8-94f31b5c98d1"
            }

            linux {
                iconFile.set(project.file("src/jvmMain/resources/icon.png"))
                packageVersion = "1.0.0"
                appCategory = "Network"
                menuGroup = "Network"
            }
        }
    }
}



