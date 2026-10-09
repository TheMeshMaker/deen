plugins {
    alias(libs.plugins.deen.android.library)
    alias(libs.plugins.deen.hilt)
}

android {
    namespace = "io.github.themeshmaker.dunya.core.data"
}

dependencies {
    api(projects.core.domain)
    implementation(projects.core.datastore)
    implementation(projects.core.location)
}
