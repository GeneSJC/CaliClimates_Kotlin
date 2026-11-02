package io.github.genesjc.climatefinder.data.remote

/** Outcome of one fetch: a reading, a station with nothing usable right now, or a failed request. */
sealed interface ObservationResult {
    data class Reading(val observation: LiveObservation) : ObservationResult
    object NoReading : ObservationResult
    object Failed : ObservationResult
}
