plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.example.jellyfintv"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.example.jellyfintv"
        minSdk = 30
        targetSdk = 37
        // CI (see .github/workflows/release.yml) overrides these from the pushed git tag so
        // each GitHub Release's APK reports a versionName the in-app updater (AppUpdateManager)
        // can actually compare against; local/debug builds fall back to these defaults.
        versionCode = System.getenv("APP_VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = System.getenv("APP_VERSION_NAME") ?: "1.0"
    }

    // Release signing comes entirely from environment variables so the keystore and its
    // passwords never need to live in this file or in version control - see
    // .github/workflows/release.yml, which decodes the RELEASE_KEYSTORE_BASE64 secret to a
    // file and sets these before invoking Gradle. Building `release` locally without them set
    // just produces an unsigned APK rather than failing the whole script evaluation (so
    // `assembleDebug`/`test` keep working for anyone without access to the signing secrets).
    val releaseKeystoreFile = System.getenv("RELEASE_KEYSTORE_FILE")
    val releaseKeystorePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
    val releaseKeyAlias = System.getenv("RELEASE_KEY_ALIAS")
    val releaseKeyPassword = System.getenv("RELEASE_KEY_PASSWORD")
    val hasReleaseSigningEnv = listOf(releaseKeystoreFile, releaseKeystorePassword, releaseKeyAlias, releaseKeyPassword)
        .all { !it.isNullOrBlank() }

    signingConfigs {
        if (hasReleaseSigningEnv) {
            create("release") {
                storeFile = file(releaseKeystoreFile!!)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigningEnv) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = false
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
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
  implementation(libs.androidx.compose.material.icons.extended)
  
  // TV Compose
  implementation(libs.androidx.tv.foundation)
  implementation(libs.androidx.tv.material)

  // Networking & JSON
  implementation(libs.retrofit.core)
  implementation(libs.retrofit.converter.gson)
  implementation(libs.okhttp.core)
  implementation(libs.okhttp.logging)

  // Image Loading
  implementation(libs.coil.compose)

  // Media3 ExoPlayer
  implementation(libs.media3.exoplayer)
  implementation(libs.media3.exoplayer.hls)
  implementation(libs.media3.ui)
  implementation(libs.media3.datasource.okhttp)

  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, mocking, fake HTTP server, Flow assertions
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.mockk)
  testImplementation(libs.okhttp.mockwebserver)
  testImplementation(libs.turbine)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)
}

