package io.github.genesjc.climatefinder.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import io.github.genesjc.climatefinder.config.StateUsaCA
import io.github.genesjc.climatefinder.config.StateUsaDefinition

/**
 * The USA state the screens are drawn for: its regions, outline and picker artwork. The root provides the registered
 * definition (from the dependency container, through the root ViewModel); the default only serves previews and tests.
 */
val LocalStateUsa = staticCompositionLocalOf<StateUsaDefinition> { StateUsaCA }
