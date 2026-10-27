package io.github.genesjc.microclimatefinder.ads

import android.app.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import io.github.genesjc.microclimatefinder.ui.ads.ADS_ENABLED
import org.koin.dsl.module

/**
 * The commercial build's ads. For now (step 1 of the AdMob set) it binds the same do-nothing controller as the open
 * build, so the seam can be built and proved on both flavors before any ads software is added. The next step binds
 * the real controller only when [ADS_ENABLED] is true, and keeps [NoAdsController] for when it is false (the app is
 * not published with ads for now).
 */
val adsModule = module {
    single<AdsController> { NoAdsController() }
}

/** An [AdsController] that never allows an ad and has nothing to start. Temporary in this flavor. */
class NoAdsController : AdsController {

    override val canShowAds: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()

    override val privacyOptionsRequired: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()

    override val adsAreLive: Boolean = false

    override val adFreeUntilMs: StateFlow<Long> = MutableStateFlow(0L).asStateFlow()

    override val canOfferAdFree: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()

    override fun start(activity: Activity) = Unit

    override fun showAdFreeOffer(activity: Activity) = Unit

    override fun showPrivacyOptions(activity: Activity) = Unit
}
