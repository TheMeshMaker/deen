plugins {
    alias(libs.plugins.deen.android.library)
    alias(libs.plugins.deen.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.github.themeshmaker.dunya.core.alarm"
}

dependencies {
    api(projects.core.domain)
    implementation(projects.core.datastore)
    implementation(projects.core.ui)
    // :core:ui exposes Compose artifacts without versions; the BOM pins them.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.work.runtime)
    implementation(libs.kotlinx.serialization.json)
}
