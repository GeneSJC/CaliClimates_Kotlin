package io.github.genesjc.climatefinder.data.remote

/**
 * The rule for how long a result may be reused before it is asked for again. A real reading is reused for 15 minutes
 * (NWS stations report about hourly). Anything else (a station with no usable reading, a failed request) is reused for
 * 5, so a broken station is not asked again at once but is not written off for long either.
 */
object ReadingFreshness {
    const val READING_MS = 15 * 60_000L
    const val NEGATIVE_MS = 5 * 60_000L

    /** Whether a [result] received [ageMs] milliseconds ago may still be used. */
    fun isFresh(result: ObservationResult, ageMs: Long): Boolean =
        ageMs < if (result is ObservationResult.Reading) READING_MS else NEGATIVE_MS
}
