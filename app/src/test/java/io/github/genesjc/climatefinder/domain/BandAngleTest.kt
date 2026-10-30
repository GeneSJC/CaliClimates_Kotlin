package io.github.genesjc.climatefinder.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BandAngleTest {

    @Test
    fun theEndsAreTheTwoAxes() {
        assertEquals(BandAxis.NORTH_SOUTH.degrees.toInt(), BandAngle.NORTH)
        assertEquals(BandAxis.WEST_EAST.degrees.toInt(), BandAngle.WEST)
        assertEquals(BandAxis.NORTH_SOUTH, BandAngle.nearestAxis(BandAngle.NORTH))
        assertEquals(BandAxis.WEST_EAST, BandAngle.nearestAxis(BandAngle.WEST))
    }

    @Test
    fun halfwayCountsAsWest() {
        assertEquals(BandAxis.NORTH_SOUTH, BandAngle.nearestAxis(44))
        assertEquals(BandAxis.WEST_EAST, BandAngle.nearestAxis(45))
    }

    @Test
    fun theAngleIsRoundedAndHeldInsideTheRange() {
        assertEquals(0, BandAngle.clamp(-12f))
        assertEquals(90, BandAngle.clamp(140f))
        assertEquals(20, BandAngle.clamp(19.6f))
        assertEquals(19, BandAngle.clamp(19.4f))
    }

    @Test
    fun aDraggedAngleSnapsOntoTheDetentsWithinThePull() {
        assertEquals(45, BandAngle.snap(42f))
        assertEquals(45, BandAngle.snap(47.9f))
        assertEquals(0, BandAngle.snap(2.6f))
        assertEquals(90, BandAngle.snap(87.4f))
    }

    @Test
    fun anAngleOutsideThePullIsLeftAlone() {
        assertEquals(41, BandAngle.snap(41f))
        assertEquals(49, BandAngle.snap(49f))
        assertEquals(10, BandAngle.snap(10f))
    }

    @Test
    fun onlyTheThreeDetentsAreDetents() {
        assertTrue(BandAngle.isDetent(0) && BandAngle.isDetent(45) && BandAngle.isDetent(90))
        assertFalse(BandAngle.isDetent(44))
    }

    @Test
    fun theSwitchGoesToTheFarEndFromTheNearerOne() {
        assertEquals(90, BandAngle.otherEnd(0))
        assertEquals(90, BandAngle.otherEnd(44))
        assertEquals(0, BandAngle.otherEnd(45))
        assertEquals(0, BandAngle.otherEnd(90))
    }

    @Test
    fun theAngleIsToldInFiveCompassNames() {
        assertEquals(listOf(0, 0, 1, 1, 2, 2, 3, 3, 4, 4), listOf(0, 11, 12, 33, 34, 56, 57, 78, 79, 90).map { BandAngle.point(it) })
        assertEquals(BandAngle.POINTS - 1, BandAngle.point(BandAngle.WEST))
        assertEquals(2, BandAngle.point(BandAngle.MIDDLE))
    }

    @Test
    fun theKeyIsNamedByTheNearestOfThreeLooks() {
        assertEquals(listOf(0, 0, 1, 1, 1, 2, 2), listOf(0, 22, 23, 45, 67, 68, 90).map { BandAngle.nameIndex(it) })
    }

    @Test
    fun movingOntoOrPastAMarkTicksIt() {
        assertEquals(BandAngle.Tick.LIGHT, BandAngle.tickBetween(29, 30))
        assertEquals(BandAngle.Tick.LIGHT, BandAngle.tickBetween(31, 29))
        assertEquals(BandAngle.Tick.LIGHT, BandAngle.tickBetween(14, 16))
        assertEquals(BandAngle.Tick.DETENT, BandAngle.tickBetween(44, 45))
        assertEquals(BandAngle.Tick.DETENT, BandAngle.tickBetween(1, 0))
        assertEquals(BandAngle.Tick.DETENT, BandAngle.tickBetween(89, 90))
    }

    @Test
    fun leavingAMarkDoesNotTickAndNeitherDoesMovingBetweenMarks() {
        assertEquals(BandAngle.Tick.NONE, BandAngle.tickBetween(30, 29))
        assertEquals(BandAngle.Tick.NONE, BandAngle.tickBetween(30, 31))
        assertEquals(BandAngle.Tick.NONE, BandAngle.tickBetween(20, 25))
        assertEquals(BandAngle.Tick.NONE, BandAngle.tickBetween(45, 45))
    }

    @Test
    fun aQuickDragOverSeveralMarksGivesTheStrongestTick() {
        assertEquals(BandAngle.Tick.DETENT, BandAngle.tickBetween(40, 62))
        assertEquals(BandAngle.Tick.LIGHT, BandAngle.tickBetween(16, 44))
        assertEquals(BandAngle.Tick.DETENT, BandAngle.tickBetween(80, 20))
    }
}
