package io.github.genesjc.climatefinder.domain.model

/** Outcome of one fetch: a reading, a station with nothing usable right now, or a failed request. */
sealed interface ObservationResult {
    data class Reading(val observation: LiveObservation) : ObservationResult
    object NoReading : ObservationResult
    object Failed : ObservationResult
}
