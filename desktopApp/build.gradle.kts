plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val appVersion = providers.gradleProperty("appVersion").orElse("1.1.7").get()

val extendedIconsRuntime = configurations.create("extendedIconsRuntime") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    add(
        extendedIconsRuntime.name,
        "org.jetbrains.compose.material:material-icons-extended-desktop:${libs.versions.compose.multiplatform.get()}@jar"
    )
}

val iconImportPattern = Regex("""import androidx\.compose\.material\.icons\.(automirrored\.)?filled\.(\w+)""")
val sharedMaterialIconEntries = fileTree("../shared/src/commonMain/kotlin") {
    include("**/*.kt")
}.files.flatMap { sourceFile ->
    sourceFile.readLines().mapNotNull { line ->
        iconImportPattern.find(line)?.let { match ->
            val iconPackage = if (match.groupValues[1].isNotEmpty()) "automirrored/filled" else "filled"
            "androidx/compose/material/icons/$iconPackage/${match.groupValues[2]}Kt.class"
        }
    }
}.distinct().sorted()

val desktopMaterialIcons = tasks.register<Jar>("desktopMaterialIcons") {
    archiveFileName.set("material-icons-extended-desktop-slim.jar")
    destinationDirectory.set(layout.buildDirectory.dir("libs"))
    inputs.files(fileTree("../shared/src/commonMain/kotlin") { include("**/*.kt") })
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    from({ extendedIconsRuntime.map { zipTree(it) } }) {
        include("META-INF/material-icons-extended.kotlin_module")
        include(*sharedMaterialIconEntries.toTypedArray())
    }
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    jvm()

    sourceSets {
        jvmMain.dependencies {
            implementation(project(":shared")) {
                exclude(group = "org.jetbrains.compose.material", module = "material-icons-extended-desktop")
            }
            runtimeOnly(files(desktopMaterialIcons.flatMap { it.archiveFile }))
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.bytegif.sendchat.desktop.MainKt"

        buildTypes.release.proguard {
            isEnabled.set(false)
        }

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
            packageVersion = appVersion
            description = "SendChat - Cross-Platform Local Network Sharing"
            copyright = "© 2026 ByteGif"
            vendor = "ByteGif"

            includeAllModules = false
            modules("java.instrument", "java.management", "java.naming", "java.sql", "jdk.unsupported")

            macOS {
                bundleID = "com.bytegif.sendchat"
                iconFile.set(project.file("src/jvmMain/resources/icon.icns"))
                packageBuildVersion = appVersion
                minimumSystemVersion = "11.0"
            }

            windows {
                iconFile.set(project.file("src/jvmMain/resources/icon.ico"))
                packageVersion = appVersion
                menuGroup = "SendChat"
                upgradeUuid = "6b4b4553-7393-4e31-89d8-94f31b5c98d1"
            }

            linux {
                iconFile.set(project.file("src/jvmMain/resources/icon.png"))
                packageVersion = appVersion
                appCategory = "Network"
                menuGroup = "Network"
            }
        }
    }
}



