package io.github.genesjc.microclimatefinder.data.local

import io.github.genesjc.microclimatefinder.domain.placeAndStationName

/**
 * Station names as the app shows them: the town a station is in, and the cleaned-up station name where there is no
 * town on record. (This object also held the local news code, which is parked; see docs/news-restore.md.)
 */
object StationTowns {

    /** The short name the maps' labels use: the station's town when it has one in the list below, else its cleaned name. */
    fun getCleanCityName(stationId: String, stationName: String): String =
        mappedCity(stationId) ?: cityFromStationName(stationName)

    /** The name shown on the chip, cards and lists: the town and the station's own name together, where they differ. */
    fun displayName(stationId: String, stationName: String): String =
        mappedCity(stationId)?.let { placeAndStationName(it, stationName) } ?: stationName

    private fun mappedCity(stationId: String): String? {
        return when (stationId) {
            "KVCB" -> "Vacaville"
            "KSUU" -> "Fairfield"
            "KSMF", "KSAC" -> "Sacramento"
            "KMHR" -> "Rancho Cordova"
            "KEDU" -> "Davis"
            "KLHM" -> "Lincoln"
            "KAUN" -> "Auburn"
            "KMYV" -> "Marysville"
            "KSCK" -> "Stockton"
            "KPAO" -> "Palo Alto"
            "KHAF" -> "Half Moon Bay"
            "MTHC1" -> "San Jose"
            "SCZC1" -> "Santa Cruz"
            "KWVI" -> "Watsonville"
            "MSLC1" -> "Moss Landing"
            "KSNS" -> "Salinas"
            "KMRY" -> "Monterey"
            "PMSC1" -> "Pacific Grove"
            "CARC1", "CHWC1" -> "Carmel Valley"
            "BIGC1" -> "Big Sur"
            "PINC1" -> "Paicines"
            "KCVH" -> "Hollister"
            "KPRB" -> "Paso Robles"
            "PLTC1" -> "Pilot Hill"
            "BENC1" -> "Georgetown"
            "BDMC1" -> "Foresthill"
            "KGOO" -> "Grass Valley"
            "SETC1" -> "Colfax"
            "RRRC1" -> "Penn Valley"
            "FSHC1" -> "Chico"
            "BNGC1" -> "Oroville"
            "BKSC1" -> "Brooks"
            "MTZC1" -> "Jackson"
            "ESPC1" -> "Placerville"
            else -> null
        }
    }

    // Words that describe the facility, not the place
    private val FACILITY_WORDS = Regex(
        "\\b(International|Intl|Airport|Airfield|Airpark|Air Park|Air Terminal|Air Force Base|" +
            "Municipal|Regional|Metropolitan|Public|County|Executive|Field|Weather Station|" +
            "Meteorological Site)\\b",
        RegexOption.IGNORE_CASE
    )

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
