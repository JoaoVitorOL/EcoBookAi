import com.android.build.api.dsl.ManagedVirtualDevice
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.dagger.hilt.android")
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}

fun localProperty(name: String, defaultValue: String = ""): String =
    localProperties.getProperty(name, defaultValue)

val googleServicesConfigFile = file("google-services.json")
val expectedGoogleServicesPackage = "com.ecobook"

if (
    googleServicesConfigFile.exists() &&
    googleServicesConfigFile.readText().contains("\"package_name\": \"$expectedGoogleServicesPackage\"")
) {
    // Keep Firebase wiring scoped to FCM and only apply the plugin when the
    // project-specific config file is present and matches this app id.
    apply(plugin = "com.google.gms.google-services")
} else if (googleServicesConfigFile.exists()) {
    logger.warn(
        "Skipping com.google.gms.google-services: google-services.json does not " +
            "contain a client for $expectedGoogleServicesPackage."
    )
}

android {
    namespace = "com.ecobook"
    compileSdk = 34

    sourceSets {
        getByName("test") {
            java.srcDirs("src/test/java", "src/test/kotlin")
        }
        getByName("androidTest") {
            java.srcDirs("src/androidTest/java", "src/androidTest/kotlin")
        }
    }

    defaultConfig {
        applicationId = "com.ecobook"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField(
            "String",
            "BACKEND_URL_OVERRIDE",
            "\"${localProperty("backend.url")}\""
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    kapt {
        correctErrorTypes = true
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        animationsDisabled = true
        managedDevices {
            devices {
                // ManagedVirtualDevice in this workspace exposes the stable knobs we use
                // here: device/apiLevel/systemImageSource. The Pixel 6 profile keeps the
                // managed AVD comfortably above the 1GB RAM floor from the original
                // checklist, while AGP handles persistent AVD storage automatically in
                // its managed-device workspace.
                maybeCreate<ManagedVirtualDevice>("pixel6Api34").apply {
                    device = "Pixel 6"
                    apiLevel = 34
                    systemImageSource = "aosp-atd"
                }
            }
            groups {
                maybeCreate("smoke").apply {
                    targetDevices.add(devices["pixel6Api34"])
                }
            }
        }
    }
}

dependencies {
    // Jetpack Core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation("com.google.android.material:material:1.12.0")

    // Jetpack Compose
    val composeBom = platform("androidx.compose:compose-bom:2023.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Jetpack Compose Navigation
    implementation("androidx.navigation:navigation-compose:2.7.3")

    // Jetpack Compose Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")

    // Jetpack Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Retrofit & HTTP Client
    implementation("com.squareup.retrofit2:retrofit:2.10.0")
    implementation("com.squareup.retrofit2:converter-gson:2.10.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // Hilt Dependency Injection
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.12.0"))
    implementation("com.google.firebase:firebase-messaging")

    // Image Loading
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // JSON Processing
    implementation("com.google.code.gson:gson:2.10.1")

    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Unit Tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    // Integration Tests
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
