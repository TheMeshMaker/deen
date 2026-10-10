plugins {
    alias(libs.plugins.deen.android.library)
    alias(libs.plugins.deen.hilt)
}

android {
    namespace = "io.github.themeshmaker.dunya.feature.widget"
}

dependencies {
    implementation(projects.core.alarm)
    implementation(projects.core.domain)
    implementation(projects.core.ui)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
}
