plugins {
    alias(libs.plugins.deen.android.library)
    alias(libs.plugins.deen.android.library.compose)
}

android {
    namespace = "io.github.themeshmaker.dunya.core.designsystem"
}

dependencies {
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.ui)
    implementation(libs.androidx.core.ktx)
    implementation(projects.core.model)
}
