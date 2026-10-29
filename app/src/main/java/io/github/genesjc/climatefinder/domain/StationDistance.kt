package io.github.genesjc.climatefinder.domain

import io.github.genesjc.climatefinder.domain.model.MicroclimateStation
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * How far apart stations are on the ground, which the Elevation view cannot show by itself (it orders stations by
 * height only). Facts only: miles and a share of the region's size; the screens decide how to show them.
 */
object StationDistance {
    private const val EARTH_RADIUS_MILES = 3_958.8

    /** Great-circle distance between two points, in miles. */
    fun miles(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val p1 = Math.toRadians(lat1)
        val p2 = Math.toRadians(lat2)
        val dLat = p2 - p1
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(p1) * cos(p2) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * EARTH_RADIUS_MILES * atan2(sqrt(a), sqrt(1 - a))
    }

    fun miles(from: MicroclimateStation, to: MicroclimateStation): Double =
        miles(from.latitude, from.longitude, to.latitude, to.longitude)

    /** The region's size: the corner-to-corner distance of the box around its stations, in miles. */
    fun spanMiles(stations: List<MicroclimateStation>): Double {
        if (stations.size < 2) return 0.0
        return miles(
            stations.minOf { it.latitude }, stations.minOf { it.longitude },
            stations.maxOf { it.latitude }, stations.maxOf { it.longitude }
        )
    }

    /**
     * How far a station is from the chosen one as a share of the region's size: 0 is the chosen station's own spot,
     * 1 is as far as the region goes. A region of one place (no span) has no depth.
     */
    fun depth(miles: Double, spanMiles: Double): Float =
        if (spanMiles <= 0.0) 0f else min(1.0, max(0.0, miles / spanMiles)).toFloat()

    /** Each station's distance from [from], in whole miles, by station id. */
    fun milesFrom(from: MicroclimateStation, stations: List<MicroclimateStation>): Map<String, Int> =
        stations.filter { it.id != from.id }.associate { it.id to miles(from, it).roundToInt() }
}
