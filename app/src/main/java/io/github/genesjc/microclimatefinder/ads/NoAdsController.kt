package io.github.genesjc.microclimatefinder.ads

import android.app.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * An [AdsController] that never allows an ad and has nothing to start: every flag is false and every call does nothing.
 * It lives in shared code so every build can use it. The open (foss) build always binds it; the commercial build binds
 * it while ads are switched off (the `adsEnabled` build setting), so the screens, which show an ad only when a flag
 * says so, need no change either way.
 */
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
