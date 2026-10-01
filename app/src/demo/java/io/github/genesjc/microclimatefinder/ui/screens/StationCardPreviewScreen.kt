package io.github.genesjc.microclimatefinder.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.genesjc.microclimatefinder.domain.model.MicroclimateStation
import io.github.genesjc.microclimatefinder.domain.model.TerrainType
import io.github.genesjc.microclimatefinder.domain.model.WeatherReading
import io.github.genesjc.microclimatefinder.ui.components.StationCard

@Composable
fun StationCardPreviewScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top App Bar with back button (zero extra icon dependencies)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .size(36.dp)
                    .clickable(onClick = onNavigateBack)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "←",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "Station Card Preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Standalone Component Preview",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Render the station card component: the full card, then the compact one used as the
        // Station Inspector under the Topo Map and the Elevation Profile
        Text(
            text = "Full card (Station Feed)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        StationCard(station = sampleStation, useMockReadings = true)

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Compact card (Station Inspector)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        StationCard(station = sampleStation, useMockReadings = true, compact = true)
    }
}
