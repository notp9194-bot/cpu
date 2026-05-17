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
          versionCode = 9
          versionName = "9.0"
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
              isMinifyEnabled = true
              isShrinkResources = true
              signingConfig = signingConfigs.getByName("release")
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
      implementation(project(":feature-network"))
      implementation(project(":feature-camera"))
      implementation(project(":feature-audio"))
      implementation(project(":feature-display"))
      implementation(project(":feature-codec"))
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