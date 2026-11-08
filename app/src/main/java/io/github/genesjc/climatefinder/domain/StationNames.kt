package io.github.genesjc.climatefinder.domain

// Words that describe the facility, not the place. "County" goes too ("Yuba County Airport"), except in the
// place name "County Line".
private val FACILITY_WORDS = Regex(
    "\\b(International|Intl|Airport|Airfield|Airpark|Air Park|Air Terminal|" +
        "Municipal|Regional|Metropolitan|Public|County(?!\\s+Line\\b)|Executive|Field|Weather Station|" +
        "Meteorological Site)\\b",
    RegexOption.IGNORE_CASE
)

/** Whether a station's name describes its facility (an airport, a weather station) and not only its place. */
fun hasFacilityWords(stationName: String): Boolean = FACILITY_WORDS.containsMatchIn(stationName)

/**
 * The place part of a station's own name: what comes before a "/", a ",", a " - " or a " Near ", without the facility words and
 * the station codes, up to three words ("Santa Barbara"). The name itself when nothing is left.
 */
fun placeFromStationName(stationName: String): String {
    val cleaned = stationName
        .substringBefore('/')
        .substringBefore(',')
        .substringBefore(" - ")
        .substringBefore(" Near ")
        .replace(FACILITY_WORDS, " ")
        .replace(Regex("\\s+"), " ")
        .trim()
    // Drop station codes such as "Lpf01" or "SDHGPA"
    val words = cleaned.split(" ").filter { word ->
        word.isNotBlank() &&
            word.none { it.isDigit() } &&
            !(word.length >= 4 && word.all { it.isUpperCase() })
    }
    return if (words.isEmpty()) stationName else words.take(3).joinToString(" ")
}

/**
 * The name shown for a station that has a known town: the town first, then the station's own name in brackets
 * ("Jackson (Mount Zion)"). A station whose name already says its town ("Hollister, Ca") keeps it.
 */
fun placeAndStationName(place: String, stationName: String): String =
    if (place.isBlank() || stationName.isBlank() || stationName.contains(place, ignoreCase = true)) {
        stationName
    } else {
        "$place ($stationName)"
    }

/**
 * The name the screens show for a station. A name that describes a facility (an airport, a weather station) is shown as the
 * place alone: the town when the state has one on record, else the place part of the name ("Burbank", never "Bob Hope Airport").
 * Any other name goes with its town as [placeAndStationName] puts it.
 */
fun stationDisplayName(town: String?, stationName: String): String = when {
    !hasFacilityWords(stationName) -> town?.let { placeAndStationName(it, stationName) } ?: stationName
    town != null -> town
    else -> placeFromStationName(stationName)
}
