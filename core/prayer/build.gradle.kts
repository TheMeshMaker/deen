plugins {
    alias(libs.plugins.deen.jvm.library)
}

dependencies {
    api(projects.core.model)
    implementation(libs.adhan)
    implementation(libs.javax.inject)

    testImplementation(libs.kotlinx.serialization.json)
}
