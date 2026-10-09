plugins {
    alias(libs.plugins.deen.jvm.library)
}

dependencies {
    api(projects.core.model)
    api(projects.core.prayer)
    api(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)
}
