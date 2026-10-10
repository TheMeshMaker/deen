package io.github.themeshmaker.dunya.core.model

/** In-app destinations that notifications and the widget can open (an Intent extra on the launch intent). */
object DeenLinks {
    const val EXTRA_OPEN = "io.github.themeshmaker.dunya.extra.OPEN"

    private const val ADHKAR_PREFIX = "adhkar/"

    fun adhkar(categoryId: String): String = ADHKAR_PREFIX + categoryId

    /** The adhkar category in [link], if it is an adhkar link. */
    fun adhkarCategory(link: String?): String? = link?.takeIf { it.startsWith(ADHKAR_PREFIX) }?.removePrefix(ADHKAR_PREFIX)
}
