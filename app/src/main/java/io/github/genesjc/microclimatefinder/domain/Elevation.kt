package io.github.genesjc.microclimatefinder.domain

import androidx.annotation.StringRes
import io.github.genesjc.microclimatefinder.R
import kotlin.math.roundToInt

/** The plain-language band for a height. Bands are by feet: 100, 500, 1,500, 3,500 and 6,000. */
enum class ElevationBand(@StringRes val labelRes: Int) {
    NEAR_SEA_LEVEL(R.string.elevation_band_near_sea_level),
    LOWLANDS(R.string.elevation_band_lowlands),
    FOOTHILLS(R.string.elevation_band_foothills),
    HILLS(R.string.elevation_band_hills),
    MOUNTAINS(R.string.elevation_band_mountains),
    HIGH_COUNTRY(R.string.elevation_band_high_country)
}

/** A landmark a height is compared with: [isNear] when within about 20% of its height, else the height is above it. */
data class LandmarkComparison(val name: String, val feet: Int, val isNear: Boolean)

/** What is worth saying about a height: a landmark comparison, if any, and the rule-of-thumb cooling in degrees F. */
data class ElevationNote(val landmark: LandmarkComparison?, val coolerFahrenheit: Int)

/**
 * The facts about an elevation. The data stays in metres; the user reads feet, in a plain-language band. This object
 * returns facts only (numbers, a band, a landmark); the words are string resources chosen by the screens.
 */
object Elevation {
    private const val FEET_PER_METER = 3.28084

    fun feet(meters: Int): Int = (meters * FEET_PER_METER).roundToInt()

    /** The band for a height. */
    fun band(meters: Int): ElevationBand = when (feet(meters)) {
        in Int.MIN_VALUE until 100 -> ElevationBand.NEAR_SEA_LEVEL
        in 100 until 500 -> ElevationBand.LOWLANDS
        in 500 until 1_500 -> ElevationBand.FOOTHILLS
        in 1_500 until 3_500 -> ElevationBand.HILLS
        in 3_500 until 6_000 -> ElevationBand.MOUNTAINS
        else -> ElevationBand.HIGH_COUNTRY
    }

    /** A well-known place and its height in feet (approximate). */
    private class Landmark(val name: String, val feet: Int)

    // Approximate heights, for comparisons in plain words
    private val LANDMARKS: Map<String, List<Landmark>> = mapOf(
        "bay_area_sacramento" to listOf(
            Landmark("Twin Peaks", 925), Landmark("Mt. Tamalpais", 2_570), Landmark("Mt. Diablo", 3_850),
            Landmark("Mt. Hamilton", 4_210), Landmark("Lake Tahoe", 6_225)
        ),
        "los_angeles" to listOf(
            Landmark("Griffith Observatory", 1_130), Landmark("the Hollywood Sign", 1_600),
            Landmark("Mt. Wilson", 5_710), Landmark("Big Bear Lake", 6_750), Landmark("Mt. Baldy", 10_060)
        ),
        "san_diego" to listOf(
            Landmark("Mt. Soledad", 820), Landmark("Julian", 4_200), Landmark("Cuyamaca Peak", 6_510)
        ),
        "central_coast" to listOf(
            Landmark("Morro Rock", 580), Landmark("the Pinnacles High Peaks", 3_300),
            Landmark("Junipero Serra Peak", 5_860)
        ),
        "central_valley" to listOf(
            Landmark("Fresno", 300), Landmark("the Yosemite Valley floor", 4_000), Landmark("Half Dome", 8_840)
        ),
        "northern_california" to listOf(
            Landmark("Redding", 500), Landmark("Lassen Peak", 10_460), Landmark("Mt. Shasta", 14_180)
        )
    )

    /** Rule of thumb: the air cools about 3.5 \u00B0F for every 1,000 ft of height. */
    fun coolerFahrenheit(meters: Int): Int = (feet(meters) / 1000.0 * 3.5).roundToInt()

    /**
     * The facts for a sentence about the height: a landmark comparison (only when a landmark is within about 20% of
     * the height, or else the highest one it is above) and the rule-of-thumb cooling.
     */
    fun note(meters: Int, regionId: String?): ElevationNote {
        val ft = feet(meters)
        val landmarks = LANDMARKS[regionId].orEmpty()
        val near = landmarks.minByOrNull { kotlin.math.abs(it.feet - ft) }
            ?.takeIf { ft > 0 && kotlin.math.abs(it.feet - ft) <= 0.2 * it.feet }
        val landmark = when {
            near != null -> LandmarkComparison(near.name, near.feet, isNear = true)
            else -> landmarks.filter { it.feet < ft }.maxByOrNull { it.feet }
                ?.let { LandmarkComparison(it.name, it.feet, isNear = false) }
        }
        return ElevationNote(landmark, coolerFahrenheit(meters))
    }
}
