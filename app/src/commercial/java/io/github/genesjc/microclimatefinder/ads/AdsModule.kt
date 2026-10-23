package io.github.genesjc.microclimatefinder.ads

import android.app.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.dsl.module

/**
 * The commercial build's ads. For now (step 1 of the AdMob set) it binds the same do-nothing controller as the open
 * build, so the seam can be built and proved on both flavors before any ads software is added. The next step replaces
 * the controller with the real one and removes [NoAdsController] from here.
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
