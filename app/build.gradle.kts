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

    // Release signing comes from the maintainer's own machine (~/.gradle/gradle.properties), never from
    // this repository: deen.keystore, deen.keystore.password, deen.key.alias, deen.key.password.
    val releaseKeystore = providers.gradleProperty("deen.keystore").orNull
    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = providers.gradleProperty("deen.keystore.password").get()
                keyAlias = providers.gradleProperty("deen.key.alias").get()
                keyPassword = providers.gradleProperty("deen.key.password").get()
            }
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
            signingConfig = signingConfigs.findByName("release")
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

    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(projects.core.alarm)
    implementation(projects.core.audio)
    implementation(projects.core.content)
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    implementation(projects.core.domain)
    implementation(projects.core.location)
    implementation(projects.core.model)
    implementation(projects.core.ui)
    implementation(projects.feature.adhkar)
    implementation(projects.feature.hadith)
    implementation(projects.feature.more)
    implementation(projects.feature.onboarding)
    implementation(projects.feature.qibla)
    implementation(projects.feature.quran)
    implementation(projects.feature.reliability)
    implementation(projects.feature.settings)
    implementation(projects.feature.timetable)
    implementation(projects.feature.today)
    implementation(projects.feature.widget)

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

    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.compose.ui.test.manifest)
}

// Screenshots of the Liquid shell (build/outputs/roborazzi), only when asked for: -Pscreenshots
tasks.withType<Test>().configureEach {
    if (project.hasProperty("screenshots")) {
        systemProperty("roborazzi.test.record", "true")
    } else {
        exclude("**/*ScreenshotTest*")
        failOnNoDiscoveredTests = false
    }
}
