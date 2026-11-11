package io.github.genesjc.climatefinder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Palette. Each colour marks one thing, so a line in the listing matches what it draws.
private val Orange = Color(0xFFFF9800) // first background
private val Green = Color(0xFF4CAF50)  // second background
private val Yellow = Color(0xFFFFEB3B) // the border added inside the variants
private val Cyan = Color(0xFF00BCD4)   // the fixed frame around the box under test

private val FrameSize = 140.dp // sized so two frames fit side by side on a 360 dp-wide screen

/** One line of a modifier chain: the text shown on screen, an optional colour for it, and what it applies. */
private class Step(val code: String, val color: Color? = null, val apply: (Modifier) -> Modifier)

/** One variant: what to expect, and the chain of steps from top to bottom. */
private class Variant(val expect: String, val steps: List<Step>) {
    val isClickable = steps.any { it.code.startsWith("clickable") }
}

private fun size(dp: Int) = Step("size($dp.dp)") { it.size(dp.dp) }
private fun padding(dp: Int) = Step("padding($dp.dp)") { it.padding(dp.dp) }
private fun background(name: String, color: Color) = Step("background($name)", color) { it.background(color) }
private fun yellowBorder() = Step("border(4.dp, Yellow)", Yellow) { it.border(4.dp, Yellow) }
private fun clickable(onTap: () -> Unit) = Step("clickable { taps++ }") { it.clickable { onTap() } }

/** The five variants; onTap(i) is called when variant i is tapped. */
private fun variants(onTap: (Int) -> Unit) = listOf(
    Variant(
        "Orange fills the 100 dp box, Green sits 20 dp inside it: an orange ring.",
        listOf(size(100), background("Orange", Orange), padding(20), background("Green", Green))
    ),
    Variant(
        "Padding comes first, so Orange is only the inner 60 dp. No ring.",
        listOf(size(100), padding(20), background("Orange", Orange))
    ),
    Variant(
        "The first size wins: the box is 80 dp, not 40.",
        listOf(size(80), size(40), background("Orange", Orange))
    ),
    Variant(
        "clickable sits above the padding, so tapping the empty margin inside the yellow border counts.",
        listOf(size(100), yellowBorder(), clickable { onTap(3) }, padding(20), background("Orange", Orange))
    ),
    Variant(
        "clickable sits below the padding, so tapping the margin does nothing; only the orange counts.",
        listOf(size(100), yellowBorder(), padding(20), clickable { onTap(4) }, background("Orange", Orange))
    )
)

/**
 * SANDBOX ONLY (never merged). Modifier order demo: two variants side by side, and each tap on the
 * button slides the window by one (1+2, 2+3, 3+4, 4+5, 5+1). Each modifier wraps the result of the
 * one before it, so order changes the outcome.
 */
@Composable
fun ModifierOrderDemo() {
    var left by rememberSaveable { mutableStateOf(0) }
    val taps = remember { mutableStateListOf(0, 0, 0, 0, 0) } // one tap count per variant
    val all = variants(onTap = { i -> taps[i] = taps[i] + 1 })
    val right = (left + 1) % all.size
    val listLines = 1 + all.maxOf { it.steps.size } // the "Modifier" line plus the longest chain

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Mode: Variants ${left + 1} and ${right + 1} of ${all.size}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VariantPane(all[left], taps[left], listLines, Modifier.weight(1f))
            VariantPane(all[right], taps[right], listLines, Modifier.weight(1f))
        }
        Text("Cyan border: the fixed frame around each box", color = Cyan, fontSize = 12.sp)
        Button(onClick = { left = (left + 1) % all.size }) { Text("Next variants") }
    }
}

/**
 * One column: its modifier chain, then the frame with the box under test, then what to expect.
 * listLines reserves the same height for every chain, so the two frames line up.
 */
@Composable
private fun VariantPane(variant: Variant, taps: Int, listLines: Int, modifier: Modifier = Modifier) {
    // The box under test is built from the same steps that are listed above it
    val subject = variant.steps.fold<Step, Modifier>(Modifier) { chain, step -> step.apply(chain) }
    // The modifier chain, one line per modifier, coloured to match what it draws
    val listing = buildAnnotatedString {
        append("Modifier")
        variant.steps.forEach { step ->
            append("\n")
            withStyle(SpanStyle(color = step.color ?: Color.Unspecified)) { append(".${step.code}") }
        }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(listing, fontFamily = FontFamily.Monospace, fontSize = 12.sp, minLines = listLines)
        Box(Modifier.size(FrameSize).border(4.dp, Cyan), contentAlignment = Alignment.Center) {
            Box(subject) // the box under test
        }
        Text(variant.expect, fontSize = 13.sp)
        if (variant.isClickable) Text("taps on the box: $taps", fontSize = 13.sp)
    }
}
