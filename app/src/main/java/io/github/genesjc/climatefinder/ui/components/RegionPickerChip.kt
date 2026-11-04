package io.github.genesjc.climatefinder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import io.github.genesjc.climatefinder.R

/** The picker button's height, in the region's bar at the right of the header; tunable here. */
val PICKER_BUTTON_HEIGHT = 32.dp

/**
 * The region picker as an outlined button, as tall as its [modifier] makes it: the state's silhouette with the current
 * region at full strength. Tapping anywhere on it opens the graphical picker, which grows out of the silhouette.
 */
@Composable
fun RegionPickerChip(
    state: RegionPickerState,
    selectedRegionId: String,
    modifier: Modifier = Modifier,
    // The room between the button's outline and the icon
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(Color.White.copy(alpha = 0.9f))
            .border(1.dp, MaterialTheme.colorScheme.primary, shape)
            .clickable(
                enabled = !state.visible,
                onClickLabel = stringResource(R.string.region_picker_change),
                role = Role.Button
            ) { state.open(selectedRegionId) }
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StateUsaOutlineIcon(
            fill = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f),
            stroke = MaterialTheme.colorScheme.onPrimaryContainer,
            regionColors = regionPickerColors(),
            selectedRegionId = selectedRegionId,
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(0.8f)
                .onGloballyPositioned { state.buttonBounds = Rect(it.positionInRoot(), it.size.toSize()) }
                .alpha(if (state.visible) 0f else 1f)
        )
    }
}
