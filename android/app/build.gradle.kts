plugins {
    id("com.android.application")
    id("kotlin-android")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

android {
    namespace = "com.titan.universe_share"
    compileSdk = 35  // 从34更新到35
    // ndkVersion = flutter.ndkVersion
    ndkVersion = "27.0.12077973"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    defaultConfig {
        // TODO: Specify your own unique Application ID (https://developer.android.com/studio/build/application-id.html).
        applicationId = "com.titan.universe_share"
        // You can update the following values to match your application needs.
        // For more information, see: https://flutter.dev/to/review-gradle-config.
        minSdk = flutter.minSdkVersion
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    buildTypes {
        release {
            // TODO: Add your own signing config for the release build.
            // Signing with the debug keys for now, so `flutter run --release` works.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

flutter {
    source = "../.."
}

// 添加自定义的 Maven 仓库配置
repositories {
    mavenCentral()
    google()
    // 添加 OpenPnp Maven 仓库，它有 OpenCV4Android
    maven {
        url = uri("https://javacv.scijava.org/")
    }
    // 添加 Bytedeco 仓库 (另一个 OpenCV 可用的仓库)
    maven {
        url = uri("https://packagecloud.io/bytedeco/javacpp/maven2/")
    }
}

dependencies {
    // 添加AndroidX核心库
    implementation("androidx.core:core:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    
    // 媒体相关依赖
    implementation("androidx.media:media:1.7.0")
    
    // ML Kit 文本识别和条码扫描
    implementation("com.google.mlkit:text-recognition:16.0.0")
    implementation("com.google.mlkit:text-recognition-chinese:16.0.0")
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    implementation("com.google.android.gms:play-services-tasks:18.0.2")
    
    // 图像处理
    implementation("com.google.android.gms:play-services-mlkit-image-labeling:16.0.8")
    implementation("androidx.exifinterface:exifinterface:1.3.6")
    
    testImplementation("junit:junit:4.13.2")
    // ... existing dependencies ...
}
