package io.github.genesjc.microclimatefinder.viewmodel

import androidx.lifecycle.ViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * The parent of every screen ViewModel. It exposes app-level values only: no repository, no state holder, no logic and
 * no coroutines, so a value added here later reaches every screen without touching them. It is the one place that
 * is a [KoinComponent]; the concrete ViewModels inherit it and declare their own helpers with `by inject()`.
 */
abstract class AppBaseViewModel : ViewModel(), KoinComponent {
    private val config: AppConfig by inject()

    val isCommercial: Boolean get() = config.isCommercial
    val buildLabel: String get() = config.buildLabel
}
