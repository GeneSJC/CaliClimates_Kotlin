package io.github.genesjc.climatefinder.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Which labels are held while the viewing angle changes: following the view, or only through a jump. */
class LabelHoldTest {

    private fun hold() = LabelHold(0).apply { lastPlaced = setOf("A", "B") }

    @Test
    fun followingTheViewASmallTurnHoldsTheLabelsThatWereShowing() {
        val h = hold()
        h.angleChanged(10, zoom = 2f, touch = 5L, followView = true)
        assertEquals(setOf("A", "B"), h.ids)
        assertEquals(10, h.angle)
        assertEquals(2f, h.zoom, 0f)
        assertEquals(5L, h.touch)
    }

    @Test
    fun followingTheViewAHoldAlreadyRunningKeepsItsSetWhileTheDialKeepsTurning() {
        val h = hold()
        h.angleChanged(10, zoom = 2f, touch = 5L, followView = true)
        h.lastPlaced = setOf("C")
        h.angleChanged(20, zoom = 2f, touch = 5L, followView = true)
        assertEquals(setOf("A", "B"), h.ids)
        assertEquals(20, h.angle)
    }

    @Test
    fun notFollowingASmallTurnLaysTheLabelsOutAfresh() {
        val h = hold()
        h.angleChanged(10, zoom = 1f, touch = 0L, followView = false)
        assertNull(h.ids)
        assertEquals(10, h.angle)
    }

    @Test
    fun notFollowingAJumpHoldsTheLabelsThroughThePivot() {
        val h = hold()
        h.angleChanged(45, zoom = 1f, touch = 0L, jumpDegrees = 30, followView = false)
        assertEquals(setOf("A", "B"), h.ids)
    }

    @Test
    fun notFollowingTheJumpIsMeasuredFromTheAngleBeforeTheChange() {
        val h = LabelHold(40).apply { lastPlaced = setOf("A") }
        h.angleChanged(50, zoom = 1f, touch = 0L, jumpDegrees = 30, followView = false)
        assertNull(h.ids)
    }
}
