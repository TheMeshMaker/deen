plugins {
    alias(libs.plugins.deen.android.application)
    alias(libs.plugins.deen.android.application.compose)
    alias(libs.plugins.deen.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.github.themeshmaker.dunya"

    defaultConfig {
        applicationId = "io.github.themeshmaker.dunya"
        versionCode = 1
        versionName = "0.1.0"
    }

    // See docs/BLUEPRINT.md §5. "foss" must never pull in proprietary code; CI checks this.
    flavorDimensions += "distribution"
    productFlavors {
        create("play") {
            dimension = "distribution"
        }
        create("foss") {
            dimension = "distribution"
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    androidResources {
        generateLocaleConfig = true
    }

    // The dependency-metadata blob is encrypted with a Google key; F-Droid asks apps to omit it.
    dependenciesInfo {
        includeInApk = false
    }

    lint {
        checkDependencies = true
        abortOnError = true
    }
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    implementation(projects.core.domain)
    implementation(projects.core.location)
    implementation(projects.core.model)
    implementation(projects.feature.adhkar)
    implementation(projects.feature.more)
    implementation(projects.feature.onboarding)
    implementation(projects.feature.qibla)
    implementation(projects.feature.tasbih)
    implementation(projects.feature.today)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.core)

    // Google Play services are only ever linked into the Play build.
    "playImplementation"(projects.core.locationGms)
}
