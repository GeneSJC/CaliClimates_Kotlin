package io.github.genesjc.microclimatefinder.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.genesjc.microclimatefinder.ui.navigation.AppScreen

/**
 * Animation Previews Hub Screen
 * Displays a list of cards for inspecting atmospheric animation prototypes.
 */
@Composable
fun AnimationPreviewsScreen(
    onNavigateBack: () -> Unit,
    onSelectAnimation: (AppScreen) -> Unit,
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
                text = "Animation Previews",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Header Info Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Atmospheric Animation Prototypes",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Interactive previews of meteorological motion graphics designed with clean, idiomatic Jetpack Compose for high readability.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Available Animations",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Card 1: Fixed cloud v1.0 Animation
        AnimationCard(
            title = "Fixed cloud v1.0 Animation",
            description = "Canonical hero cloud with gentle floating hover, radial sun rays, wind streamlines, and swaying coastal palm.",
            badge = "v1.0",
            iconChar = "☀️",
            onClick = { onSelectAnimation(AppScreen.ANIMATION_FIXED_CLOUD_V1) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Card 2: Moving clouds v2.0 Animation
        AnimationCard(
            title = "Moving clouds v2.0 Animation",
            description = "Multi-cloud horizontal stratus drift (tandem cloud pair + solo offset cloud slowed down by 50%), flowing wind streamlines, sun pulse, and swaying palm.",
            badge = "v2.0",
            iconChar = "☁️",
            onClick = { onSelectAnimation(AppScreen.ANIMATION_MOVING_CLOUDS_V2) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Card 3: Web-Vector Animation (WebView Asset - Exact Web Preview)
        AnimationCard(
            title = "Web-Vector Animation (WebView)",
            description = "Exact vector HTML5/SVG animation loaded offline from assets. Renders the polished web preview with rich SVG curves, layered drop-shadows, and 60fps CSS physics.",
            badge = "v0.25.0",
            iconChar = "🌐",
            onClick = { onSelectAnimation(AppScreen.ANIMATION_WEB_VIEW) }
        )
    }
}

@Composable
private fun AnimationCard(
    title: String,
    description: String,
    badge: String,
    iconChar: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = iconChar,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }

            Text(
                text = "→",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
