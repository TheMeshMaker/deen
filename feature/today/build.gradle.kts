plugins {
    alias(libs.plugins.deen.android.feature)
}

android {
    namespace = "io.github.themeshmaker.dunya.feature.today"
}

dependencies {
    implementation(projects.core.alarm)
}
