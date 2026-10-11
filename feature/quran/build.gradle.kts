plugins {
    alias(libs.plugins.deen.android.feature)
}

android {
    namespace = "io.github.themeshmaker.dunya.feature.quran"
}

dependencies {
    implementation(projects.core.audio)
}
