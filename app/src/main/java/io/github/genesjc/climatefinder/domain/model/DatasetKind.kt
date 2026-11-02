package io.github.genesjc.climatefinder.domain.model

/** Where a dataset's stations come from. The screens turn this into words; the data layer only reports it. */
enum class DatasetKind { BUNDLED_PACK, CSV_OVERRIDE, NONE }
