package io.github.genesjc.climatefinder.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.genesjc.climatefinder.R
import io.github.genesjc.climatefinder.config.StateUsaRegion
import io.github.genesjc.climatefinder.ui.components.RegionPickerChip
import io.github.genesjc.climatefinder.ui.components.RegionPickerState
import io.github.genesjc.climatefinder.ui.components.regionPickerColors

/** The region name with its drop-down, and the region picker button, in one tinted bar. Parked at present: see SHOW_REGION_BAR. */
@Composable
internal fun HomeRegionBar(
    region: StateUsaRegion,
    regionOptions: List<RegionOption>,
    selectedRegionId: String,
    pickerState: RegionPickerState,
    onSelectRegion: (String) -> Unit
) {
    var showRegionMenu by remember { mutableStateOf(false) }
    val headerShape = RoundedCornerShape(8.dp)
    val headerFill = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        .compositeOver(MaterialTheme.colorScheme.background)
    Row(
        modifier = Modifier.fillMaxWidth(),
        // A wider gap between the region widget and the picker widget, so more terrain shows between them (8dp, then 16dp)
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Widget 2: the region name with a chevron (tap for the region list), and the Region Info link
        // on the line below, in the same card. The Box is the list's anchor.
        Box(modifier = Modifier.weight(1f).height(HEADER_HEIGHT)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(headerShape)
                    // The tile's flat tint and 1 dp outline, so the name and Info link read clearly
                    .background(headerFill)
                    .border(1.dp, MaterialTheme.colorScheme.primary, headerShape)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClickLabel = stringResource(R.string.region_choose), role = Role.Button) {
                            showRegionMenu = true
                        }
                        .padding(vertical = 0.dp)
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(region.nameRes),
                            style = MaterialTheme.typography.titleLarge,
                            // 25% smaller than the title style, so more of a long name shows
                            fontSize = 20.6.sp,
                            lineHeight = 26.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = "\u25BE",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(start = 6.dp, end = 4.dp)
                        )
                    }
                    // The region picker, at the right of the name's row (it belongs with the drop-down)
                    Spacer(modifier = Modifier.width(8.dp))
                    // In a small button of its own, so it reads as tappable
                    RegionPickerChip(state = pickerState, selectedRegionId = selectedRegionId)
                }

            }
                // The region list, dressed like the rest of the app: a warm sand panel with a fine
                // outline, each region marked with its own map colour, the current one tinted
                val menuColors = regionPickerColors()
                val lightLook = MaterialTheme.colorScheme.background.luminance() > 0.5f
                DropdownMenu(
                    expanded = showRegionMenu,
                    onDismissRequest = { showRegionMenu = false },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = if (lightLook) Color(0xFFF1E2C4) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (lightLook) Color(0xFF3A2A1E).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.25f)
                    ),
                    tonalElevation = 0.dp,
                    shadowElevation = 8.dp
                ) {
                    regionOptions.forEach { option ->
                        val isCurrent = option.id == selectedRegionId
                        val regionColor = menuColors[option.id] ?: MaterialTheme.colorScheme.primary
                        DropdownMenuItem(
                            modifier = Modifier
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isCurrent) regionColor.copy(alpha = 0.22f) else Color.Transparent),
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.widthIn(min = 200.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(regionColor)
                                            .border(1.dp, Color(0xFF3A2A1E).copy(alpha = 0.35f), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(option.nameRes),
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isCurrent) {
                                        Text(
                                            text = "\u2713",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(start = 12.dp)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                showRegionMenu = false
                                if (!isCurrent) onSelectRegion(option.id)
                            }
                        )
                    }
                }
        }

    }
}
