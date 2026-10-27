package io.github.genesjc.microclimatefinder.ads

import org.koin.dsl.module

/**
 * The open (foss) build's ads: none. The commercial flavor has a file with the same name and package that supplies the
 * real controller when ads are switched on, so the shared code asks for an [AdsController] and never knows which build
 * it is in. The open build ignores the ads switch: it never has ads.
 */
val adsModule = module {
    single<AdsController> { NoAdsController() }
}
