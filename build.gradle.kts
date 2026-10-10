plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.spotless)
}

spotless {
    // Matches .gitattributes; avoids Spotless scanning git attributes (breaks the configuration cache).
    lineEndings = com.diffplug.spotless.LineEnding.UNIX
    // Only source folders: walking build/ races with tasks that write there in the same build.
    kotlin {
        target("app/src/**/*.kt", "core/*/src/**/*.kt", "feature/*/src/**/*.kt", "build-logic/*/src/**/*.kt")
        ktlint(libs.versions.ktlint.get())
    }
    kotlinGradle {
        target("*.gradle.kts", "*/*.gradle.kts", "*/*/*.gradle.kts")
        ktlint(libs.versions.ktlint.get())
    }
}
