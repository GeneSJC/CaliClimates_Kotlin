package io.github.genesjc.microclimatefinder.domain

/**
 * The name shown for a station that has a known town: the town first, then the station's own name in brackets
 * ("Jackson (Mount Zion)"). A station whose name already says its town ("Sacramento Executive Airport") keeps it.
 */
fun placeAndStationName(place: String, stationName: String): String =
    if (place.isBlank() || stationName.isBlank() || stationName.contains(place, ignoreCase = true)) {
        stationName
    } else {
        "$place ($stationName)"
    }
