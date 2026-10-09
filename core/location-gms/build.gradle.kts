// Google Play services location. Only the `play` flavor may depend on this module (CI enforces it).
plugins {
    alias(libs.plugins.deen.android.library)
    alias(libs.plugins.deen.hilt)
}

android {
    namespace = "io.github.themeshmaker.dunya.core.location.gms"
}

dependencies {
    implementation(projects.core.location)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.play.services.location)
}
