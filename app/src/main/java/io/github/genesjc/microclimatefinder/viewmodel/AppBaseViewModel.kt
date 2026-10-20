package io.github.genesjc.microclimatefinder.viewmodel

import androidx.lifecycle.ViewModel

/**
 * The parent of every screen ViewModel. It exposes app-level values only: no data access and no logic,
 * so a value added here later reaches every screen without touching them.
 */
abstract class AppBaseViewModel : ViewModel() {
    private val config = AppConfig.current

    val isCommercial: Boolean get() = config.isCommercial
    val buildLabel: String get() = config.buildLabel
}
