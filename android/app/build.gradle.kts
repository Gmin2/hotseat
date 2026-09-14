import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}

fun hotseat(name: String, fallback: String): String =
    providers.gradleProperty("hotseat.$name").orNull ?: localProperties.getProperty("hotseat.$name") ?: fallback

android {
    namespace = "dev.mintu.hotseat"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.mintu.hotseat"
        minSdk = 29
        targetSdk = 37
        // ci sets these from the tag, local builds stay at the defaults
        versionCode = hotseat("versionCode", "1").toInt()
        versionName = hotseat("versionName", "0.1.0")

        // the deployed worker and its key live in local.properties (not committed), -P overrides them,
        // and with neither the app talks to wrangler dev on the mac through adb reverse
        buildConfigField("String", "WORKER_URL", "\"${hotseat("workerUrl", "http://127.0.0.1:8790")}\"")
        buildConfigField("String", "APP_KEY", "\"${hotseat("appKey", "dev")}\"")
    }

    signingConfigs {
        create("release") {
            // the keystore never goes in git, locally it is in local.properties, in ci it comes from secrets
            hotseat("keystore", "").takeIf { it.isNotEmpty() }?.let {
                storeFile = rootProject.file(it)
                storePassword = hotseat("keystorePassword", "")
                keyAlias = hotseat("keyAlias", "hotseat")
                keyPassword = hotseat("keyPassword", "")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release").takeIf { it.storeFile != null }
            // phones are arm64, dropping the other webrtc builds takes most of the size off
            ndk { abiFilters += "arm64-v8a" }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.jvmArgs(
                "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED",
                "--add-opens=java.base/java.io=ALL-UNNAMED",
                "--enable-native-access=ALL-UNNAMED",
            )
            it.systemProperty("roborazzi.test.record", "true")
            System.getenv("ROBOLECTRIC_DEPS")?.let { dir ->
                it.systemProperty("robolectric.offline", "true")
                it.systemProperty("robolectric.dependency.dir", dir)
            }
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.webrtc)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.espresso.core)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
}
