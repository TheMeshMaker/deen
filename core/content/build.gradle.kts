plugins {
    alias(libs.plugins.deen.android.library)
    alias(libs.plugins.deen.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.github.themeshmaker.dunya.core.content"
}

dependencies {
    api(projects.core.domain)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
}
