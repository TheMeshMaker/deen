plugins {
    alias(libs.plugins.deen.android.library)
    alias(libs.plugins.deen.hilt)
}

android {
    namespace = "io.github.themeshmaker.dunya.core.audio"
}

dependencies {
    api(projects.core.model)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.datasource)
    implementation(libs.androidx.media3.database)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.guava)
}
