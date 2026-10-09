plugins {
    alias(libs.plugins.deen.android.library)
    alias(libs.plugins.deen.hilt)
}

android {
    namespace = "io.github.themeshmaker.dunya.core.location"
}

dependencies {
    api(projects.core.model)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
}
