package io.github.genesjc.climatefinder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.genesjc.climatefinder.R

/**
 * The region picker as a small outlined button: the region's own shape, and a chevron beside it so it reads as a choice,
 * like a select list. The shape and the chevron both open the graphical picker.
 */
@Composable
fun RegionPickerChip(state: RegionPickerState, selectedRegionId: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 62.dp, height = 34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.6f))
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RegionPickerButton(
                state = state,
                selectedRegionId = selectedRegionId,
                background = Color.Transparent,
                content = MaterialTheme.colorScheme.onPrimaryContainer,
                border = Color.Transparent,
                size = 34.dp,
                iconSize = 28.dp
            )
            // A chevron beside the shape: this is a choice, like a select list. It opens the same picker
            Text(
                text = "\u25BE",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .padding(start = 2.dp, end = 6.dp)
                    .clickable(enabled = !state.visible, onClickLabel = stringResource(R.string.region_picker_change), role = Role.Button) {
                        state.open(selectedRegionId)
                    }
            )
        }
    }
}
