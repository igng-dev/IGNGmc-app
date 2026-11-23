plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

fun signingCredential(name: String): String? =
    providers.environmentVariable(name).orNull?.takeIf { it.isNotBlank() }
        ?: providers.gradleProperty(name).orNull?.takeIf { it.isNotBlank() }

val releaseStoreFile = signingCredential("ANDROID_SIGNING_STORE_FILE")
val releaseStorePassword = signingCredential("ANDROID_SIGNING_STORE_PASSWORD")
val releaseKeyAlias = signingCredential("ANDROID_SIGNING_KEY_ALIAS")
val releaseKeyPassword = signingCredential("ANDROID_SIGNING_KEY_PASSWORD")
val hasReleaseSigning = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }
val requireReleaseSigning = signingCredential("IGNG_REQUIRE_RELEASE_SIGNING")
    ?.equals("true", ignoreCase = true) == true
if (requireReleaseSigning && !hasReleaseSigning) {
    throw GradleException("Release signing is required (IGNG_REQUIRE_RELEASE_SIGNING=true) but signing credentials are missing.")
}

android {
    namespace = "net.igng.mcstatus"
    compileSdk = 36

    defaultConfig {
        applicationId = "net.igng.mcstatus"
        minSdk = 26
        targetSdk = 36
        versionCode = 20302
        versionName = "2.3.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val apiBaseUrl = providers.gradleProperty("IGNG_API_BASE_URL")
            .orElse("https://www.igng.net")
            .get()
            .trimEnd('/')
        val ssoBaseUrl = providers.gradleProperty("IGNG_SSO_BASE_URL")
            .orElse("https://sso.igng.net")
            .get()
            .trimEnd('/')
        val mcBaseUrl = providers.gradleProperty("IGNG_MC_BASE_URL")
            .orElse("https://mc.igng.net")
            .get()
            .trimEnd('/')
        val dashBaseUrl = providers.gradleProperty("IGNG_DASH_BASE_URL")
            .orElse("https://dash.igng.net")
            .get()
            .trimEnd('/')
        val chatBaseUrl = providers.gradleProperty("IGNG_CHAT_BASE_URL")
            .orElse("https://chat.igng.net")
            .get()
            .trimEnd('/')

        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "SSO_BASE_URL", "\"$ssoBaseUrl\"")
        buildConfigField("String", "MC_BASE_URL", "\"$mcBaseUrl\"")
        buildConfigField("String", "DASH_BASE_URL", "\"$dashBaseUrl\"")
        buildConfigField("String", "CHAT_BASE_URL", "\"$chatBaseUrl\"")
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
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

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:network"))
    implementation(project(":core:auth"))
    implementation(project(":core:data"))
    implementation(project(":feature:explore"))
    implementation(project(":feature:status"))
    implementation(project(":feature:map"))
    implementation(project(":feature:chat"))
    implementation(project(":feature:wiki"))
    implementation(project(":feature:player"))
    implementation(project(":feature:ticket"))
    implementation(project(":feature:admin"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)

    testImplementation(libs.junit)
}
