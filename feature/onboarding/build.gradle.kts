plugins {
    alias(libs.plugins.deen.android.feature)
}

android {
    namespace = "io.github.themeshmaker.dunya.feature.onboarding"
}

dependencies {
    implementation(libs.androidx.activity.compose)
}
