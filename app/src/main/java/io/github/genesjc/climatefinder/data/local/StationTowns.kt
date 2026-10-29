package io.github.genesjc.climatefinder.data.local

import io.github.genesjc.climatefinder.domain.placeAndStationName

// Words that describe the facility, not the place. "County" goes too ("Yuba County Airport"), except in the
// place name "County Line".
private val FACILITY_WORDS = Regex(
    "\\b(International|Intl|Airport|Airfield|Airpark|Air Park|Air Terminal|Air Force Base|" +
        "Municipal|Regional|Metropolitan|Public|County(?!\\s+Line\\b)|Executive|Field|Weather Station|" +
        "Meteorological Site)\\b",
    RegexOption.IGNORE_CASE
)

/**
 * Station names as the app shows them: the town a station is in, and the cleaned-up station name where there is no
 * town on record. The table of towns is the USA state's ([io.github.genesjc.climatefinder.domain.model.StateUsaDefinition.stationTowns]);
 * the container builds this over the registered definition's table. (The local news code that once shared this file is
 * parked; see docs/news-restore.md.)
 */
class StationTowns(private val towns: Map<String, String>) {

    /** The short name the maps' labels use: the station's town when the state has one on record, else its cleaned name. */
    fun getCleanCityName(stationId: String, stationName: String): String =
        mappedCity(stationId) ?: cityFromStationName(stationName)

    /** The name shown on the chip, cards and lists: the town and the station's own name together, where they differ. */
    fun displayName(stationId: String, stationName: String): String =
        mappedCity(stationId)?.let { placeAndStationName(it, stationName) } ?: stationName

    private fun mappedCity(stationId: String): String? = towns[stationId]

    /** Keeps the place part of a station name, up to three words (for example "Santa Barbara"). */
    private fun cityFromStationName(stationName: String): String {
        val cleaned = stationName
            .substringBefore('/')
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
}
