package io.github.genesjc.microclimatefinder.domain.model

/**
 * Regional section of California for microclimate transects and geographical constraint definition.
 * Used to partition California into distinct atmospheric and topoclimatic zones.
 */
data class CaliforniaRegion(
    val id: String,
    val name: String,
    val title: String,
    val subtitle: String,
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double,
    val centerLat: Double,
    val centerLon: Double,
    val minElevationMeters: Int,
    val maxElevationMeters: Int,
    val keyCities: List<String>,
    val microclimateTraits: List<String>,
    val description: String,
    val status: RegionStatus,
    val activeStationCount: Int = 0,
    val colorHex: String
)

enum class RegionStatus(val label: String) {
    ACTIVE_TRANSECT("Active Transect"),
    PLANNED_CONSTRAINTS("Planned Regional Constraints")
}

object CaliforniaRegionalDefinitions {
    val SAN_DIEGO = CaliforniaRegion(
        id = "san_diego",
        name = "San Diego",
        title = "San Diego & South Coast",
        subtitle = "Border Marine Layer to Anza-Borrego Desert",
        minLat = 32.53,
        maxLat = 33.50,
        minLon = -117.60,
        maxLon = -116.08,
        centerLat = 32.85,
        centerLon = -116.95,
        minElevationMeters = 0,
        maxElevationMeters = 1992, // Cuyamaca Peak
        keyCities = listOf("San Diego", "Coronado", "La Jolla", "Ramona", "Mt. Laguna", "Borrego Springs"),
        microclimateTraits = listOf(
            "Persistent May Gray / June Gloom marine stratus",
            "Sharp inland heating over coastal mesas and chaparral",
            "Cuyamaca / Laguna Mountain pine-oak temperate transition",
            "Rain-shadow drop into extreme Colorado/Sonoran Desert"
        ),
        description = "South Coast transect spanning coastal ocean waters, inland mesas, alpine pine peaks, and dropping 5,000 feet into the Anza-Borrego desert rain shadow within a 50-mile drive.",
        status = RegionStatus.PLANNED_CONSTRAINTS,
        activeStationCount = 0,
        colorHex = "#15803D"
    )

    val LOS_ANGELES = CaliforniaRegion(
        id = "los_angeles",
        name = "Los Angeles Area",
        title = "Los Angeles & Inland Empire",
        subtitle = "Santa Monica Basin to San Gabriel Crest",
        minLat = 33.50,
        maxLat = 34.85,
        minLon = -119.05,
        maxLon = -116.90,
        centerLat = 34.10,
        centerLon = -118.25,
        minElevationMeters = 0,
        maxElevationMeters = 3068, // Mt. Baldy / San Antonio
        keyCities = listOf("Santa Monica", "Downtown LA", "Burbank", "Pasadena", "San Bernardino", "Mt. Baldy"),
        microclimateTraits = listOf(
            "Severe marine layer entrapment beneath thermal inversion lids",
            "Pass-driven heating across Sepulveda & Cahuenga into San Fernando Valley",
            "Inland Empire high diurnal swing and thermal inertia",
            "Subalpine pine zones across 10,000-foot San Gabriel peaks"
        ),
        description = "Dense coastal urban basin bounded by sheer transverse mountain walls that generate severe marine layer thermal capping and sharp 30°F gradients across individual ridgelines.",
        status = RegionStatus.PLANNED_CONSTRAINTS,
        activeStationCount = 0,
        colorHex = "#2563EB"
    )

    val CENTRAL_COAST = CaliforniaRegion(
        id = "central_coast",
        name = "Central Coast",
        title = "Central Coast & Valleys",
        subtitle = "Big Sur Cliffs to Salinas Valley Fog Funnel",
        minLat = 34.40,
        maxLat = 37.05,
        minLon = -122.00,
        maxLon = -119.50,
        centerLat = 35.60,
        centerLon = -120.80,
        minElevationMeters = 0,
        maxElevationMeters = 1787, // Cone Peak
        keyCities = listOf("Monterey", "Salinas", "Big Sur", "San Luis Obispo", "Paso Robles", "Santa Barbara"),
        microclimateTraits = listOf(
            "Steepest coastal elevation gradient in contiguous US (Cone Peak 5,155 ft within 3 miles)",
            "Salinas Valley agricultural marine wind and fog funnel",
            "Paso Robles extreme diurnal swings (up to 50°F day-night shift)",
            "Santa Ynez maritime-to-continental wine microclimates"
        ),
        description = "World-renowned maritime upwelling zones and steep Santa Lucia coastal scarps channeling cold marine air deep into interior agricultural valleys.",
        status = RegionStatus.PLANNED_CONSTRAINTS,
        activeStationCount = 0,
        colorHex = "#DB2777"
    )

    val CENTRAL_VALLEY = CaliforniaRegion(
        id = "central_valley",
        name = "Central Valley",
        title = "San Joaquin & Central Valley",
        subtitle = "Interior Basin, Tule Fog & Extreme Diurnal Cycles",
        minLat = 35.00,
        maxLat = 39.00,
        minLon = -121.20,
        maxLon = -118.70,
        centerLat = 36.80,
        centerLon = -119.80,
        minElevationMeters = -3,
        maxElevationMeters = 250,
        keyCities = listOf("Bakersfield", "Fresno", "Madera", "Merced", "Modesto", "Stockton"),
        microclimateTraits = listOf(
            "Interior continental summer heat basin (frequent 100°F+ temperatures)",
            "Winter radiation inversion generating dense Tule radiation fog",
            "Pronounced day-to-night temperature swings (35°F to 45°F)",
            "Dry adiabatic lapse rate along Sierra foothills"
        ),
        description = "Expansive 400-mile flat interior basin protected by coastal ranges and the Sierra Nevada, characterized by high thermal accumulation and severe winter radiation inversions.",
        status = RegionStatus.PLANNED_CONSTRAINTS,
        activeStationCount = 0,
        colorHex = "#C2410C"
    )

    val BAY_AREA_SACRAMENTO = CaliforniaRegion(
        id = "bay_area_sacramento",
        name = "Bay Area / Sacramento",
        title = "Bay Area & Sacramento",
        subtitle = "Golden Gate Gap to Sierra Foothills (Active Transect)",
        minLat = 36.60,
        maxLat = 39.20,
        minLon = -122.60,
        maxLon = -120.40,
        centerLat = 37.95,
        centerLon = -121.70,
        minElevationMeters = 0,
        maxElevationMeters = 1406, // Blue Canyon
        keyCities = listOf("Monterey", "Santa Cruz", "San Francisco", "Oakland", "Berkeley", "Walnut Creek", "Stockton", "Sacramento", "Colfax", "Blue Canyon"),
        microclimateTraits = listOf(
            "Active NOAA transect of real-time recording stations",
            "Golden Gate ocean cold-water marine surge into the Delta Breeze",
            "Diablo Range rain shadow and thermal inland transition",
            "Lapse-rate cooling up Western Sierra Nevada slope to 1406m"
        ),
        description = "The primary baseline transect: fully unified draggable and zoomable coordinate space covering stations from Pacific tidepools to alpine ridge tops.",
        status = RegionStatus.ACTIVE_TRANSECT,
        activeStationCount = 36,
        colorHex = "#0E7C86"
    )

    val NORTHERN_CALIFORNIA = CaliforniaRegion(
        id = "northern_california",
        name = "Northern CA",
        title = "Northern California & Cascades",
        subtitle = "Redwood Temperate Rainforest to Mt. Shasta",
        minLat = 39.00,
        maxLat = 42.00,
        minLon = -124.45,
        maxLon = -119.99,
        centerLat = 40.70,
        centerLon = -122.30,
        minElevationMeters = 0,
        maxElevationMeters = 4322, // Mt. Shasta
        keyCities = listOf("Eureka", "Crescent City", "Redding", "Red Bluff", "Mt. Shasta City", "Truckee", "South Lake Tahoe"),
        microclimateTraits = listOf(
            "Hyper-maritime coastal redwood rain forest (70+ inches annual rain)",
            "Extreme inland heat funnel in northern Sacramento Valley at Redding",
            "Glaciated alpine microclimates on 14,179 ft Mt. Shasta",
            "High-elevation Great Basin / Sierra cold sink basins in Truckee"
        ),
        description = "Vast, rugged territory encompassing temperate coastal rainforests, volcanic Cascade peaks, high alpine passes, and extreme rain shadow transitions.",
        status = RegionStatus.PLANNED_CONSTRAINTS,
        activeStationCount = 0,
        colorHex = "#6D28D9"
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
