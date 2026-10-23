package io.github.genesjc.microclimatefinder.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.genesjc.microclimatefinder.R
import io.github.genesjc.microclimatefinder.data.local.StationDatasetInfo
import io.github.genesjc.microclimatefinder.ui.theme.LocalStateUsa

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
