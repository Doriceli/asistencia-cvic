plugins {
    id("com.android.application")
}

android {
    namespace = "mx.cvic.asistencia"
    compileSdk = 37

    defaultConfig {
        applicationId = "mx.cvic.asistencia"
        // Android 10+: guardar en Descargas con MediaStore sin pedir permisos.
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Sin dependencias externas: la app es un WebView con la página en assets/index.html.
