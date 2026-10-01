package io.github.genesjc.microclimatefinder.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * What the menu drawer holds, in the About dialog's style: the same gradient header (with the California
 * icon, the name and the build), and menu rows with tinted icon tiles. It only dresses the existing items.
 */
@Composable
fun MenuDrawerContent(
    appName: String,
    buildLabel: String,
    showDemos: Boolean,
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onOpenDemos: () -> Unit,
    onOpenAbout: () -> Unit
) {
    Column {
        // Header: the About dialog's gradient, reaching up behind the status bar
        Box(
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
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CaliforniaOutlineIcon(
                    fill = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    stroke = MaterialTheme.colorScheme.onPrimaryContainer,
                    regionColors = regionPickerColors(),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Build $buildLabel",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (showDemos) {
                MenuRow(icon = "▦", title = "Demos & Previews", accent = Color(0xFF2563EB), onClick = onOpenDemos)
            }
            MenuRow(
                icon = "◐",
                title = "Dark mode",
                accent = Color(0xFF7C3AED),
                onClick = { onDarkModeChange(!darkMode) },
                trailing = { Switch(checked = darkMode, onCheckedChange = onDarkModeChange) }
            )
            MenuRow(icon = "ℹ️", title = "About", accent = Color(0xFF0D9488), onClick = onOpenAbout)
        }
    }
}

/** One menu row: a soft card with a tinted icon tile, the title, and an optional control at the right. */
@Composable
private fun MenuRow(
    icon: String,
    title: String,
    accent: Color,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) { Text(text = icon, fontSize = 20.sp, color = accent) }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            trailing?.invoke()
        }
    }
}
