package io.github.genesjc.climatefinder.viewmodel

import androidx.lifecycle.ViewModel
import io.github.genesjc.climatefinder.config.StateUsaDefinition
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * The parent of every screen ViewModel. It exposes app-level values only: no repository, no state holder, no logic and
 * no coroutines, so a value added here later reaches every screen without touching them. It also carries the
 * cross-cutting helper for wording ([string]), which knows nothing about any one feature. It is the one place that
 * is a [KoinComponent]; the concrete ViewModels inherit it and declare their own helpers with `by inject()`.
 */
abstract class AppBaseViewModel : ViewModel(), KoinComponent {
    private val config: AppConfig by inject()
    private val stateUsaDefinition: StateUsaDefinition by inject()

    val isCommercial: Boolean get() = config.isCommercial
    val buildLabel: String get() = config.buildLabel

    /** The USA state the app is built for (its regions and geography); the root hands it to the screens. */
    val stateUsa: StateUsaDefinition get() = stateUsaDefinition

    /** Resolves a string resource: `string(R.string.x, args)`. Wording built from facts is chosen here, not in the data layer. */
    protected val string: StringResolver by inject()
}
