plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.voicecloner.abcvdx"
    minSdk = 24
    targetSdk = 36
    versionCode = 22
    versionName = "1.0.4"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
      ndk {
        debugSymbolLevel = "FULL"
      }
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
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.text.google.fonts)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  implementation(libs.vico.compose.m3)
  implementation(libs.vico.core)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

tasks.register("incrementVersion") {
    notCompatibleWithConfigurationCache("Modify build.gradle.kts file directly")
    doLast {
        val gradleFile = file("build.gradle.kts")
        if (gradleFile.exists()) {
            var content = gradleFile.readText()
            
            // Find versionCode
            val versionCodeRegex = """versionCode\s*=\s*(\d+)""".toRegex()
            val matchVC = versionCodeRegex.find(content)
            if (matchVC != null) {
                val currentVC = matchVC.groupValues[1].toInt()
                val nextVC = currentVC + 1
                content = content.replaceFirst("versionCode = $currentVC", "versionCode = $nextVC")
                println("SUCCESS: Incremented versionCode from $currentVC to $nextVC")
            } else {
                println("ERROR: Could not find versionCode in build.gradle.kts")
            }
            
            // Find versionName
            val versionNameRegex = """versionName\s*=\s*"([^"]+)"""".toRegex()
            val matchVN = versionNameRegex.find(content)
            if (matchVN != null) {
                val currentVN = matchVN.groupValues[1]
                val parts = currentVN.split(".")
                if (parts.size >= 3) {
                    val patch = parts[2].toIntOrNull() ?: 0
                    val nextVN = "${parts[0]}.${parts[1]}.${patch + 1}"
                    content = content.replaceFirst("""versionName = "$currentVN"""", """versionName = "$nextVN"""")
                    println("SUCCESS: Incremented versionName from $currentVN to $nextVN")
                } else if (parts.size == 2) {
                    val minor = parts[1].toIntOrNull() ?: 0
                    val nextVN = "${parts[0]}.${minor + 1}"
                    content = content.replaceFirst("""versionName = "$currentVN"""", """versionName = "$nextVN"""")
                    println("SUCCESS: Incremented versionName from $currentVN to $nextVN")
                } else {
                    val nextVN = "$currentVN.1"
                    content = content.replaceFirst("""versionName = "$currentVN"""", """versionName = "$nextVN"""")
                    println("SUCCESS: Setup versionName from $currentVN to $nextVN")
                }
            } else {
                println("ERROR: Could not find versionName in build.gradle.kts")
            }
            
            // Ensure applicationId is set correct
            val appIDRegex = """applicationId\s*=\s*"([^"]+)"""".toRegex()
            val matchID = appIDRegex.find(content)
            if (matchID != null) {
                val currentID = matchID.groupValues[1]
                if (currentID != "com.aistudio.voicecloner.abcvdx") {
                    content = content.replaceFirst("""applicationId = "$currentID"""", """applicationId = "com.aistudio.voicecloner.abcvdx"""")
                    println("SUCCESS: Enforced applicationId to com.aistudio.voicecloner.abcvdx (was $currentID)")
                } else {
                    println("SUCCESS: applicationId was already verified correct: $currentID")
                }
            } else {
                println("WARNING: Could not find applicationId line to verify")
            }
            
            gradleFile.writeText(content)
        } else {
            println("ERROR: build.gradle.kts file not found under ${gradleFile.absolutePath}")
        }
    }
}

