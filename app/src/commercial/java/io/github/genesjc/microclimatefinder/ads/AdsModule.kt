package io.github.genesjc.microclimatefinder.ads

import org.koin.dsl.module

/**
 * The commercial build's ads, and the one place the ads switch (`adsEnabled`, which becomes BuildConfig.ADS_ENABLED in
 * this flavor only) is read. Ads are off by default, so this binds the do-nothing controller. The next step of the AdMob
 * set adds the real controller here: bound when ADS_ENABLED is true, with [NoAdsController] otherwise.
 */
val adsModule = module {
    single<AdsController> { NoAdsController() }
}
