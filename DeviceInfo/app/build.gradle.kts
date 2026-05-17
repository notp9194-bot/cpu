plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.deviceinfo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.deviceinfo"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // local.properties se padhega — keystore file project root me rakhna
            val props = java.util.Properties().apply {
                val f = rootProject.file("local.properties")
                if (f.exists()) load(f.inputStream())
            }
            storeFile     = file(props.getProperty("KEYSTORE_FILE", "keystore.jks"))
            storePassword = props.getProperty("KEYSTORE_PASSWORD", "")
            keyAlias      = props.getProperty("KEY_ALIAS", "")
            keyPassword   = props.getProperty("KEY_PASSWORD", "")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // AAB ke liye bundle config — Play Store automatically ABI split karta hai
    bundle {
        language { enableSplit = true }
        density  { enableSplit = true }
        abi      { enableSplit = true }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions { jvmTarget = "1.8" }
    buildFeatures { viewBinding = true }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":feature-soc"))
    implementation(project(":feature-device"))
    implementation(project(":feature-system"))
    implementation(project(":feature-battery"))
    implementation(project(":feature-thermal"))
    implementation(project(":feature-sensors"))
    implementation(project(":feature-about"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.fragment.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
