package com.example.kotlinapp

import com.example.kotlinapp.data.model.SeasonalTheme
import com.example.kotlinapp.data.model.WasteStatus
import com.example.kotlinapp.data.model.WoodWasteItem
import com.example.kotlinapp.data.repository.PilotPreferences
import org.junit.Assert.*
import org.junit.Test

class PilotPreferencesTest {
    @Test
    fun automaticCalendarUsesAllFourSeasonsAndWrapsYear() {
        assertEquals(SeasonalTheme.SNOW, PilotPreferences.seasonForMonth(1))
        assertEquals(SeasonalTheme.SPRING, PilotPreferences.seasonForMonth(3))
        assertEquals(SeasonalTheme.SUMMER, PilotPreferences.seasonForMonth(8))
        assertEquals(SeasonalTheme.AUTUMN, PilotPreferences.seasonForMonth(11))
        assertEquals(SeasonalTheme.SNOW, PilotPreferences.seasonForMonth(12))
    }

    @Test
    fun manualChoiceOverridesCalendar() {
        assertEquals(SeasonalTheme.AUTUMN, PilotPreferences.resolvedSeason("AUTUMN", 6))
        assertEquals(SeasonalTheme.SPRING, PilotPreferences.resolvedSeason("AUTO", 4))
    }

    @Test
    fun unknownStatusDoesNotBecomeAvailableAndConditionDoesNotReserve() {
        assertEquals(WasteStatus.INACTIVO, WoodWasteItem(status = "unknown").typedStatus)
        assertEquals(
            WasteStatus.HALLAZGO,
            WoodWasteItem(status = "HALLAZGO", materialState = "Dañado").typedStatus,
        )
        assertFalse(WoodWasteItem(status = "HALLAZGO", materialState = "Dañado").isReserved)
    }
}
