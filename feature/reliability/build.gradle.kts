plugins {
    alias(libs.plugins.deen.android.feature)
}

android {
    namespace = "io.github.themeshmaker.dunya.feature.reliability"
}

dependencies {
    implementation(projects.core.alarm)
    implementation(libs.androidx.activity.compose)
}
