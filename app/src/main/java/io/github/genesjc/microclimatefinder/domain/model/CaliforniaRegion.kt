package io.github.genesjc.microclimatefinder.domain.model

import androidx.annotation.StringRes
import io.github.genesjc.microclimatefinder.R

/**
 * Regional section of California for microclimate transects and geographical constraint definition.
 * Used to partition California into distinct atmospheric and topoclimatic zones. The wording (names, titles,
 * descriptions, traits) is held as string resource ids; the key cities are place names and stay as data.
 */
data class CaliforniaRegion(
    val id: String,
    @StringRes val nameRes: Int,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double,
    val centerLat: Double,
    val centerLon: Double,
    val minElevationMeters: Int,
    val maxElevationMeters: Int,
    val keyCities: List<String>,
    val microclimateTraitRes: List<Int>,
    @StringRes val descriptionRes: Int,
    val status: RegionStatus,
    val activeStationCount: Int = 0,
    val colorHex: String
)

enum class RegionStatus(@StringRes val labelRes: Int) {
    ACTIVE_TRANSECT(R.string.region_status_active),
    PLANNED_CONSTRAINTS(R.string.region_status_planned)
}

object CaliforniaRegionalDefinitions {
    val SAN_DIEGO = CaliforniaRegion(
        id = "san_diego",
        nameRes = R.string.ca_san_diego_name,
        titleRes = R.string.ca_san_diego_title,
        subtitleRes = R.string.ca_san_diego_subtitle,
        minLat = 32.53,
        maxLat = 33.50,
        minLon = -117.60,
        maxLon = -116.08,
        centerLat = 32.85,
        centerLon = -116.95,
        minElevationMeters = 0,
        maxElevationMeters = 1992, // Cuyamaca Peak
        keyCities = listOf("San Diego", "Coronado", "La Jolla", "Ramona", "Mt. Laguna", "Borrego Springs"),
        microclimateTraitRes = listOf(
            R.string.ca_san_diego_trait_1,
            R.string.ca_san_diego_trait_2,
            R.string.ca_san_diego_trait_3,
            R.string.ca_san_diego_trait_4
        ),
        descriptionRes = R.string.ca_san_diego_description,
        status = RegionStatus.PLANNED_CONSTRAINTS,
        activeStationCount = 0,
        colorHex = "#5E7F49"
    )

    val LOS_ANGELES = CaliforniaRegion(
        id = "los_angeles",
        nameRes = R.string.ca_los_angeles_name,
        titleRes = R.string.ca_los_angeles_title,
        subtitleRes = R.string.ca_los_angeles_subtitle,
        minLat = 33.50,
        maxLat = 34.85,
        minLon = -119.05,
        maxLon = -116.90,
        centerLat = 34.10,
        centerLon = -118.25,
        minElevationMeters = 0,
        maxElevationMeters = 3068, // Mt. Baldy / San Antonio
        keyCities = listOf("Santa Monica", "Downtown LA", "Burbank", "Pasadena", "San Bernardino", "Mt. Baldy"),
        microclimateTraitRes = listOf(
            R.string.ca_los_angeles_trait_1,
            R.string.ca_los_angeles_trait_2,
            R.string.ca_los_angeles_trait_3,
            R.string.ca_los_angeles_trait_4
        ),
        descriptionRes = R.string.ca_los_angeles_description,
        status = RegionStatus.PLANNED_CONSTRAINTS,
        activeStationCount = 0,
        colorHex = "#6C86A8"
    )

    val CENTRAL_COAST = CaliforniaRegion(
        id = "central_coast",
        nameRes = R.string.ca_central_coast_name,
        titleRes = R.string.ca_central_coast_title,
        subtitleRes = R.string.ca_central_coast_subtitle,
        minLat = 34.40,
        maxLat = 37.05,
        minLon = -122.00,
        maxLon = -119.50,
        centerLat = 35.60,
        centerLon = -120.80,
        minElevationMeters = 0,
        maxElevationMeters = 1787, // Cone Peak
        keyCities = listOf("Monterey", "Salinas", "Big Sur", "San Luis Obispo", "Paso Robles", "Santa Barbara"),
        microclimateTraitRes = listOf(
            R.string.ca_central_coast_trait_1,
            R.string.ca_central_coast_trait_2,
            R.string.ca_central_coast_trait_3,
            R.string.ca_central_coast_trait_4
        ),
        descriptionRes = R.string.ca_central_coast_description,
        status = RegionStatus.PLANNED_CONSTRAINTS,
        activeStationCount = 0,
        colorHex = "#C9857E"
    )

    val CENTRAL_VALLEY = CaliforniaRegion(
        id = "central_valley",
        nameRes = R.string.ca_central_valley_name,
        titleRes = R.string.ca_central_valley_title,
        subtitleRes = R.string.ca_central_valley_subtitle,
        minLat = 35.00,
        maxLat = 39.00,
        minLon = -121.20,
        maxLon = -118.70,
        centerLat = 36.80,
        centerLon = -119.80,
        minElevationMeters = -3,
        maxElevationMeters = 250,
        keyCities = listOf("Bakersfield", "Fresno", "Madera", "Merced", "Modesto", "Stockton"),
        microclimateTraitRes = listOf(
            R.string.ca_central_valley_trait_1,
            R.string.ca_central_valley_trait_2,
            R.string.ca_central_valley_trait_3,
            R.string.ca_central_valley_trait_4
        ),
        descriptionRes = R.string.ca_central_valley_description,
        status = RegionStatus.PLANNED_CONSTRAINTS,
        activeStationCount = 0,
        colorHex = "#B8733A"
    )

    val BAY_AREA_SACRAMENTO = CaliforniaRegion(
        id = "bay_area_sacramento",
        nameRes = R.string.ca_bay_area_sacramento_name,
        titleRes = R.string.ca_bay_area_sacramento_title,
        subtitleRes = R.string.ca_bay_area_sacramento_subtitle,
        minLat = 36.60,
        maxLat = 39.20,
        minLon = -122.60,
        maxLon = -120.40,
        centerLat = 37.95,
        centerLon = -121.70,
        minElevationMeters = 0,
        maxElevationMeters = 1406, // Blue Canyon
        keyCities = listOf("Monterey", "Santa Cruz", "San Francisco", "Oakland", "Berkeley", "Walnut Creek", "Stockton", "Sacramento", "Colfax", "Blue Canyon"),
        microclimateTraitRes = listOf(
            R.string.ca_bay_area_sacramento_trait_1,
            R.string.ca_bay_area_sacramento_trait_2,
            R.string.ca_bay_area_sacramento_trait_3,
            R.string.ca_bay_area_sacramento_trait_4
        ),
        descriptionRes = R.string.ca_bay_area_sacramento_description,
        status = RegionStatus.ACTIVE_TRANSECT,
        activeStationCount = 36,
        colorHex = "#5F8F7C"
    )

    val NORTHERN_CALIFORNIA = CaliforniaRegion(
        id = "northern_california",
        nameRes = R.string.ca_northern_california_name,
        titleRes = R.string.ca_northern_california_title,
        subtitleRes = R.string.ca_northern_california_subtitle,
        minLat = 39.00,
        maxLat = 42.00,
        minLon = -124.45,
        maxLon = -119.99,
        centerLat = 40.70,
        centerLon = -122.30,
        minElevationMeters = 0,
        maxElevationMeters = 4322, // Mt. Shasta
        keyCities = listOf("Eureka", "Crescent City", "Redding", "Red Bluff", "Mt. Shasta City", "Truckee", "South Lake Tahoe"),
        microclimateTraitRes = listOf(
            R.string.ca_northern_california_trait_1,
            R.string.ca_northern_california_trait_2,
            R.string.ca_northern_california_trait_3,
            R.string.ca_northern_california_trait_4
        ),
        descriptionRes = R.string.ca_northern_california_description,
        status = RegionStatus.PLANNED_CONSTRAINTS,
        activeStationCount = 0,
        colorHex = "#7C6A8C"
    )

    val ALL_REGIONS = listOf(
        NORTHERN_CALIFORNIA,
        BAY_AREA_SACRAMENTO,
        CENTRAL_COAST,
        CENTRAL_VALLEY,
        LOS_ANGELES,
        SAN_DIEGO
    )
}
