package io.github.genesjc.climatefinder.domain

/**
 * How a station's distance from the selected one changes the way it is drawn: near stations a little larger and
 * solid, far ones smaller and fainter. The input is [StationDistance.depth] (0 = right here, 1 = across the region).
 * The far values are floors, so a distant label stays readable. Factors only; the screens apply them.
 */
object DepthCue {
    private const val NEAR_TEXT = 1.1f
    private const val FAR_TEXT = 0.8f
    private const val FAR_ALPHA = 0.6f
    private const val FAR_DOT = 0.8f

    private fun ease(depth: Float, near: Float, far: Float): Float {
        val d = depth.coerceIn(0f, 1f)
        return near + (far - near) * d
    }

    /** Multiplies the label's text size and its width estimate. */
    fun textScale(depth: Float): Float = ease(depth, NEAR_TEXT, FAR_TEXT)

    /** Multiplies the label's and dot's opacity. */
    fun alpha(depth: Float): Float = ease(depth, 1f, FAR_ALPHA)

    /** Multiplies the dot's radius. */
    fun dotScale(depth: Float): Float = ease(depth, 1f, FAR_DOT)
}
