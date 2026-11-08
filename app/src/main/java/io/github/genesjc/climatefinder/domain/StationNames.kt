package io.github.genesjc.climatefinder.domain

// Words that describe the facility, not the place. "County" goes too ("Yuba County Airport"), except in the
// place name "County Line".
private val FACILITY_WORDS = Regex(
    "\\b(International|Intl|Airport|Airfield|Airpark|Air Park|Air Terminal|" +
        "Municipal|Regional|Metropolitan|Public|County(?!\\s+Line\\b)|Executive|Field|Weather Station|" +
        "Meteorological Site)\\b",
    RegexOption.IGNORE_CASE
)

// Codes and fittings that name a piece of equipment or an agency, not a place ("Vnc Station 54", "Uscrn Site", "Sacata Repeater")
private val CODE_WORDS = Regex(
    "\\b(Raws|Uscrn|Btaws|Iraws|Nwr|Vnc|Vor|Tower|Repeater|Portable|Booster|Met|Wx|Sdhgpa|Cfa|Powerplant|Gage)\\b",
    RegexOption.IGNORE_CASE
)

/** Whether a station's name describes its facility (an airport, a weather station) and not only its place. */
fun hasFacilityWords(stationName: String): Boolean = FACILITY_WORDS.containsMatchIn(stationName)

/**
 * Whether a station's name is a facility, an agency code or a numbered site (any digit) and so says little about the place: such a
 * name is shown as the place alone (see [stationDisplayName]).
 */
fun isFacilityOrCodeName(stationName: String): Boolean =
    hasFacilityWords(stationName) || CODE_WORDS.containsMatchIn(stationName) || stationName.any { it.isDigit() }

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
 * ("Jackson (Mount Zion)"). A station whose name already says its town ("Hollister, Ca") keeps it, and a town that already holds the
 * station's name stands alone ("West Santa Monica", not "West Santa Monica (Santa Monica)").
 */
fun placeAndStationName(place: String, stationName: String): String = when {
    place.isBlank() || stationName.isBlank() -> stationName
    stationName.contains(place, ignoreCase = true) -> stationName
    place.contains(stationName, ignoreCase = true) -> place
    else -> "$place ($stationName)"
}

/**
 * The name the screens show for a station. A name that describes a facility (an airport, a weather station), is an agency code
 * ("Vnc Station 54") or carries a site number is shown as the place alone: the town when the state has one on record, else the place part of the name ("Burbank", never "Bob Hope Airport").
 * Any other name goes with its town as [placeAndStationName] puts it.
 */
fun stationDisplayName(town: String?, stationName: String): String = when {
    !isFacilityOrCodeName(stationName) -> town?.let { placeAndStationName(it, stationName) } ?: stationName
    town != null -> town
    else -> placeFromStationName(stationName)
}
