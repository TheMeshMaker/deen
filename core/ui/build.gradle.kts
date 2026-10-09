plugins {
    alias(libs.plugins.deen.android.library)
    alias(libs.plugins.deen.android.library.compose)
}

android {
    namespace = "io.github.themeshmaker.dunya.core.ui"
}

dependencies {
    api(projects.core.designsystem)
    api(projects.core.model)
}
