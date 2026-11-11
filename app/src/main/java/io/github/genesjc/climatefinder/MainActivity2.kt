package io.github.genesjc.climatefinder

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** The pages of the sandbox. */
private enum class Screen { Start, Remember, Modifiers }

/**
 * SANDBOX ONLY (feature-set/walkthroughSandbox, never merged).
 * The sandbox's single Activity: a start page and the demo pages, swapped by one state variable,
 * the same single-activity idea as the real app's AppNavigation. Filter Logcat by tag "Mini".
 */
class MainActivity2 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // required; also restores saved state for rememberSaveable
        // null on a fresh launch, non-null when the system recreates us (e.g. rotation)
        Log.d("Mini", "onCreate, savedInstanceState = ${if (savedInstanceState == null) "null (fresh)" else "non-null (recreated)"}")

        setContent { // builds a ComposeView and starts the composition; this lambda re-runs on recomposition
            Log.d("Mini", "setContent lambda ran") // runs again each time the page changes, because it reads `screen`
            var screen by rememberSaveable { mutableStateOf(Screen.Start) } // which page is showing
            BackHandler(enabled = screen != Screen.Start) { screen = Screen.Start } // system back goes to the menu
            val toMenu = { screen = Screen.Start }
            // Fixed gray background and white text, whatever the system dark mode is (the real app does this with its theme)
            Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF3C4043), contentColor = Color.White) {
                when (screen) {
                    Screen.Start -> StartPage(
                        onRemember = { screen = Screen.Remember },
                        onModifiers = { screen = Screen.Modifiers }
                    )
                    Screen.Remember -> DemoPage("remember vs rememberSaveable", onBack = toMenu) { RememberDemo() }
                    Screen.Modifiers -> DemoPage("Modifier order", onBack = toMenu) { ModifierOrderDemo() }
                }
            }
        }
    }
}
