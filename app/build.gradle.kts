plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

val versionCodeOverride =
  providers.gradleProperty("versionCodeOverride").orNull?.toIntOrNull() ?: 2
val versionNameOverride =
  providers.gradleProperty("versionNameOverride").orNull?.takeIf { it.isNotBlank() }
    ?: "1.1.0-beta.1"
val buildChannelOverride =
  providers.gradleProperty("buildChannelOverride").orNull?.takeIf { it.isNotBlank() } ?: "beta"
val githubReleaseRepoOverride =
  providers.gradleProperty("githubReleaseRepoOverride").orNull?.trim().orEmpty()

fun escapeBuildConfigString(value: String): String =
  value.replace("\\", "\\\\").replace("\"", "\\\"")

android {
  namespace = "com.example"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.aistudio.speakcraft.qxmvkp"
    minSdk = 24
    targetSdk = 36
    versionCode = versionCodeOverride
    versionName = versionNameOverride

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    buildConfigField(
      "String",
      "BUILD_CHANNEL",
      "\"${escapeBuildConfigString(buildChannelOverride)}\""
    )
    buildConfigField(
      "String",
      "GITHUB_RELEASE_REPO",
      "\"${escapeBuildConfigString(githubReleaseRepoOverride)}\""
    )
  }

  signingConfigs {
    create("release") {
      val ksPath = System.getenv("KEYSTORE_PATH")
      storeFile = if (!ksPath.isNullOrBlank()) file(ksPath) else rootProject.file("my-upload-key.jks")
      storePassword = System.getenv("STORE_PASSWORD") ?: ""
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD") ?: ""
    }
    create("debugConfig") {
      storeFile = rootProject.file("debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  testOptions {
    unitTests {
      isIncludeAndroidResources = true
      isReturnDefaultValues = true
    }
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

dependencies {
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.serialization.json)

  // CameraX for Video Recording & Body-Language Self-Review
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.androidx.camera.video)

  // Networking & Local Preferences
  implementation(libs.okhttp)
  implementation(libs.androidx.datastore.preferences)

  // Firebase Auth, Firestore & App Check + Credential Manager Google Sign-In
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.firestore)
  implementation(libs.firebase.auth)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.firebase.appcheck.debug)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services)
  implementation(libs.googleid)

  // On-Device AI: MediaPipe Tasks (Vision/Audio) + TensorFlow Lite
  implementation(libs.mediapipe.tasks.vision) {
    exclude(group = "org.tensorflow", module = "tensorflow-lite-api")
  }
  implementation(libs.mediapipe.tasks.audio) {
    exclude(group = "org.tensorflow", module = "tensorflow-lite-api")
  }
  implementation(libs.tensorflow.lite)

  // Unit & Robolectric Compose Tests
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.runner)
  testImplementation(libs.robolectric)
  testImplementation(platform(libs.androidx.compose.bom))
  testImplementation(libs.androidx.compose.ui.test.junit4)

  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)

  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
}
