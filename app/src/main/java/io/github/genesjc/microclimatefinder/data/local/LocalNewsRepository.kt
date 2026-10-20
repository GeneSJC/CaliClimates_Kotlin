package io.github.genesjc.microclimatefinder.data.local

import io.github.genesjc.microclimatefinder.domain.model.LocalNewsStory
import io.github.genesjc.microclimatefinder.domain.placeAndStationName

object LocalNewsRepository {

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

    fun getLocalStories(city: String): List<LocalNewsStory> {
        return ARCHIVE[city] ?: listOf(
            LocalNewsStory(
                title = "$city regional weather update: Microclimate conditions across valley transect",
                source = "Northern California News Wire",
                url = "https://news.google.com/search?q=" + city.replace(" ", "+") + "+California+news",
                published = "Just now",
                snippet = "Live meteorological and community reports for $city and surrounding California microclimates."
            ),
            LocalNewsStory(
                title = "Community highlights and civic updates from $city, California",
                source = "Local Pulse",
                url = "https://news.google.com/search?q=" + city.replace(" ", "+") + "+California",
                published = "3 hours ago",
                snippet = "Recent local events, municipal announcements, and community gatherings in $city."
            ),
            LocalNewsStory(
                title = "Seasonal agricultural and transit developments in $city",
                source = "Central California Journal",
                url = "https://news.google.com/search?q=" + city.replace(" ", "+") + "+news",
                published = "1 day ago",
                snippet = "Infrastructure, transit, and regional environmental conservation updates."
            )
        )
    }

    private val ARCHIVE = mapOf(
        "Vacaville" to listOf(
            LocalNewsStory(
                title = "Vacaville Police investigate shooting on Rocky Hill Road; suspect identified",
                source = "The Reporter - Vacaville",
                url = "https://www.thereporter.com/local-news/",
                published = "2 hours ago",
                snippet = "Authorities are searching for a suspect in connection with a shooting that occurred on Rocky Hill Road on Tuesday evening."
            ),
            LocalNewsStory(
                title = "Solano County water conservation update shows improved Putah Creek reservoir levels",
                source = "Daily Republic",
                url = "https://www.dailyrepublic.com/all-news/solano-county/",
                published = "5 hours ago",
                snippet = "Regional water district reports favorable runoff trends heading into autumn following unseasonal Sierra weather patterns."
            ),
            LocalNewsStory(
                title = "Nut Tree Plaza unveils fall community farmers market and artisan festival dates",
                source = "Vacaville Magazine",
                url = "https://www.thereporter.com/lifestyle/",
                published = "14 hours ago",
                snippet = "Historic Nut Tree commercial and aviation district announces seasonal weekend lineup featuring Solano growers and makers."
            ),
            LocalNewsStory(
                title = "Vacaville High Bulldogs host rival matchup under Friday night lights",
                source = "MaxPreps",
                url = "https://www.maxpreps.com/ca/vacaville/",
                published = "1 day ago",
                snippet = "Monticello Empire League opener brings strong crowd as Vacaville prepares for North Bay clash."
            )
        ),
        "Fairfield" to listOf(
            LocalNewsStory(
                title = "Travis Air Force Base conducts scheduled emergency response readiness drill",
                source = "Daily Republic",
                url = "https://www.dailyrepublic.com/all-news/solano-county/fairfield/",
                published = "3 hours ago",
                snippet = "The 60th Air Mobility Wing coordinated multi-agency emergency readiness exercises across Suisun Valley airspace."
            ),
            LocalNewsStory(
                title = "Suisun Marsh tidal restoration project marks major milestone for native waterfowl",
                source = "The Reporter",
                url = "https://www.thereporter.com/local-news/",
                published = "9 hours ago",
                snippet = "State wildlife officials report encouraging numbers for migrating waterfowl along the Suisun Bay wetlands."
            ),
            LocalNewsStory(
                title = "Fairfield City Council approves downtown revitalisation infrastructure grant",
                source = "Daily Republic",
                url = "https://www.dailyrepublic.com/all-news/solano-county/fairfield/",
                published = "1 day ago",
                snippet = "Funding earmarked for Texas Street pedestrian safety improvements, street trees, and outdoor dining patios."
            )
        ),
        "Sacramento" to listOf(
            LocalNewsStory(
                title = "Sacramento Delta breeze brings crisp evening cooling across the valley basin",
                source = "KCRA 3 News",
                url = "https://www.kcra.com/weather",
                published = "1 hour ago",
                snippet = "A brisk marine push through the Carquinez Strait dropped evening temperatures into the high 60s across Sacramento."
            ),
            LocalNewsStory(
                title = "Golden 1 Center announces expanded zero-waste sustainability initiatives",
                source = "Sacramento Bee",
                url = "https://www.sacbee.com/news/local/",
                published = "4 hours ago",
                snippet = "Downtown venue achieves national green building benchmarks ahead of the upcoming Kings NBA season."
            ),
            LocalNewsStory(
                title = "Sacramento River Parkway trail expansion opens 4 new miles to cyclists",
                source = "CapRadio NPR",
                url = "https://www.capradio.org/news/",
                published = "8 hours ago",
                snippet = "New paved riverfront trail connects Old Sacramento to Miller Regional Park with scenic wildlife viewpoints."
            ),
            LocalNewsStory(
                title = "Midtown Farmers Market celebrates harvest season with record regional farm participation",
                source = "The Sacramento Observer",
                url = "https://sacobserver.com/",
                published = "1 day ago",
                snippet = "Over 80 local vendors gathered on 20th Street showcasing Central Valley heirloom produce and organic honey."
            )
        ),
        "Davis" to listOf(
            LocalNewsStory(
                title = "UC Davis researchers model climate adaptation strategies for Central Valley vineyards",
                source = "The Davis Enterprise",
                url = "https://www.davisenterprise.com/news/",
                published = "3 hours ago",
                snippet = "Viticulture and enology department publishes new microclimate moisture retention study for warm interior valleys."
            ),
            LocalNewsStory(
                title = "City of Davis expands protected bidirectional bicycle network along Russell Boulevard",
                source = "The Davis Enterprise",
                url = "https://www.davisenterprise.com/local-news/",
                published = "7 hours ago",
                snippet = "Infrastructure overhaul creates continuous protected lane connecting West Davis directly to campus."
            ),
            LocalNewsStory(
                title = "Davis Farmers Market ranked among top outdoor agricultural markets in Northern California",
                source = "Sacramento Bee",
                url = "https://www.sacbee.com/news/local/",
                published = "18 hours ago",
                snippet = "Central Park gathering draws regional visitors with seasonal citrus, artisan bread, and live acoustic music."
            )
        ),
        "Auburn" to listOf(
            LocalNewsStory(
                title = "American River Canyon trail restoration completed ahead of fall hiking season",
                source = "Auburn Journal",
                url = "https://www.goldcountrymedia.com/auburn-journal/",
                published = "2 hours ago",
                snippet = "Volunteers and State Parks rangers stabilized canyon overlooks and improved switchback drainage."
            ),
            LocalNewsStory(
                title = "Auburn Municipal Airport hosts vintage aircraft showcase and community fly-in",
                source = "Gold Country Media",
                url = "https://www.goldcountrymedia.com/auburn-journal/",
                published = "6 hours ago",
                snippet = "Dozens of historical planes gathered at the foothill airport above the Central Valley haze."
            ),
            LocalNewsStory(
                title = "Placer County thermal belt orchards report early mandarin harvest expectations",
                source = "Sierra Sun Times",
                url = "https://sierrasuntimes.com/",
                published = "1 day ago",
                snippet = "Foothill microclimate conditions deliver ideal sugar concentration for Newcastle and Penryn citrus growers."
            )
        ),
        "Monterey" to listOf(
            LocalNewsStory(
                title = "Monterey Bay coastal upwelling brings rare marine mammal sightings near submarine canyon",
                source = "Monterey Herald",
                url = "https://www.montereyherald.com/news/",
                published = "2 hours ago",
                snippet = "Marine biologists observed blue whales and humpbacks feeding along the deep submarine trench edges."
            ),
            LocalNewsStory(
                title = "Historic Cannery Row pedestrian revitalization project moves to public comment phase",
                source = "KSBW Action News 8",
                url = "https://www.ksbw.com/local-news",
                published = "5 hours ago",
                snippet = "City planners proposal highlights widened walkways, native coastal landscaping, and heritage plaques."
            ),
            LocalNewsStory(
                title = "Pacific Grove Monarch Butterfly Sanctuary prepares for initial seasonal arrivals",
                source = "Monterey County Weekly",
                url = "https://www.montereycountyweekly.com/",
                published = "11 hours ago",
                snippet = "Volunteers complete eucalyptus grove restoration to provide ideal microclimatic wind shelter."
            )
        ),
        "Salinas" to listOf(
            LocalNewsStory(
                title = "Salinas Valley ag-tech conference showcases solar and precision irrigation advances",
                source = "The Californian",
                url = "https://www.thecalifornian.com/news/",
                published = "3 hours ago",
                snippet = "Growers across the 'Salad Bowl of the World' adopt automated soil moisture telemetry to conserve water."
            ),
            LocalNewsStory(
                title = "National Steinbeck Center opens special exhibition on regional agricultural history",
                source = "Salinas Valley Tribune",
                url = "https://salinasvalleytribune.com/",
                published = "8 hours ago",
                snippet = "Rare photographs and journal entries illuminate the cultural fabric of early 20th century California valleys."
            )
        ),
        "Palo Alto" to listOf(
            LocalNewsStory(
                title = "Palo Alto Baylands Nature Preserve expands tidal marsh boardwalk and bird blinds",
                source = "Palo Alto Online",
                url = "https://www.paloaltoonline.com/news/",
                published = "4 hours ago",
                snippet = "New boardwalk allows visitors to view endangered Ridgway's rails without disturbing the tidal wetlands."
            ),
            LocalNewsStory(
                title = "Stanford University researchers unveil low-cost microclimate sensor network",
                source = "Stanford Report",
                url = "https://news.stanford.edu/",
                published = "9 hours ago",
                snippet = "Department of Civil and Environmental Engineering installs regional sensors to monitor Bay Area urban heat islands."
            )
        ),
        "Half Moon Bay" to listOf(
            LocalNewsStory(
                title = "Half Moon Bay coast prepares for seasonal Pacific swell and big-wave surf watch",
                source = "Half Moon Bay Review",
                url = "https://www.hmbreview.com/news/",
                published = "3 hours ago",
                snippet = "Harbor patrol and coastal safety teams monitor incoming ocean swells off Pillar Point."
            ),
            LocalNewsStory(
                title = "Coastside pumpkin harvest reaches peak season across Highway 92 farm stands",
                source = "Half Moon Bay Review",
                url = "https://www.hmbreview.com/",
                published = "7 hours ago",
                snippet = "Cool coastal fog and warm interior soils provide prime conditions for heritage pumpkin varieties."
            )
        ),
        "Santa Cruz" to listOf(
            LocalNewsStory(
                title = "Santa Cruz Beach Boardwalk celebrates coastal conservation milestone with local schools",
                source = "Santa Cruz Sentinel",
                url = "https://www.santacruzsentinel.com/news/",
                published = "2 hours ago",
                snippet = "Students participate in Monterey Bay National Marine Sanctuary beach cleanup and dune restoration."
            ),
            LocalNewsStory(
                title = "West Cliff Drive coastal trail stabilization project reaches Phase 2 completion",
                source = "Lookout Santa Cruz",
                url = "https://lookout.co/santacruz/",
                published = "6 hours ago",
                snippet = "Engineers reinforced sea cliffs and reopened continuous oceanfront multi-use paths."
            )
        )
    )
}
