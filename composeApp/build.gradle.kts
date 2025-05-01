//import org.jetbrains.compose.reload.ComposeHotRun
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("org.jetbrains.kotlin.plugin.serialization") version("1.8.10")
    id("app.cash.sqldelight") version("2.0.2")
    id("dev.hydraulic.conveyor") version "1.12"
//    id("org.jetbrains.compose.hot-reload") version "1.0.0-alpha05" // <- add this additionally
//    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

group = "io.github.octestx.krecall"
version = "0.1"

kotlin {
    androidTarget {
//        @OptIn(ExperimentalKotlinGradlePluginApi::class)
//        compilerOptions {
//            jvmTarget.set(JvmTarget.JVM_11)
//        }
    }
    
    jvm("desktop") {
        // 可选配置，如设置目标版本
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
    }
    
    sourceSets {
        val desktopMain by getting
        
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.basic.multiplatform.lib)
            implementation(libs.basic.multiplatform.ui.lib)
            implementation(libs.library)
            implementation(libs.kotlinx.serialization.json)

            // Ktor基础库
            val ktorVersion = "3.1.0"
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)

            implementation(libs.openai.client)
            implementation(libs.oapi.java.sdk)

            implementation(libs.opencv)

            implementation(libs.coil.compose)
            implementation(libs.kotlinx.datetime)

            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs)
            implementation(libs.filekit.dialogs.compose)
            implementation(libs.filekit.coil)

            implementation(libs.compottie)
            implementation(libs.compottie.dot)

            implementation(libs.conveyor.control)

            implementation("io.arrow-kt:arrow-core:2.1.0")
            implementation("io.arrow-kt:arrow-fx-coroutines:2.1.0")
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.sqlite.driver)
        }
    }
    sourceSets.commonMain.dependencies {
        implementation(kotlin("reflect"))
    }
}

android {
    namespace = "io.github.octestx.krecall"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "io.github.octestx.krecall"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.ui.text.android)
    debugImplementation(compose.uiTooling)

    // Use the configurations created by the Conveyor plugin to tell Gradle/Conveyor where to find the artifacts for each platform.
    linuxAmd64(compose.desktop.linux_x64)
    macAmd64(compose.desktop.macos_x64)
    macAarch64(compose.desktop.macos_arm64)
    windowsAmd64(compose.desktop.windows_x64)
}

compose {
    resources {
        publicResClass = true
        generateResClass = always
    }
//    composeCompiler {
//        featureFlags.add(ComposeFeatureFlag.OptimizeNonSkippingGroups)
//    }
}

compose.desktop {
    application {
        mainClass = "io.github.octestx.krecall.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "io.github.octestx.krecall"
            packageVersion = "1.0.0"
        }
    }
}
sqldelight {
    databases {
        create("DB") {
            packageName.set("models.sqld")
            dialect("app.cash.sqldelight:sqlite-3-35-dialect:2.0.2") // 明确指定方言
//            // 添加迁移配置
//            migrationOutputDirectory.set(file("src/main/sqldelight/migrations"))
//            schemaOutputDirectory.set(file("src/main/sqldelight/schemas"))
//            // 启用迁移验证
//            verifyMigrations.set(true)
        }
    }
}

// region Work around temporary Compose bugs.
configurations.all {
    attributes {
        // https://github.com/JetBrains/compose-jb/issues/1404#issuecomment-1146894731
        attribute(Attribute.of("ui", String::class.java), "awt")
    }
}
// endregion


//tasks.withType<ComposeHotRun>().configureEach {
//    mainClass.set("io.github.octestx.krecall.MainKt")
//}