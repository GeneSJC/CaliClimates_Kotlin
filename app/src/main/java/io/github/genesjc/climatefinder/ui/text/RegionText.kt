package io.github.genesjc.climatefinder.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.genesjc.climatefinder.R
import io.github.genesjc.climatefinder.domain.model.StationDatasetInfo
import io.github.genesjc.climatefinder.ui.theme.LocalStateUsa

/** The dataset's region name, shown as its title. */
@Composable
fun datasetTitleText(dataset: StationDatasetInfo): String =
    LocalStateUsa.current.region(dataset.regionId)?.nameRes?.let { stringResource(it) } ?: dataset.regionId

/** "Bay Area / Sacramento • 60 stations" */
@Composable
fun datasetHeaderText(dataset: StationDatasetInfo): String = stringResource(
    R.string.dataset_header,
    datasetTitleText(dataset),
    pluralStringResource(R.plurals.station_count, dataset.stationCount, dataset.stationCount)
)
