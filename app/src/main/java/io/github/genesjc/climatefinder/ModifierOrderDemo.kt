package io.github.genesjc.climatefinder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * SANDBOX ONLY (never merged). Modifier order demo: tap the button to cycle through five variants.
 * Each modifier wraps the result of the one before it, so order changes the outcome.
 */
@Composable
fun ModifierOrderDemo() {
    var variant by rememberSaveable { mutableStateOf(0) }
    var taps by rememberSaveable { mutableStateOf(0) }

    // Variant text: the chain, then what to expect on screen
    val chain = listOf(
        "size(120).background(Red).padding(24).background(Blue)",
        "size(120).padding(24).background(Red)",
        "size(100).size(50).background(Red)",
        "size(120).border.clickable{taps++}.padding(24).background(Red)",
        "size(120).border.padding(24).clickable{taps++}.background(Red)"
    )
    val expect = listOf(
        "Red fills 120 dp, Blue sits 24 dp inside it: a red ring.",
        "Padding first, so Red is only the inner 72 dp. No ring.",
        "The first size wins: the box is 100 dp, not 50.",
        "Clickable is outside the padding: tapping the empty margin counts.",
        "Clickable is inside the padding: tapping the margin does nothing."
    )
    val subject: Modifier = when (variant) {
        0 -> Modifier
            .size(120.dp)
            .background(Color.Red)
            .padding(24.dp)
            .background(Color.Blue)

        1 -> Modifier
            .size(120.dp)
            .padding(24.dp)
            .background(Color.Red)

        2 -> Modifier
            .size(100.dp)
            .size(50.dp)
            .background(Color.Red)

        3 -> Modifier
            .size(120.dp)
            .border(1.dp, Color.LightGray)
            .clickable { taps++ }
            .padding(24.dp)
            .background(Color.Red)

        else -> Modifier
            .size(120.dp)
            .border(1.dp, Color.LightGray)
            .padding(24.dp)
            .clickable { taps++ }
            .background(Color.Red)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Variant ${variant + 1} of ${chain.size}: ${chain[variant]}")
        Text(expect[variant])
        Box(Modifier.size(160.dp).border(1.dp, Color.Gray), contentAlignment = Alignment.Center) {
            Box(subject) // the box under test
        }
        Text("taps on the box: $taps")
        Button(onClick = { variant = (variant + 1) % chain.size }) { Text("Next variant") }
    }
}
