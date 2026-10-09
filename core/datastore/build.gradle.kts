plugins {
    alias(libs.plugins.deen.android.library)
    alias(libs.plugins.deen.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.github.themeshmaker.dunya.core.datastore"
}

dependencies {
    api(projects.core.model)
    api(libs.androidx.datastore)
    implementation(libs.kotlinx.serialization.json)
}
