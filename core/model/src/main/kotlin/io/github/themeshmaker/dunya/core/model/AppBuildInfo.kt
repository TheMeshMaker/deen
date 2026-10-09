package io.github.themeshmaker.dunya.core.model

/** Where this build is distributed. Each channel is a Gradle product flavor. */
enum class DistributionChannel {
    /** Google Play: may use Google Play services (location, billing). */
    PLAY,

    /** F-Droid / GitHub Releases: no proprietary code at all. */
    FOSS
}

data class AppBuildInfo(val versionName: String, val channel: DistributionChannel, val isDebug: Boolean)
