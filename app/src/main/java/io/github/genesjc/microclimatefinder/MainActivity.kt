package io.github.genesjc.microclimatefinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.genesjc.microclimatefinder.domain.model.MicroclimateStation
import io.github.genesjc.microclimatefinder.domain.model.TerrainType
import io.github.genesjc.microclimatefinder.domain.model.WeatherReading
import io.github.genesjc.microclimatefinder.ui.components.StationCard
import io.github.genesjc.microclimatefinder.ui.theme.MicroClimateFinderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MicroClimateFinderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    val sampleStation = MicroclimateStation(
        id = "station_ridge_01",
        name = "Mt. Tamalpais Ridge",
        latitude = 37.9235,
        longitude = -122.5965,
        elevationMeters = 784,
        terrain = TerrainType.RIDGE,
        currentReading = WeatherReading(
            temperatureCelsius = 18.4,
            humidityPercent = 44,
            windSpeedKmh = 24.0,
            windDirectionDegrees = 290,
            solarIrradianceWm2 = 720.0,
            pressureHpa = 1012.4
        ),
        microclimateDescription = "Warm and arid conditions sitting above the marine boundary inversion layer."
    )

    // Resolved cleanly at compile-time via Gradle BuildConfig for the active flavor
    val flavorDescription = if (BuildConfig.IS_COMMERCIAL) {
        "Micro Climate Finder (Commercial Edition)"
    } else {
        "Micro Climate Finder (FOSS Edition)"
    }

    val mapProviderName = if (BuildConfig.IS_COMMERCIAL) {
        "Google Maps SDK & Premium Radar"
    } else {
        "OpenStreetMap / OsmDroid"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Flavor indicator banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = flavorDescription,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Map Engine: $mapProviderName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Active Microclimate Station",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Visual verification: Render the station card directly
        StationCard(station = sampleStation)
    }
}
