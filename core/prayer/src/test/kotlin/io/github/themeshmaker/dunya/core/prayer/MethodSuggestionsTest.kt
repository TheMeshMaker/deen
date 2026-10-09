package io.github.themeshmaker.dunya.core.prayer

import io.github.themeshmaker.dunya.core.model.AsrMethod
import io.github.themeshmaker.dunya.core.model.CalculationMethod
import org.junit.Assert.assertEquals
import org.junit.Test

class MethodSuggestionsTest {
    @Test
    fun knownCountries() {
        assertEquals(CalculationMethod.UMM_AL_QURA, MethodSuggestions.method("SA"))
        assertEquals(CalculationMethod.KARACHI, MethodSuggestions.method("pk"))
        assertEquals(CalculationMethod.NORTH_AMERICA, MethodSuggestions.method("US"))
        assertEquals(AsrMethod.HANAFI, MethodSuggestions.asrMethod("PK"))
        assertEquals(AsrMethod.STANDARD, MethodSuggestions.asrMethod("SA"))
    }

    @Test
    fun unknownOrMissingCountryFallsBackToMwlAndStandardAsr() {
        assertEquals(CalculationMethod.MUSLIM_WORLD_LEAGUE, MethodSuggestions.method("FR"))
        assertEquals(CalculationMethod.MUSLIM_WORLD_LEAGUE, MethodSuggestions.method(null))
        assertEquals(AsrMethod.STANDARD, MethodSuggestions.asrMethod(null))
    }
}
