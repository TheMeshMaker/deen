/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.core.prayer

import io.github.themeshmaker.dunya.core.model.AsrMethod
import io.github.themeshmaker.dunya.core.model.CalculationMethod

/**
 * Suggested defaults by country (ISO 3166-1 alpha-2). The user always confirms or changes them
 * during onboarding. Kept deliberately conservative; the religious reviewer should confirm
 * this table before v1.0 (docs/BLUEPRINT.md §7).
 */
object MethodSuggestions {
    private val methodByCountry: Map<String, CalculationMethod> = buildMap {
        listOf("SA", "YE").forEach { put(it, CalculationMethod.UMM_AL_QURA) }
        put("AE", CalculationMethod.DUBAI)
        put("KW", CalculationMethod.KUWAIT)
        put("QA", CalculationMethod.QATAR)
        put("EG", CalculationMethod.EGYPTIAN)
        listOf("PK", "IN", "BD", "AF").forEach { put(it, CalculationMethod.KARACHI) }
        listOf("US", "CA").forEach { put(it, CalculationMethod.NORTH_AMERICA) }
        listOf("GB", "IE").forEach { put(it, CalculationMethod.MOONSIGHTING_COMMITTEE) }
        listOf("SG", "MY", "ID", "BN").forEach { put(it, CalculationMethod.SINGAPORE) }
        put("TR", CalculationMethod.TURKEY)
    }

    /** Countries where most Muslims follow the Hanafi school. */
    private val hanafiCountries = setOf(
        "PK", "IN", "BD", "AF", "TR", "UZ", "TJ", "KZ", "KG", "TM", "BA", "AL", "XK", "MK"
    )

    fun method(countryCode: String?): CalculationMethod =
        countryCode?.uppercase()?.let(methodByCountry::get) ?: CalculationMethod.MUSLIM_WORLD_LEAGUE

    fun asrMethod(countryCode: String?): AsrMethod =
        if (countryCode?.uppercase() in hanafiCountries) AsrMethod.HANAFI else AsrMethod.STANDARD
}
