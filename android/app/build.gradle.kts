import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val signingProperties = Properties()
val signingFile = rootProject.file("keystore/keystore.properties")
if (signingFile.exists()) signingFile.inputStream().use(signingProperties::load)

android {
    namespace = "net.igng.mcstatus"
    compileSdk = 36

    defaultConfig {
        applicationId = "net.igng.mcstatus"
        minSdk = 26
        targetSdk = 36
        versionCode = 20200
        versionName = "2.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val apiBaseUrl = providers.gradleProperty("IGNG_API_BASE_URL")
            .orElse("https://www.igng.net")
            .get()
            .trimEnd('/')
        val mcBaseUrl = providers.gradleProperty("IGNG_MC_BASE_URL")
            .orElse(providers.gradleProperty("MC_STATUS_BASE_URL"))
            .orElse("https://mc.igng.net")
            .get()
            .trimEnd('/')
        val dashBaseUrl = providers.gradleProperty("IGNG_DASH_BASE_URL")
            .orElse("https://dash.igng.net")
            .get()
            .trimEnd('/')
        val ssoBaseUrl = providers.gradleProperty("IGNG_SSO_BASE_URL")
            .orElse("https://sso.igng.net")
            .get()
            .trimEnd('/')
        val chatBaseUrl = providers.gradleProperty("IGNG_CHAT_BASE_URL")
            .orElse("https://chat.igng.net")
            .get()
            .trimEnd('/')
        val wikiBaseUrl = providers.gradleProperty("IGNG_WIKI_BASE_URL")
            .orElse(providers.gradleProperty("WIKI_BASE_URL"))
            .orElse("https://wiki.igng.net")
            .get()
            .trimEnd('/')
        val mapRelayUrl = providers.gradleProperty("IGNG_MAP_RELAY_URL")
            .orElse("https://map-api.igng.net")
            .get()
            .trimEnd('/')

        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "MC_BASE_URL", "\"$mcBaseUrl\"")
        buildConfigField("String", "DASH_BASE_URL", "\"$dashBaseUrl\"")
        buildConfigField("String", "SSO_BASE_URL", "\"$ssoBaseUrl\"")
        buildConfigField("String", "CHAT_BASE_URL", "\"$chatBaseUrl\"")
        buildConfigField("String", "WIKI_BASE_URL", "\"$wikiBaseUrl\"")
        buildConfigField("String", "MAP_RELAY_URL", "\"$mapRelayUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (signingProperties.isNotEmpty()) {
                signingConfig = signingConfigs.create("release") {
                    storeFile = rootProject.file("keystore/${signingProperties.getProperty("storeFile")}")
                    storePassword = signingProperties.getProperty("storePassword")
                    keyAlias = signingProperties.getProperty("keyAlias")
                    keyPassword = signingProperties.getProperty("keyPassword")
                }
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
