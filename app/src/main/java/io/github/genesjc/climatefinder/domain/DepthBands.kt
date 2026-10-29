package io.github.genesjc.climatefinder.domain

/**
 * The Elevation view's layers: stations grouped by latitude into bands, like looking north across the region (band 0
 * is the southern third, drawn in front; the last band is the northern third, behind), each band drawn as its own
 * ridge, smaller, higher and hazier the farther back it is. The bands depend only on the stations, so selecting
 * another station does not move them. Factors and positions only; the screens draw them.
 */
object DepthBands {
    const val COUNT = 3

    private val SQUASH = floatArrayOf(1f, 0.85f, 0.7f)
    private val HAZE = floatArrayOf(0f, 0.3f, 0.45f)
    private val LINE = floatArrayOf(1f, 0.75f, 0.5f)

    /** How much of the lift (see [layeredY]) a band gets, as a share of the room it leaves above itself. */
    private const val LIFT_SHARE = 0.8f

    /**
     * The band of each station, given their latitudes in the stations' own order: the southernmost third is band 0
     * (front), the northernmost the last band (back). Split by rank, so each band gets about the same number of stations.
     */
    fun byLatitude(latitudes: List<Double>): IntArray {
        val order = latitudes.indices.sortedBy { latitudes[it] }
        val bands = IntArray(latitudes.size)
        order.forEachIndexed { rank, index -> bands[index] = rank * COUNT / latitudes.size }
        return bands
    }

    /** Multiplies the band's heights: the far ridges are lower on the page. */
    fun squash(band: Int): Float = SQUASH[band.coerceIn(0, COUNT - 1)]

    /** Opacity of the haze laid over the band's ground (0 for the nearest). */
    fun haze(band: Int): Float = HAZE[band.coerceIn(0, COUNT - 1)]

    /** Opacity of the band's ridge line and station dots. */
    fun lineFade(band: Int): Float = LINE[band.coerceIn(0, COUNT - 1)]

    /**
     * Where a station's pin goes on the page. [y] is its place in the plain profile; the band squashes the height
     * toward [seaY] and lifts the whole ridge, so the far bands sit higher and behind, and no band rises above [topY].
     * Band 0 is the plain profile.
     */
    fun layeredY(y: Float, band: Int, topY: Float, seaY: Float): Float {
        val squash = squash(band)
        return seaY - (seaY - y) * squash - (1f - squash) * (seaY - topY) * LIFT_SHARE
    }
}
