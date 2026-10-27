plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.example.tforwarder"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.example.tforwarder"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    // مكتبة TDLib الجاهزة للأندرويد (عبر JitPack). إن فشل التنزيل راجع README.
    implementation("com.github.tdlibx:td:1.8.0")
}
