package io.github.genesjc.climatefinder.domain.model

import io.github.genesjc.climatefinder.config.StateUsaCA
import io.github.genesjc.climatefinder.config.StateUsaDefinition
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

/**
 * The opening view's decorative dots must read as a scatter inside the state: enough of them, all inside the outline with room to
 * spare, none crowding another. They are not stations, so nothing here looks at station data.
 */
class IntroDotsTest {

    private val definitions: List<StateUsaDefinition> = listOf(StateUsaCA)

    // Longitude degrees are narrower than latitude degrees, so distances scale longitude by this (as the drawing does)
    private val lonScale = 0.8

    private fun StateUsaDefinition.border(): List<Pair<Double, Double>> =
        (outline.coast + outline.landBorder).map { (lon, lat) -> lon * lonScale to lat }

    private fun inside(x: Double, y: Double, border: List<Pair<Double, Double>>): Boolean {
        var in_ = false
        for (i in border.indices) {
            val (x1, y1) = border[i]
            val (x2, y2) = border[(i + 1) % border.size]
            if ((y1 > y) != (y2 > y) && x < (x2 - x1) * (y - y1) / (y2 - y1) + x1) in_ = !in_
        }
        return in_
    }

    private fun distanceToEdge(x: Double, y: Double, border: List<Pair<Double, Double>>): Double {
        var best = Double.MAX_VALUE
        for (i in border.indices) {
            val (ax, ay) = border[i]
            val (bx, by) = border[(i + 1) % border.size]
            val dx = bx - ax
            val dy = by - ay
            val length = dx * dx + dy * dy
            val t = if (length == 0.0) 0.0 else (((x - ax) * dx + (y - ay) * dy) / length).coerceIn(0.0, 1.0)
            best = minOf(best, hypot(x - ax - t * dx, y - ay - t * dy))
        }
        return best
    }

    @Test
    fun thereAreEnoughDotsButNotTooMany() {
        for (d in definitions) assertTrue("${d.code} has ${d.introDots.size} dots", d.introDots.size in 40..80)
    }

    @Test
    fun everyDotIsInsideTheOutlineWithRoomToSpare() {
        for (d in definitions) {
            val border = d.border()
            for ((lon, lat) in d.introDots) {
                val x = lon * lonScale
                assertTrue("${d.code} dot $lon, $lat is outside the outline", inside(x, lat, border))
                assertTrue("${d.code} dot $lon, $lat is too close to the edge", distanceToEdge(x, lat, border) >= 0.15)
            }
        }
    }

    @Test
    fun noTwoDotsCrowdEachOther() {
        for (d in definitions) {
            val dots = d.introDots
            for (i in dots.indices) for (j in i + 1 until dots.size) {
                val gap = hypot((dots[i].first - dots[j].first) * lonScale, dots[i].second - dots[j].second)
                assertTrue("${d.code} dots ${dots[i]} and ${dots[j]} are only $gap apart", gap >= 0.3)
            }
        }
    }
}
