plugins {
    id("com.android.application")
}

// as .so vêm dos scripts (build-android-modules.sh e build-android-runtime.sh): o Gradle só empacota
val nativeLibs = rootProject.file("../build/lib")

android {
    namespace = "com.luisxl.gowcollection"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.luisxl.gowcollection"
        minSdk = 28 // AAudio e Vulkan 1.1 de verdade
        targetSdk = 35
        versionCode = 4
        versionName = "0.1.2-fork.1"
        ndk { abiFilters += listOf("arm64-v8a") }
    }
    sourceSets["main"].jniLibs.directories.add("src/main/jniLibs")
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging {
        // extraídas no disco: o vitart abre os módulos pelo caminho (e o LoadExec confere se o arquivo existe)
        jniLibs.useLegacyPackaging = true
    }
}

// copia as .so prontas para o lugar que o empacotador lê
val copyNative by tasks.registering(Copy::class) {
    from(nativeLibs) { include("*.so") }
    into("src/main/jniLibs/arm64-v8a")
}
val copyAssets by tasks.registering(Copy::class) {
    from(rootProject.file("../build/link.hlevars.txt"))
    into("src/main/assets")
}
tasks.named("preBuild") { dependsOn(copyNative, copyAssets) }
