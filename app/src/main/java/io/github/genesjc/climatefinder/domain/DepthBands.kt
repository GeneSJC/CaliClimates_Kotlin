package io.github.genesjc.climatefinder.domain

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The direction the Elevation view looks across the region, as an angle from north toward west: 0 degrees looks north
 * (the north is at the back, the south in front), 90 degrees looks west (the west is at the back, the east in front).
 * The two values are the two ends of a continuous range, so a later control can offer the angles between them.
 */
enum class BandAxis(val degrees: Double) {
    NORTH_SOUTH(0.0),
    WEST_EAST(90.0);

    /** The other axis, for a switch between the two. */
    fun other(): BandAxis = if (this == NORTH_SOUTH) WEST_EAST else NORTH_SOUTH

    /** How far back a station is when looking along this axis: bigger is farther back (see [DepthBands.depthKey]). */
    fun depthKey(latitude: Double, longitude: Double): Double = DepthBands.depthKey(latitude, longitude, degrees)
}

/**
 * The viewing angle as the control and the saved setting hold it: whole degrees from 0 (north at the back) to 90 (west at
 * the back). The two ends are the [BandAxis] values; the angles between them are the halfway views.
 */
object BandAngle {
    const val NORTH = 0
    const val MIDDLE = 45
    const val WEST = 90

    /** How close (degrees) a dragged angle has to come to 0, 45 or 90 to snap onto it. */
    const val DETENT_PULL = 3

    private val DETENTS = intArrayOf(NORTH, MIDDLE, WEST)

    /** The nearest whole degree, held inside the range. */
    fun clamp(degrees: Float): Int = degrees.roundToInt().coerceIn(NORTH, WEST)

    /** The dragged angle as the chart takes it: whole degrees, snapped onto a detent when within [DETENT_PULL] of one. */
    fun snap(degrees: Float): Int {
        val whole = clamp(degrees)
        return DETENTS.firstOrNull { abs(it - whole) <= DETENT_PULL } ?: whole
    }

    fun isDetent(degrees: Int): Boolean = degrees in DETENTS

    /** The spacing of the light ticks along the slider (degrees); the detents are every third of them. */
    const val TICK_EVERY = 15

    /** The feel of moving the slider from one angle to another. */
    enum class Tick { NONE, LIGHT, DETENT }

    /**
     * What the slider should feel like moving from [from] to [to]: a [Tick.DETENT] when it reaches or passes 0, 45 or 90, a
     * [Tick.LIGHT] when it reaches or passes any other multiple of [TICK_EVERY], nothing otherwise. A mark counts when it is
     * entered or passed, not when it is left, and a quick drag that jumps over several still gives one tick, the strongest.
     */
    fun tickBetween(from: Int, to: Int): Tick {
        if (from == to) return Tick.NONE
        val lo = minOf(from, to)
        val hi = maxOf(from, to)
        var tick = Tick.NONE
        var mark = (lo + TICK_EVERY - 1) / TICK_EVERY * TICK_EVERY
        while (mark <= hi) {
            if (mark != from) {
                if (isDetent(mark)) return Tick.DETENT
                tick = Tick.LIGHT
            }
            mark += TICK_EVERY
        }
        return tick
    }

    /** The end the angle is nearer to, for what only has two looks (the key's colours and the scenery): halfway counts as west. */
    fun nearestAxis(degrees: Int): BandAxis = if (degrees < MIDDLE) BandAxis.NORTH_SOUTH else BandAxis.WEST_EAST

    /** The compass names the angle is told in, in order: north, north-northwest, northwest, west-northwest, west. */
    const val POINTS = 5

    /** Which of the [POINTS] compass names the angle is nearest to (0 north up to 4 west, each covering 22.5 degrees). */
    fun point(degrees: Int): Int = ((degrees.coerceIn(NORTH, WEST) + 11.25) / 22.5).toInt().coerceIn(0, POINTS - 1)

    /**
     * Which of three names tells the angle: 0 for the north-south end (the key reads South, Central, North), 2 for the west-east
     * end (East, Central, West) and 1 between them (Southeast, Central, Northwest), the middle one covering 23 to 67 degrees.
     */
    fun nameIndex(degrees: Int): Int = when {
        degrees < 23 -> 0
        degrees > 67 -> 2
        else -> 1
    }

}

/**
 * The Elevation view's layers: stations grouped into bands along a [BandAxis], like looking across the region (band 0
 * is the front third, the last band is the back third; looking north, the south is in front and the north behind),
 * each band drawn as its own ridge, smaller, higher and hazier the farther back it is. The bands depend only on the
 * stations and the axis, so selecting another station does not move them. Factors and positions only; the screens
 * draw them.
 */
object DepthBands {
    const val COUNT = 3

    private val SQUASH = floatArrayOf(1f, 0.85f, 0.7f)
    private val HAZE = floatArrayOf(0f, 0.3f, 0.45f)
    private val LINE = floatArrayOf(1f, 0.75f, 0.5f)

    /**
     * How much of the lift (see [layeredY]) a band gets, as a share of the range between the sea and the top. Above 1 the lift is
     * enough that the summit of each band behind stands clear above the summit of the one in front (about 7 percent of the range
     * for each band, and the back band's summit about 15 percent above the front band's), so the bands never cover one another
     * and the margin that gives the sense of depth holds in every region; the room for it is the sky above the highest station.
     */
    private const val LIFT_SHARE = 1.5f

    /**
     * How far back a station is when looking in the direction [degrees] (0 = north at the back, 90 = west at the back):
     * latitude times cos plus minus longitude times sin, so a bigger value is farther back. West longitudes are
     * negative in the Americas, which is why the longitude is negated. At exactly 0 and 90 degrees the key is the plain
     * latitude and the plain minus longitude.
     */
    fun depthKey(latitude: Double, longitude: Double, degrees: Double): Double {
        val (c, s) = when (degrees) {
            0.0 -> 1.0 to 0.0
            90.0 -> 0.0 to 1.0
            else -> Math.toRadians(degrees).let { cos(it) to sin(it) }
        }
        return latitude * c + (-longitude) * s
    }

    /**
     * How far across the line of sight a station stands when looking in the direction [degrees]: bigger is farther to the
     * right. Looking north (0) the right is east, so it is the longitude; looking west (90) the right is north, so it is the
     * latitude. It is at right angles to [depthKey], so the two together place a station on the land seen from that way.
     */
    fun lateralKey(latitude: Double, longitude: Double, degrees: Double): Double {
        val (c, s) = when (degrees) {
            0.0 -> 1.0 to 0.0
            90.0 -> 0.0 to 1.0
            else -> Math.toRadians(degrees).let { cos(it) to sin(it) }
        }
        return longitude * c + latitude * s
    }

    /**
     * The band of each station, given their depth keys in the stations' own order: the smallest third is band 0
     * (front), the largest the last band (back). Split by rank, so each band gets about the same number of stations.
     */
    fun byRank(keys: List<Double>): IntArray {
        val order = keys.indices.sortedBy { keys[it] }
        val bands = IntArray(keys.size)
        order.forEachIndexed { rank, index -> bands[index] = rank * COUNT / keys.size }
        return bands
    }

    /** Multiplies the band's heights: the far ridges are lower on the page. */
    fun squash(band: Int): Float = SQUASH[band.coerceIn(0, COUNT - 1)]

    /** The layers that do not hold the selected station are subdued: their lines and dots at this opacity, ... */
    const val INACTIVE_FADE = 0.7f

    /** ... and their ground under a wash of this opacity (white-ish), so the active layer stands out. */
    const val INACTIVE_WASH = 0.3f

    /** Opacity of the haze laid over the band's ground (0 for the nearest). */
    fun haze(band: Int): Float = HAZE[band.coerceIn(0, COUNT - 1)]

    /** Opacity of the band's ridge line and station dots. */
    fun lineFade(band: Int): Float = LINE[band.coerceIn(0, COUNT - 1)]

    /**
     * Where a station's pin goes on the page. [y] is its place in the plain profile; the band squashes the height
     * toward [seaY] and lifts the whole ridge, so the far bands sit higher and behind, each band's summit clear above the one in
     * front (the back band's rises above [topY] by about 15 percent of the range, into the sky). Band 0 is the plain profile.
     */
    fun layeredY(y: Float, band: Int, topY: Float, seaY: Float): Float {
        val squash = squash(band)
        return seaY - (seaY - y) * squash - (1f - squash) * (seaY - topY) * LIFT_SHARE
    }
}
