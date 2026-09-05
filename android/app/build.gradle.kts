plugins {
    id("com.android.application")
}

android {
    namespace = "zw.co.dialhealth.screenfactory"
    compileSdk = 36

    defaultConfig {
        applicationId = "zw.co.dialhealth.screenfactory.mobile"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "1.3.0"
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
}
