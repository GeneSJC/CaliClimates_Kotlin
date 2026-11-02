package io.github.genesjc.climatefinder.config

/** The build's constants as one immutable value. The module builds it once from the build configuration. */
data class AppConfig(
    val isCommercial: Boolean,
    val buildLabel: String
)
