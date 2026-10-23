package io.github.genesjc.microclimatefinder.data.local

import android.content.Context
import android.content.SharedPreferences
import io.github.genesjc.microclimatefinder.domain.model.StateUsaCA
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [AppSettingsPreferences] over a mocked SharedPreferences. The mock keeps the written values in a map, so a value saved
 * through one instance can be read back through another, like the file across app launches.
 */
class AppSettingsPreferencesTest {

    private val values = HashMap<String, Any>()
    private val editor = mockk<SharedPreferences.Editor>()
    private val prefs = mockk<SharedPreferences>()

    init {
        every { prefs.getString(any(), any()) } answers { (values[firstArg<String>()] as String?) ?: secondArg<String?>() }
        every { prefs.getInt(any(), any()) } answers { (values[firstArg<String>()] as Int?) ?: secondArg<Int>() }
        every { prefs.getFloat(any(), any()) } answers { (values[firstArg<String>()] as Float?) ?: secondArg<Float>() }
        every { prefs.getBoolean(any(), any()) } answers { (values[firstArg<String>()] as Boolean?) ?: secondArg<Boolean>() }
        every { prefs.edit() } returns editor
        every { editor.putString(any(), any()) } answers {
            val value = secondArg<String?>()
            if (value == null) values.remove(firstArg<String>()) else values[firstArg<String>()] = value
            editor
        }
        every { editor.putInt(any(), any()) } answers { values[firstArg<String>()] = secondArg<Int>(); editor }
        every { editor.putFloat(any(), any()) } answers { values[firstArg<String>()] = secondArg<Float>(); editor }
        every { editor.putBoolean(any(), any()) } answers { values[firstArg<String>()] = secondArg<Boolean>(); editor }
        every { editor.apply() } just Runs
    }

    private val north = StateUsaCA.NORTHERN_CALIFORNIA.id
    private val bay = StateUsaCA.BAY_AREA_SACRAMENTO.id

    /** A new instance over the same preferences: the app after a restart. */
    private fun launch() = AppSettingsPreferences(prefs, StateUsaCA)

    @Test
    fun opensItsOwnFileInPrivateMode() {
        val context = mockk<Context>()
        every { context.getSharedPreferences(any(), any()) } returns prefs
        AppSettingsPreferences(context, StateUsaCA)
        verify(exactly = 1) { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }
    }

    @Test
    fun theRegionDefaultsToTheBayAreaUntilOneIsSaved() {
        assertEquals(bay, launch().selectedRegionId())
    }

    @Test
    fun aSavedRegionComesBackAfterARestart() {
        launch().saveSelectedRegionId(north)
        assertEquals(north, launch().selectedRegionId())
    }

    @Test
    fun aSavedRegionThatNoLongerExistsFallsBackToTheDefault() {
        launch().saveSelectedRegionId("gone")
        assertEquals(bay, launch().selectedRegionId())
    }

    @Test
    fun theStationComesBackOnlyForTheRegionItWasSavedIn() {
        launch().saveSelectedStationId(north, "KXYZ")
        val settings = launch()
        assertEquals("KXYZ", settings.selectedStationId(north))
        assertNull(settings.selectedStationId(bay))
    }

    @Test
    fun savingNoStationForgetsIt() {
        val settings = launch()
        settings.saveSelectedStationId(north, "KXYZ")
        settings.saveSelectedStationId(north, null)
        assertNull(launch().selectedStationId(north))
    }

    @Test
    fun theZoomComesBackOnlyForTheRegionItWasSetIn() {
        launch().saveMapZoom(north, 3f)
        val settings = launch()
        assertEquals(3f, settings.mapZoom(north))
        assertEquals(1f, settings.mapZoom(bay))
    }

    @Test
    fun theStationAndTheZoomAreRememberedIndependently() {
        val settings = launch()
        settings.saveSelectedStationId(north, "KXYZ")
        settings.saveMapZoom(bay, 2f)
        assertEquals("KXYZ", settings.selectedStationId(north))
        assertEquals(1f, settings.mapZoom(north))
        assertEquals(2f, settings.mapZoom(bay))
    }

    @Test
    fun theLookStartsLightAndKeepsTheChoice() {
        assertFalse(launch().darkMode())
        launch().saveDarkMode(true)
        assertTrue(launch().darkMode())
    }

    @Test
    fun theHintShowsOnTheFirstThreeLaunchesAndThenStops() {
        repeat(3) {
            val settings = launch()
            settings.countElevationHintLaunch()
            assertTrue(settings.shouldShowElevationHint())
        }
        val fourth = launch()
        fourth.countElevationHintLaunch()
        assertFalse(fourth.shouldShowElevationHint())
    }

    @Test
    fun aLaunchIsCountedOnceHoweverOftenTheViewIsOpened() {
        val settings = launch()
        settings.countElevationHintLaunch()
        settings.countElevationHintLaunch()
        settings.countElevationHintLaunch()
        verify(exactly = 1) { editor.putInt(any(), any()) }
    }

    @Test
    fun tappingTheLineStopsTheHintForGoodAndIsWrittenOnce() {
        val settings = launch()
        settings.markElevationLineTapped()
        settings.markElevationLineTapped()
        verify(exactly = 1) { editor.putBoolean(any(), true) }
        assertFalse(launch().shouldShowElevationHint())
    }
}
