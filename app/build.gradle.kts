plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.paytrackr"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.nexus.paytrackr"
        minSdk = 24
        targetSdk = 36
        versionCode = 5
        versionName = "1.1.3"
    }

    signingConfigs {
        create("release") {
            val keystorePath = project.findProperty("PAYTRACKR_KEYSTORE_PATH") as String?
                ?: System.getenv("PAYTRACKR_KEYSTORE_PATH")
            val keystorePassword = project.findProperty("PAYTRACKR_KEYSTORE_PASSWORD") as String?
                ?: System.getenv("PAYTRACKR_KEYSTORE_PASSWORD")
            val keyAlias = project.findProperty("PAYTRACKR_KEY_ALIAS") as String?
                ?: System.getenv("PAYTRACKR_KEY_ALIAS")
            val keyPassword = project.findProperty("PAYTRACKR_KEY_PASSWORD") as String?
                ?: System.getenv("PAYTRACKR_KEY_PASSWORD")

            if (keystorePath != null && file(keystorePath).exists()) {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            } else {
                val debugSigning = getByName("debug")
                storeFile = debugSigning.storeFile
                storePassword = debugSigning.storePassword
                this.keyAlias = debugSigning.keyAlias
                this.keyPassword = debugSigning.keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = true
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation("androidx.compose.material:material-icons-core")
  implementation("androidx.compose.material:material-icons-extended")
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Firebase
  val firebaseBom = platform(libs.firebase.bom)
  implementation(firebaseBom)
  implementation(libs.firebase.auth)
  implementation(libs.firebase.firestore)
  implementation(libs.play.services.auth)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)
}
