package io.github.genesjc.microclimatefinder.ui.screens

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.genesjc.microclimatefinder.data.local.StationCsvParser
import io.github.genesjc.microclimatefinder.data.local.StationDatasetInfo
import io.github.genesjc.microclimatefinder.ui.components.RegionPickerButton
import io.github.genesjc.microclimatefinder.ui.components.RegionPickerOverlay
import io.github.genesjc.microclimatefinder.ui.components.rememberRegionPickerState
import io.github.genesjc.microclimatefinder.ui.components.StationCard

@Composable
fun StationListScreen(
    dataset: StationDatasetInfo,
    selectedRegionId: String,
    onSelectRegion: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val parser = remember(dataset.fileName) { StationCsvParser(context) }
    val stations = remember(dataset) { parser.loadStations(dataset) }
    var selectedStationId by remember { mutableStateOf<String?>(null) }
    val pickerState = rememberRegionPickerState()

    Box(modifier = modifier.fillMaxSize()) {
    // The same margins and header card as Home
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 20.dp, start = 10.dp, end = 10.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f)
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back, in the same tile as Home's menu button
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                    modifier = Modifier
                        .size(36.dp)
                        .clickable(onClickLabel = "Back", onClick = onNavigateBack)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "\u2190",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dataset.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 16.5.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = dataset.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // The region picker, the same chip as Home's
                RegionPickerButton(
                    state = pickerState,
                    selectedRegionId = selectedRegionId,
                    background = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                    content = MaterialTheme.colorScheme.onPrimaryContainer,
                    size = 52.dp,
                    iconSize = 40.dp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (stations.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No stations loaded from: ${dataset.fileName}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(stations, key = { it.id }) { station ->
                    StationCard(
                        station = station,
                        isSelected = station.id == selectedStationId,
                        onSelect = {
                            selectedStationId = if (selectedStationId == station.id) null else station.id
                        }
                    )
                }
            }
        }
    }
    RegionPickerOverlay(
        state = pickerState,
        selectedRegionId = selectedRegionId,
        onSelectRegion = onSelectRegion
    )
    }
}
