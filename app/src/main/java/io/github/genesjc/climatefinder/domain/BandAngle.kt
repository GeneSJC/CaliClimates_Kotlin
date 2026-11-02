package io.github.genesjc.climatefinder.domain

import kotlin.math.abs
import kotlin.math.roundToInt

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
