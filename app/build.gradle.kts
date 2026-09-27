import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.roborazzi)
}

// Release signing comes from environment variables (CI secrets) or, for local builds, from a
// keystore.properties file next to settings.gradle.kts. Neither the keystore nor its passwords
// are ever committed. Without them, release builds are simply unsigned.
val keystoreProperties = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
// Trimmed: secrets pasted into GitHub often pick up a trailing line break, which would make an
// otherwise correct password fail.
fun signingValue(name: String): String? =
    (System.getenv(name) ?: keystoreProperties.getProperty(name))?.trim()?.takeIf { it.isNotEmpty() }

android {
    namespace = "com.expensetracker.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dev40.kharcha"
        minSdk = 26
        targetSdk = 35
        // CI passes an increasing number for Play uploads; local builds use 1.
        versionCode = System.getenv("KHARCHA_VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = "1.0.0"
    }

    signingConfigs {
        val storePath = signingValue("KHARCHA_KEYSTORE_FILE")
        if (storePath != null) {
            create("release") {
                storeFile = file(storePath)
                storePassword = signingValue("KHARCHA_KEYSTORE_PASSWORD")
                keyAlias = signingValue("KHARCHA_KEY_ALIAS")
                keyPassword = signingValue("KHARCHA_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        // Robolectric renders real Compose output, so it needs the merged resources and manifest.
        unitTests.isIncludeAndroidResources = true
    }
}

roborazzi {
    // Golden images live in the test source set, so they show up in review like any other file.
    // The tests pass matching explicit file names; this tells the Gradle tasks where to look.
    outputDir.set(layout.projectDirectory.dir("src/test/screenshots"))
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation("com.expensetracker:core")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)

    // Supplies the ComponentActivity that createComposeRule() launches.
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
