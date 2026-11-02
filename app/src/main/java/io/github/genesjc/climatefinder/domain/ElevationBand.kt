package io.github.genesjc.climatefinder.domain

/** The plain-language band for a height. Bands are by feet: 100, 500, 1,500, 3,500 and 6,000. */
enum class ElevationBand {
    NEAR_SEA_LEVEL,
    LOWLANDS,
    FOOTHILLS,
    HILLS,
    MOUNTAINS,
    HIGH_COUNTRY
}
