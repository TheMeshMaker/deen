plugins {
    alias(libs.plugins.deen.android.feature)
}

android {
    namespace = "io.github.themeshmaker.dunya.feature.today"
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(projects.core.alarm)

    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.compose.ui.test.manifest)
}

// Screenshots of Today in every style (build/outputs/roborazzi), only when asked for: -Pscreenshots
tasks.withType<Test>().configureEach {
    if (project.hasProperty("screenshots")) {
        systemProperty("roborazzi.test.record", "true")
    } else {
        exclude("**/*ScreenshotTest*")
        failOnNoDiscoveredTests = false
    }
}
