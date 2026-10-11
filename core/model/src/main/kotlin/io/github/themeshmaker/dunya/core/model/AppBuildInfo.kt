package io.github.themeshmaker.dunya.core.model

/** Where this build is distributed. Each channel is a Gradle product flavor. */
enum class DistributionChannel {
    /** Google Play: may use Google Play services (location, billing). */
    PLAY,

    /** F-Droid / GitHub Releases: no proprietary code at all. */
    FOSS
}

data class AppBuildInfo(val versionName: String, val channel: DistributionChannel, val isDebug: Boolean) {
    /**
     * Google Play only allows donations through Play Billing, so the outside donation link is
     * offered in the FOSS build only.
     */
    val canLinkToDonations: Boolean get() = channel == DistributionChannel.FOSS
}

/** The project's public pages. */
object ProjectLinks {
    const val REPOSITORY = "https://github.com/TheMeshMaker/deen"
    const val RELEASES = "$REPOSITORY/releases"
    const val ISSUES = "$REPOSITORY/issues"
    const val DONATE = "https://github.com/sponsors/TheMeshMaker"
}
