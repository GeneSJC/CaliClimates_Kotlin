package io.github.genesjc.climatefinder.domain.model

import io.github.genesjc.climatefinder.R

/**
 * California, the one USA state defined so far: its six regions, outline, picture bounds, station packs, landmarks,
 * scenery and the artwork of the region picker map. This object and the string resources it points to
 * (`strings_region_ca.xml`) are the only places that name California; everything else asks [StateUsaDefinition].
 */
object StateUsaCA : StateUsaDefinition {

    override val code = "ca"

    override val taglineRes = R.string.ca_tagline

    override val regionsScreenTitleRes = R.string.ca_regions_screen_title

    override val regionsMapTitleRes = R.string.ca_regions_map_title

    val SAN_DIEGO = StateUsaRegion(
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

    val LOS_ANGELES = StateUsaRegion(
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

    val CENTRAL_COAST = StateUsaRegion(
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

    val CENTRAL_VALLEY = StateUsaRegion(
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

    val BAY_AREA_SACRAMENTO = StateUsaRegion(
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

    val NORTHERN_CALIFORNIA = StateUsaRegion(
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

    override val regions = listOf(
        NORTHERN_CALIFORNIA,
        BAY_AREA_SACRAMENTO,
        CENTRAL_COAST,
        CENTRAL_VALLEY,
        LOS_ANGELES,
        SAN_DIEGO
    )

    override val defaultRegionId = BAY_AREA_SACRAMENTO.id

    /** Generalized California outline, coarse on purpose: a coastline of about 20 points. */
    override val outline = StateUsaOutline(
        // Pacific coast from the Oregon border (north) to the Mexico border (south)
        coast = listOf(
            -124.21 to 42.00, -124.41 to 40.44, -123.65 to 38.80, -123.00 to 38.00,
            -122.51 to 37.77, -122.20 to 37.00, -121.85 to 36.80, -121.90 to 36.30,
            -120.85 to 35.30, -120.47 to 34.45, -119.70 to 34.40, -119.10 to 34.10,
            -118.85 to 34.02, -118.40 to 33.74, -117.60 to 33.38, -117.25 to 32.72,
            -117.12 to 32.53
        ),
        // Land border from the Mexico coast, east and north around Nevada, to the Oregon coast
        landBorder = listOf(
            -117.12 to 32.53, -114.72 to 32.72, -114.63 to 35.00, -120.00 to 39.00,
            -120.00 to 42.00, -124.21 to 42.00
        )
    )

    // What each terrain picture covers. The images are generated to these bounds; a unit test checks the stations fit.
    override val terrainBounds: Map<String, GeoBounds> = mapOf(
        "northern_california" to GeoBounds(-125.4572, -118.9025, 37.3110, 43.8160),
        "bay_area_sacramento" to GeoBounds(-123.8959, -119.0628, 36.3508, 39.8245),
        "central_coast" to GeoBounds(-123.3826, -117.9792, 33.7269, 37.6208),
        "central_valley" to GeoBounds(-123.7687, -115.9850, 34.6013, 39.3836),
        "los_angeles" to GeoBounds(-119.3960, -116.5571, 32.3232, 35.9385),
        "san_diego" to GeoBounds(-117.7634, -115.8557, 31.7898, 34.2508)
    )

    /** The whole state for the picker, and its neighbours drawn quieter. Their pixels are linear in lon and lat. */
    override val stateBounds = GeoBounds(-124.5, -114.1, 31.9, 43.0)

    // Station pack per region, named by the app's region order: north to south, then west to east
    override val stationPacks: Map<String, String> = mapOf(
        NORTHERN_CALIFORNIA.id to "a.txt",
        BAY_AREA_SACRAMENTO.id to "b.txt",
        CENTRAL_COAST.id to "c.txt",
        CENTRAL_VALLEY.id to "d.txt",
        LOS_ANGELES.id to "e.txt",
        SAN_DIEGO.id to "f.txt"
    )

    /** The town each station is in, by station id, for the names on the maps and cards (not every station has one). */
    override val stationTowns: Map<String, String> = mapOf(
        "KVCB" to "Vacaville",
        "KSMF" to "Sacramento",
        "KSAC" to "Sacramento",
        "KMHR" to "Rancho Cordova",
        "KEDU" to "Davis",
        "KLHM" to "Lincoln",
        "KAUN" to "Auburn",
        "KMYV" to "Marysville",
        "KSCK" to "Stockton",
        "KPAO" to "Palo Alto",
        "KHAF" to "Half Moon Bay",
        "MTHC1" to "San Jose",
        "SCZC1" to "Santa Cruz",
        "KWVI" to "Watsonville",
        "MSLC1" to "Moss Landing",
        "KSNS" to "Salinas",
        "KMRY" to "Monterey",
        "PMSC1" to "Pacific Grove",
        "CARC1" to "Carmel Valley",
        "CHWC1" to "Carmel Valley",
        "BIGC1" to "Big Sur",
        "PINC1" to "Paicines",
        "KCVH" to "Hollister",
        "KPRB" to "Paso Robles",
        "PLTC1" to "Pilot Hill",
        "BENC1" to "Georgetown",
        "BDMC1" to "Foresthill",
        "KGOO" to "Grass Valley",
        "SETC1" to "Colfax",
        "RRRC1" to "Penn Valley",
        "FSHC1" to "Chico",
        "BNGC1" to "Oroville",
        "BKSC1" to "Brooks",
        "MTZC1" to "Jackson",
        "ESPC1" to "Placerville"
    )

    // Approximate heights, for comparisons in plain words
    override val landmarks: Map<String, List<Landmark>> = mapOf(
        "bay_area_sacramento" to listOf(
            Landmark("Twin Peaks", 925), Landmark("Mt. Tamalpais", 2_570), Landmark("Mt. Diablo", 3_850),
            Landmark("Mt. Hamilton", 4_210), Landmark("Lake Tahoe", 6_225)
        ),
        "los_angeles" to listOf(
            Landmark("Griffith Observatory", 1_130), Landmark("the Hollywood Sign", 1_600),
            Landmark("Mt. Wilson", 5_710), Landmark("Big Bear Lake", 6_750), Landmark("Mt. Baldy", 10_060)
        ),
        "san_diego" to listOf(
            Landmark("Mt. Soledad", 820), Landmark("Julian", 4_200), Landmark("Cuyamaca Peak", 6_510)
        ),
        "central_coast" to listOf(
            Landmark("Morro Rock", 580), Landmark("the Pinnacles High Peaks", 3_300),
            Landmark("Junipero Serra Peak", 5_860)
        ),
        "central_valley" to listOf(
            Landmark("Fresno", 300), Landmark("the Yosemite Valley floor", 4_000), Landmark("Half Dome", 8_840)
        ),
        "northern_california" to listOf(
            Landmark("Redding", 500), Landmark("Lassen Peak", 10_460), Landmark("Mt. Shasta", 14_180)
        )
    )

    /** The kinds of tree along the near hills, by region (repeats make a kind more common). */
    /**
     * The places people look for first (a station stands for a place when it is the airport or the station by it). The Bay Area
     * has no station in San Francisco itself, so the city's airport stands for it, and none in Santa Cruz, so that waits for the
     * station data refresh (the Central Coast has Monterey, Salinas and Watsonville, which is next to it). In each region the best
     * known place comes first (San Francisco, Santa Barbara, Fresno, Los Angeles, San Diego, Redding), then by size and fame.
     * Stations that report nothing are left out, which is why Eureka is the Arcata / Eureka airport and not Woodley Island.
     */
    override val landmarkStations: Map<String, List<String>> = mapOf(
        "bay_area_sacramento" to listOf(
            "KSFO", // San Francisco (the airport, south of the city)
            "KOAK", // Oakland
            "KRHV", // San Jose
            "KSMF", // Sacramento
            "KSCK", // Stockton
            "KAPC", // Napa
            "KDVO"  // Novato, the nearest to Marin
        ),
        "northern_california" to listOf(
            "KRDD", // Redding
            "KCIC", // Chico
            "KACV", // Eureka and Arcata (the Arcata / Eureka airport)
            "KTRK", // Truckee and Tahoe
            "KMHS", // Mount Shasta
            "KUKI", // Ukiah
            "KCEC"  // Crescent City
        ),
        "central_coast" to listOf(
            "KSBA", // Santa Barbara
            "KMRY", // Monterey
            "KSBP", // San Luis Obispo
            "KSMX", // Santa Maria
            "KSNS", // Salinas
            "KWVI", // Watsonville, next to Santa Cruz
            "KPRB"  // Paso Robles
        ),
        "central_valley" to listOf(
            "KFAT", // Fresno
            "KBFL", // Bakersfield
            "KVIS", // Visalia
            "KMCE", // Merced
            "KMMH", // Mammoth Lakes
            "KHJO"  // Hanford
        ),
        "los_angeles" to listOf(
            "KLAX",  // Los Angeles (the airport)
            "FHMC1", // Los Angeles Downtown
            "KSMO",  // Santa Monica
            "KLGB",  // Long Beach
            "KBUR",  // Burbank
            "KSNA",  // Orange County (Santa Ana)
            "KONT",  // Ontario
            "KRAL"   // Riverside
        ),
        "san_diego" to listOf(
            "KSAN",  // San Diego
            "LJAC1", // La Jolla
            "KCRQ",  // Carlsbad
            "KOKB",  // Oceanside
            "KSEE",  // El Cajon (Gillespie Field)
            "KRNM"   // Ramona
        )
    )
    )

    override val treeMix: Map<String, List<TreeKind>> = mapOf(
        "northern_california" to listOf(TreeKind.CONIFER, TreeKind.CONIFER, TreeKind.CONIFER, TreeKind.OAK),
        "bay_area_sacramento" to listOf(TreeKind.CONIFER, TreeKind.OAK, TreeKind.OAK, TreeKind.CONIFER),
        "central_coast" to listOf(TreeKind.OAK, TreeKind.OAK, TreeKind.CONIFER),
        "central_valley" to listOf(TreeKind.OAK, TreeKind.OAK, TreeKind.OAK, TreeKind.CONIFER),
        "los_angeles" to listOf(TreeKind.PALM, TreeKind.OAK, TreeKind.OAK),
        "san_diego" to listOf(TreeKind.PALM, TreeKind.PALM, TreeKind.OAK)
    )

    /**
     * The six regions as (lon, lat) polygons that share their boundary points, so the small icon on the picker
     * button can be drawn the same way as the picker's map. The picker map draws the same boundaries.
     */
    override val regionShapes: Map<String, List<Pair<Float, Float>>> = run {
        val tahoe = -120.00f to 39.00f
        val northValleyMid = -120.80f to 39.15f
        val northBayValley = -121.60f to 39.20f
        val northBayMid = -122.80f to 38.95f
        val coastMendoSonoma = -123.65f to 38.80f

        val bayValley1 = -121.20f to 38.50f
        val bayValley2 = -121.15f to 37.95f
        val bayValley3 = -121.30f to 37.50f
        val bayValleyCoast = -121.50f to 37.00f
        val bayCoastMid = -121.80f to 36.95f
        val coastMontereyBay = -121.85f to 36.80f

        val coastValley1 = -120.70f to 36.20f
        val coastValley2 = -120.10f to 35.50f
        val coastValley3 = -119.50f to 35.00f
        val coastValleyLa = -119.00f to 34.80f
        val coastLaMid = -119.30f to 34.45f
        val coastVentura = -119.10f to 34.10f

        val valleyLa1 = -118.45f to 35.05f
        val valleyLa2 = -117.80f to 35.35f
        val valleyLa3 = -117.20f to 35.80f
        val nevadaValleyLa = -115.80f to 36.00f

        val coastSanOnofre = -117.60f to 33.38f
        val laSd1 = -117.10f to 33.45f
        val laSd2 = -116.30f to 33.35f
        val laSd3 = -115.50f to 33.20f
        val coloradoRiverMid = -114.60f to 33.30f

        mapOf(
            "northern_california" to listOf(
                coastMendoSonoma, -124.41f to 40.44f, -124.21f to 42.00f, -120.00f to 42.00f,
                tahoe, northValleyMid, northBayValley, northBayMid
            ),
            "bay_area_sacramento" to listOf(
                coastMendoSonoma, northBayMid, northBayValley, bayValley1, bayValley2, bayValley3,
                bayValleyCoast, bayCoastMid, coastMontereyBay, -122.20f to 37.00f, -122.51f to 37.77f, -123.00f to 38.00f
            ),
            "central_valley" to listOf(
                tahoe, northValleyMid, northBayValley, bayValley1, bayValley2, bayValley3, bayValleyCoast,
                coastValley1, coastValley2, coastValley3, coastValleyLa, valleyLa1, valleyLa2, valleyLa3,
                nevadaValleyLa, -117.20f to 37.00f, -118.60f to 38.00f
            ),
            "central_coast" to listOf(
                coastMontereyBay, bayCoastMid, bayValleyCoast, coastValley1, coastValley2, coastValley3,
                coastValleyLa, coastLaMid, coastVentura, -119.70f to 34.40f, -120.47f to 34.45f,
                -120.85f to 35.30f, -121.90f to 36.30f
            ),
            "los_angeles" to listOf(
                coastVentura, coastLaMid, coastValleyLa, valleyLa1, valleyLa2, valleyLa3, nevadaValleyLa,
                -114.63f to 35.00f, coloradoRiverMid, laSd3, laSd2, laSd1, coastSanOnofre,
                -118.40f to 33.74f, -118.85f to 34.02f
            ),
            "san_diego" to listOf(
                coastSanOnofre, laSd1, laSd2, laSd3, coloradoRiverMid, -114.72f to 32.72f,
                -117.12f to 32.53f, -117.25f to 32.72f
            )
        )
    }

    override val pickerMap = StateUsaPickerMap(
        view = GeoBounds(-124.45, -114.15, 31.9, 43.0),
        // The Pacific, along the coast: from the top-left corner down the coast to the bottom edge
        seaPoints = listOf(
            -124.45f to 43.00f, -124.21f to 42.00f, -124.41f to 40.44f, -123.65f to 38.80f,
            -123.00f to 38.00f, -122.51f to 37.77f, -122.20f to 37.00f, -121.85f to 36.80f,
            -121.90f to 36.30f, -120.85f to 35.30f, -120.47f to 34.45f, -119.70f to 34.40f,
            -119.10f to 34.10f, -118.85f to 34.02f, -118.40f to 33.74f, -117.60f to 33.38f,
            -117.25f to 32.72f, -117.12f to 32.53f, -117.02f to 32.20f, -116.70f to 31.90f
        ),
        seaBottomLon = -116.65f,
        gridLatitudes = listOf(34f, 36f, 38f, 40f),
        // California's mountain ranges, drawn as hachure lines when there is no terrain picture
        ridges = listOf(
            listOf(-123.60f to 41.70f, -122.80f to 41.20f, -122.20f to 40.50f, -121.80f to 39.80f),
            listOf(
                -120.30f to 39.80f, -120.00f to 39.00f, -119.50f to 38.00f, -118.80f to 37.00f,
                -118.30f to 36.20f, -118.20f to 35.30f, -118.00f to 34.90f
            ),
            listOf(
                -123.40f to 39.30f, -122.90f to 38.60f, -122.30f to 37.70f, -121.60f to 36.60f,
                -120.90f to 35.60f, -120.30f to 34.80f
            ),
            listOf(-119.60f to 34.55f, -118.90f to 34.35f, -118.10f to 34.30f, -117.30f to 34.20f),
            listOf(-116.90f to 33.70f, -116.70f to 33.20f, -116.50f to 32.70f)
        ),
        // Oregon and Nevada share the 42°N line
        borderLine = StateUsaBorderLine(lat = 42.0f, fromLon = -120.0f),
        regionLabels = listOf(
            StateUsaMapLabel(R.string.ca_map_label_northern_california, -122.2f, 40.9f),
            StateUsaMapLabel(R.string.ca_map_label_bay_area, -122.2f, 38.45f),
            StateUsaMapLabel(R.string.ca_map_label_central_valley, -119.6f, 36.9f),
            StateUsaMapLabel(R.string.ca_map_label_central_coast, -121.2f, 35.4f),
            StateUsaMapLabel(R.string.ca_map_label_los_angeles, -117.0f, 34.45f),
            StateUsaMapLabel(R.string.ca_map_label_san_diego, -116.3f, 32.95f)
        ),
        neighbourLabels = listOf(
            StateUsaMapLabel(R.string.ca_map_label_oregon, -121.9f, 42.55f),
            StateUsaMapLabel(R.string.ca_map_label_idaho, -115.3f, 42.55f),
            StateUsaMapLabel(R.string.ca_map_label_mexico, -115.9f, 32.15f),
            StateUsaMapLabel(R.string.ca_map_label_nevada, -116.9f, 39.3f, emphasis = true)
        )
    )
}
