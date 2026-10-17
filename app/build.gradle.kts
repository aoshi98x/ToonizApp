plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.colombianapps.toonizapp"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.colombianapps.toonizapp"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    // JNA para interoperabilidad con librerías nativas .so desde Kotlin
    implementation("net.java.dev.jna:jna:5.14.0@aar")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
}
val isWindows = System.getProperty("os.name").lowercase().contains("win")

fun setupWindowsRustToolchainEnvironment(exec: Exec) {
    if (isWindows) {
        val userHome = System.getProperty("user.home")
        val extraBin = file("$userHome/.cargo/extra_bin")
        if (!extraBin.exists()) {
            extraBin.mkdirs()
        }
        val dllToolInExtraBin = file("${extraBin.absolutePath}/dlltool.exe")
        if (!dllToolInExtraBin.exists()) {
            val sdkDir = project.findProperty("sdk.dir")?.toString() ?: "$userHome/AppData/Local/Android/Sdk"
            val ndkFolder = file("$sdkDir/ndk")
            val llvmDllTool = ndkFolder.walkTopDown().firstOrNull { it.name == "llvm-dlltool.exe" }
            if (llvmDllTool != null && llvmDllTool.exists()) {
                llvmDllTool.copyTo(dllToolInExtraBin, overwrite = true)
            }
        }
        val selfContained = file("$userHome/.rustup/toolchains/stable-x86_64-pc-windows-gnu/lib/rustlib/x86_64-pc-windows-gnu/bin/self-contained").absolutePath
        val currentPath = System.getenv("PATH") ?: ""
        exec.environment("PATH", "${extraBin.absolutePath};$selfContained;$currentPath")
    }
}

// Compilar Rust usando cargo-ndk
tasks.register<Exec>("buildRustCore") {
    workingDir = file("../rustCore")
    setupWindowsRustToolchainEnvironment(this)
    commandLine(
        "cargo", "ndk",
        "-t", "arm64-v8a",
        "-t", "x86_64",
        "-o", "../app/src/main/jniLibs",
        "build"
    )
}

// Compilar librería Rust para el host (requerido para UniFFI bindgen)
tasks.register<Exec>("buildRustCoreHost") {
    workingDir = file("../rustCore")
    setupWindowsRustToolchainEnvironment(this)
    commandLine("cargo", "build")
}

// Generar los bindings en Kotlin con UniFFI
tasks.register<Exec>("generateUniFFIBindings") {
    dependsOn("buildRustCore", "buildRustCoreHost")
    workingDir = file("../rustCore")
    setupWindowsRustToolchainEnvironment(this)

    val osName = System.getProperty("os.name").lowercase()
    val libFileName = when {
        osName.contains("win") -> "target/debug/rust_core.dll"
        osName.contains("mac") -> "target/debug/librust_core.dylib"
        else -> "target/debug/librust_core.so"
    }

    commandLine(
        "cargo", "run", "--bin", "uniffi-bindgen",
        "generate", "--library", libFileName,
        "--language", "kotlin",
        "--out-dir", "../app/src/main/java"
    )
}

tasks.named("preBuild") {
    dependsOn("generateUniFFIBindings")
}