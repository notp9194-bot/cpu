plugins {
      alias(libs.plugins.android.library)
      alias(libs.plugins.kotlin.android)
  }
  android {
      namespace = "com.example.deviceinfo.feature.display"
      compileSdk = 35
      defaultConfig { minSdk = 24 }
      compileOptions { sourceCompatibility = JavaVersion.VERSION_1_8; targetCompatibility = JavaVersion.VERSION_1_8 }
      kotlinOptions { jvmTarget = "1.8" }
      buildFeatures { viewBinding = true }
  }
  dependencies {
      implementation(project(":core"))
      implementation(libs.androidx.core.ktx)
      implementation(libs.androidx.appcompat)
      implementation(libs.material)
  }