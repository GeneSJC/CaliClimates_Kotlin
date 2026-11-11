package io.github.genesjc.climatefinder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** SANDBOX ONLY (never merged). One start-page button: its label and what happens when it is tapped. */
class Demo(val title: String, val onOpen: () -> Unit)

/** SANDBOX ONLY (never merged). The start page: one button per demo, stacked vertically. */
@Composable
fun StartPage(demos: List<Demo>) {
    PageColumn {
        Text("Sandbox demos", style = MaterialTheme.typography.headlineSmall)
        demos.forEach { demo ->
            Button(onClick = demo.onOpen, modifier = Modifier.fillMaxWidth()) { Text(demo.title) }
        }
    }
}

/** SANDBOX ONLY (never merged). A demo page: a way back to the menu, a title, then the demo itself. */
@Composable
fun DemoPage(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    PageColumn {
        Button(onClick = onBack) { Text("Back to menu") }
        Text(title, style = MaterialTheme.typography.headlineSmall)
        content()
    }
}

/** The shared page layout. Scroll comes before padding, so the padding scrolls with the content. */
@Composable
private fun PageColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        content()
    }
}
