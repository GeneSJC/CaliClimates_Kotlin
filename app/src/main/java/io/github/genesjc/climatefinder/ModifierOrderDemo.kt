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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Palette. Each colour marks one thing, so a line in the listing matches what it draws.
private val Orange = Color(0xFFFF9800) // first background
private val Green = Color(0xFF4CAF50)  // second background
private val Yellow = Color(0xFFFFEB3B) // the border added inside the variants
private val Cyan = Color(0xFF00BCD4)   // the fixed 160 dp frame around the box under test

/** One line of a modifier chain: the text shown on screen, an optional colour for it, and what it applies. */
private class Step(val code: String, val color: Color? = null, val apply: (Modifier) -> Modifier)

/** One variant: what to expect, and the chain of steps from top to bottom. */
private class Variant(val expect: String, val steps: List<Step>)

private fun size(dp: Int) = Step("size($dp.dp)") { it.size(dp.dp) }
private fun padding(dp: Int) = Step("padding($dp.dp)") { it.padding(dp.dp) }
private fun background(name: String, color: Color) = Step("background($name)", color) { it.background(color) }
private fun yellowBorder() = Step("border(4.dp, Yellow)", Yellow) { it.border(4.dp, Yellow) }
private fun clickable(onTap: () -> Unit) = Step("clickable { taps++ }") { it.clickable { onTap() } }

private fun variants(onTap: () -> Unit) = listOf(
    Variant(
        "Orange fills the 120 dp box, Green sits 24 dp inside it: an orange ring.",
        listOf(size(120), background("Orange", Orange), padding(24), background("Green", Green))
    ),
    Variant(
        "Padding comes first, so Orange is only the inner 72 dp. No ring.",
        listOf(size(120), padding(24), background("Orange", Orange))
    ),
    Variant(
        "The first size wins: the box is 100 dp, not 50.",
        listOf(size(100), size(50), background("Orange", Orange))
    ),
    Variant(
        "clickable sits above the padding, so tapping the empty margin inside the yellow border counts.",
        listOf(size(120), yellowBorder(), clickable(onTap), padding(24), background("Orange", Orange))
    ),
    Variant(
        "clickable sits below the padding, so tapping the margin does nothing; only the orange counts.",
        listOf(size(120), yellowBorder(), padding(24), clickable(onTap), background("Orange", Orange))
    )
)

/**
 * SANDBOX ONLY (never merged). Modifier order demo: tap the button to cycle through five variants.
 * Each modifier wraps the result of the one before it, so order changes the outcome.
 */
@Composable
fun ModifierOrderDemo() {
    var index by rememberSaveable { mutableStateOf(0) }
    var taps by rememberSaveable { mutableStateOf(0) }
    val all = variants(onTap = { taps++ })
    val current = all[index]
    // The box under test is built from the same steps that are listed below it
    val subject = current.steps.fold<Step, Modifier>(Modifier) { chain, step -> step.apply(chain) }
    val mono = FontFamily.Monospace

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Mode: Variant ${index + 1} of ${all.size}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(current.expect)

        // The modifier chain, one line per modifier, coloured to match what it draws
        Column {
            Text("Modifier", fontFamily = mono, fontSize = 14.sp)
            current.steps.forEach { step ->
                Text(
                    ".${step.code}",
                    fontFamily = mono,
                    fontSize = 14.sp,
                    color = step.color ?: Color.Unspecified,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }

        Box(Modifier.size(160.dp).border(4.dp, Cyan), contentAlignment = Alignment.Center) {
            Box(subject) // the box under test
        }
        Text("Cyan border: the fixed 160 dp frame", color = Cyan, fontSize = 12.sp)
        Text("taps on the box: $taps")
        Button(onClick = { index = (index + 1) % all.size }) { Text("Next variant") }
    }
}
