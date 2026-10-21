package io.github.genesjc.microclimatefinder.data.local

import io.github.genesjc.microclimatefinder.testutil.FakeTerrainSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.coroutines.CoroutineContext

class TerrainRepositoryTest {

    private val source = FakeTerrainSource()
    private var dispatched = 0
    private val io = object : CoroutineDispatcher() {
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            dispatched++
            block.run()
        }
    }
    private val repository = TerrainRepository(source, io)

    @Test
    fun asksTheSourceForTheRegionsPicture() = runBlocking {
        assertNull(repository.terrain("a"))
        assertEquals(listOf("a"), source.terrainLoads)
    }

    @Test
    fun passesTheSkylineThrough() = runBlocking {
        assertNotNull(repository.skyline("b"))
        assertEquals(listOf("b"), source.skylineLoads)
    }

    @Test
    fun loadsTheStatewidePicturesAndTheBackdrop() = runBlocking {
        assertNull(repository.stateTerrain())
        assertNull(repository.backdropRelief())
        assertEquals(1, source.stateLoads)
        assertEquals(1, source.backdropLoads)
    }

    @Test
    fun everyReadGoesThroughTheOwnedDispatcher() = runBlocking {
        repository.terrain("a")
        repository.skyline("a")
        repository.stateTerrain()
        repository.backdropRelief()
        assertEquals(4, dispatched)
    }
}
