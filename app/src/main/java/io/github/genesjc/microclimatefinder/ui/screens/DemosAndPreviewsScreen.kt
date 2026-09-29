package io.github.genesjc.microclimatefinder.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.genesjc.microclimatefinder.ui.components.DemoNavigationCard
import io.github.genesjc.microclimatefinder.ui.navigation.AppScreen

/**
 * Demos & Previews Hub Screen
 * Single-column list housing the component/feature demo cards moved off the Landing list:
 * Station Card Preview, Draggable Topo Map, and Animation Previews.
 */
@Composable
fun DemosAndPreviewsScreen(
    onNavigateTo: (AppScreen) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Bar with back button
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

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Demos & Previews",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Component & Feature Demos",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Station Card Preview
        DemoNavigationCard(
            title = "Station Card Preview",
            description = "Interactive single station card with elevation badge, dual C/F temps, wind, and terrain styling.",
            iconChar = "▦",
            onClick = { onNavigateTo(AppScreen.STATION_CARD_PREVIEW) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Draggable Topo Map
        DemoNavigationCard(
            title = "Draggable Topo Map",
            description = "Interactive 4-way pan (N/S/E/W) with gesture dragging across Sacramento 60-mile stations, coordinate readout & recenter.",
            iconChar = "✥",
            onClick = { onNavigateTo(AppScreen.DRAGGABLE_MAP) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Animation Previews
        DemoNavigationCard(
            title = "Animation Previews",
            description = "Interactive atmospheric and weather animation prototypes: fixed hero cloud v1.0, moving clouds v2.0, and offline web vector preview.",
            iconChar = "✨",
            onClick = { onNavigateTo(AppScreen.ANIMATION_PREVIEWS) }
        )
    }
}
