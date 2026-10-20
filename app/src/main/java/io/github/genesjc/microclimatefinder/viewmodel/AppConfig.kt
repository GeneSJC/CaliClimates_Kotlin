package io.github.genesjc.microclimatefinder.viewmodel

import io.github.genesjc.microclimatefinder.BuildConfig
import io.github.genesjc.microclimatefinder.BuildInfo

/** The build's constants, read once here and nowhere else. */
data class AppConfig(
    val isCommercial: Boolean,
    val buildLabel: String
) {
    companion object {
        val current = AppConfig(
            isCommercial = BuildConfig.IS_COMMERCIAL,
            buildLabel = BuildInfo.LABEL
        )
    }
}
