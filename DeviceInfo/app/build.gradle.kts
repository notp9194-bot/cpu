import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

fun localProps(): Properties {
    val props = Properties()
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { props.load(it) }
    return props
}

android {
    namespace = "com.example.deviceinfo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.deviceinfo"
        minSdk = 24
        targetSdk = 35
        versionCode = 20
        versionName = "20.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val props = localProps()
            val ksFile = props["KEYSTORE_FILE"]?.toString() ?: "keystore.jks"
            if (rootProject.file(ksFile).exists()) {
                storeFile     = rootProject.file(ksFile)
                storePassword = props["KEYSTORE_PASSWORD"]?.toString() ?: ""
                keyAlias      = props["KEY_ALIAS"]?.toString()         ?: ""
                keyPassword   = props["KEY_PASSWORD"]?.toString()      ?: ""
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
        release {
            isMinifyEnabled    = true
            isShrinkResources  = true
            signingConfig      = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    bundle {
        language { enableSplit = true }
        density  { enableSplit = true }
        abi      { enableSplit = true }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { viewBinding = true }
}

dependencies {
    // ── Existing modules ──────────────────────────────────────────────────
    implementation(project(":core"))
    implementation(project(":feature-soc"))
    implementation(project(":feature-device"))
    implementation(project(":feature-system"))
    implementation(project(":feature-battery"))
    implementation(project(":feature-thermal"))
    implementation(project(":feature-sensors"))
    implementation(project(":feature-about"))
    implementation(project(":feature-network"))
    implementation(project(":feature-camera"))
    implementation(project(":feature-audio"))
    implementation(project(":feature-display"))
    implementation(project(":feature-codec"))
    implementation(project(":feature-benchmark"))
    implementation(project(":feature-connectivity"))
    implementation(project(":feature-power"))
    implementation(project(":feature-gpu"))
    implementation(project(":feature-memory"))
    implementation(project(":feature-security"))
    implementation(project(":feature-input"))
    implementation(project(":feature-favorites"))
    implementation(project(":feature-health"))
    // ── 20 New Advanced Feature Modules ──────────────────────────────────
    implementation(project(":feature-cpucore"))
    implementation(project(":feature-chargesession"))
    implementation(project(":feature-pingmonitor"))
    implementation(project(":feature-storageio"))
    implementation(project(":feature-appmemory"))
    implementation(project(":feature-screentime"))
    implementation(project(":feature-dischargerate"))
    implementation(project(":feature-cpuheatmap"))
    implementation(project(":feature-throttle"))
    implementation(project(":feature-wifichannel"))
    implementation(project(":feature-loadavg"))
    implementation(project(":feature-nettraffic"))
    implementation(project(":feature-chargecurve"))
    implementation(project(":feature-sensorlive"))
    implementation(project(":feature-devcompare"))
    implementation(project(":feature-processmon"))
    implementation(project(":feature-powerestimate"))
    implementation(project(":feature-brightness"))
    implementation(project(":feature-bootspeed"))
    implementation(project(":feature-featurematrix"))
    // ── AndroidX & Material ───────────────────────────────────────────────
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
