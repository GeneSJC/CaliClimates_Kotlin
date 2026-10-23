package io.github.genesjc.microclimatefinder.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.genesjc.microclimatefinder.R
import io.github.genesjc.microclimatefinder.data.local.StateUsaTerrain
import io.github.genesjc.microclimatefinder.ui.regionpicker.RegionPickerViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.genesjc.microclimatefinder.ui.theme.LocalStateUsa
import io.github.genesjc.microclimatefinder.domain.model.RegionStatus

/** How much the picker's map is stretched vertically (1 = true shape). Taller, at the cost of some skew. */
internal const val PICKER_VERTICAL_STRETCH = 1.35f

/**
 * Content of the region picker overlay on Home: a title, a close button and the regional map. A pick on
 * the map is the choice itself ([onPickRegion]); there is no Confirm button.
 */
@Composable
fun StateUsaRegionPickerPanel(
    pendingRegionId: String,
    terrain: StateUsaTerrain?,
    onPickRegion: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        // The title, centred on the panel, and the close button at the right
        Box(
            modifier = Modifier.fillMaxWidth().heightIn(min = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.region_picker_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.align(Alignment.Center)
            )
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(32.dp)
                    .clickable(onClickLabel = stringResource(R.string.action_close), onClick = onClose)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "\u2715",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        StateUsaClassicMapCanvas(
            selectedRegionId = pendingRegionId,
            terrain = terrain,
            onSelectRegion = onPickRegion,
            // Tall: the map is stretched a little vertically and fills the panel's height (the panel is sized to
            // fit it, up to most of the screen; on a short screen the map just gets a little shorter)
            sizeModifier = Modifier.weight(1f),
            verticalStretch = PICKER_VERTICAL_STRETCH
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StateUsaRegionsMapScreen(
    selectedRegionId: String,
    onSelectRegion: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToActiveTransect: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: RegionPickerViewModel = viewModel()
) {
    val stateTerrain by viewModel.stateTerrain.collectAsStateWithLifecycle()
    val stateUsa = LocalStateUsa.current
    val selectedRegion = remember(stateUsa, selectedRegionId) {
        stateUsa.region(selectedRegionId) ?: stateUsa.defaultRegion()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(stateUsa.regionsScreenTitleRes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "v0.23.0",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = stringResource(R.string.regions_screen_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(36.dp)
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
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Interactive state map card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(stateUsa.regionsMapTitleRes),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.regions_map_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Map Viewport (Strict Light Mode)
                        StateUsaClassicMapCanvas(
                            selectedRegionId = selectedRegionId,
                            terrain = stateTerrain,
                            onSelectRegion = onSelectRegion
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Region Dropdown Selector (Pill Tag Outline Style)
                        var dropdownExpanded by remember { mutableStateOf(false) }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {
                            // Pill Tag Outline Trigger
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                                ),
                                shadowElevation = 1.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { dropdownExpanded = !dropdownExpanded }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(
                                                    color = Color(android.graphics.Color.parseColor(selectedRegion.colorHex)),
                                                    shape = CircleShape
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(selectedRegion.nameRes),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (selectedRegion.status == RegionStatus.ACTIVE_TRANSECT) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "★",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = stringResource(R.string.action_select),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (dropdownExpanded) "▴" else "▾",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            // Material3 DropdownMenu
                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        RoundedCornerShape(14.dp)
                                    )
                            ) {
                                stateUsa.regions.forEach { region ->
                                    val isSelected = region.id == selectedRegionId
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .background(
                                                                color = Color(android.graphics.Color.parseColor(region.colorHex)),
                                                                shape = CircleShape
                                                            )
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = stringResource(region.nameRes),
                                                        fontSize = 13.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    if (region.status == RegionStatus.ACTIVE_TRANSECT) {
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "★",
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                                if (isSelected) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "✓",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            onSelectRegion(region.id)
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Selected Region Detail & Constraints Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Region Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(selectedRegion.titleRes),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(selectedRegion.subtitleRes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (selectedRegion.status == RegionStatus.ACTIVE_TRANSECT) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                }
                            ) {
                                Text(
                                    text = if (selectedRegion.status == RegionStatus.ACTIVE_TRANSECT) {
                                        pluralStringResource(R.plurals.region_active_badge, selectedRegion.activeStationCount, selectedRegion.activeStationCount)
                                    } else {
                                        stringResource(R.string.region_planned_badge)
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedRegion.status == RegionStatus.ACTIVE_TRANSECT) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Description
                        Text(
                            text = stringResource(selectedRegion.descriptionRes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                            lineHeight = 20.sp
                        )

                        // Microclimate Features
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = stringResource(R.string.regions_key_dynamics),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.5.sp
                            )
                            selectedRegion.microclimateTraitRes.forEach { traitRes ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "•",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = stringResource(traitRes),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        // Action Button if Active
                        if (selectedRegion.status == RegionStatus.ACTIVE_TRANSECT) {
                            Button(
                                onClick = onNavigateToActiveTransect,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(
                                    text = pluralStringResource(R.plurals.region_open_active_transect, selectedRegion.activeStationCount, selectedRegion.activeStationCount),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
