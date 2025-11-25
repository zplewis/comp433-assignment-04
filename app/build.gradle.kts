import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.comp433assignment04"
    compileSdk {
        version = release(36)
    }

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.comp433assignment04"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val localProperties = project.rootProject.file("local.properties")
        val properties = Properties()
        if (localProperties.exists()) {
            properties.load(localProperties.inputStream())
        }

        val apiKey: String? = properties.getProperty("api.key")
        val visionApiKey : String? = properties.getProperty("vision.api.key")

        buildConfigField("String", "GEMINI_API_KEY", "\"${apiKey ?: ""}\"")
        buildConfigField("String", "VISION_API_KEY", "\"${visionApiKey ?: ""}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // In libs.versions.toml, you can keep all dependency coordinates like (group:name:version)

    // this is needed to add the OkHttp library
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // These are needed to access the Google Vision API:
    implementation("com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava")
    implementation("com.google.api-client:google-api-client-android:1.23.0") {
        exclude(module = "httpclient")
    }
    implementation("com.google.http-client:google-http-client-gson:1.23.0") {
        exclude(module = "httpclient")
    }
    implementation("com.google.apis:google-api-services-vision:v1-rev369-1.23.0")

    // these were standard when I created this project
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}