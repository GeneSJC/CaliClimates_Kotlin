package io.github.genesjc.microclimatefinder.ui.ads

import android.app.Activity
import io.github.genesjc.microclimatefinder.ads.AdsController
import io.github.genesjc.microclimatefinder.viewmodel.AppBaseViewModel
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.inject

/**
 * The screens' one interface to advertising: the flags they read and the calls they make, all passed through to the
 * [AdsController] the build flavor supplies. MainActivity calls [start] once at launch. An Activity is handed over for
 * a call only (the consent form and the video need one) and is never kept, so nothing leaks across a configuration
 * change. In the open build every flag is off and every call does nothing.
 */
class AdsViewModel : AppBaseViewModel() {

    private val adsController: AdsController by inject()

    /** True once consent allows requesting ads, no ad-free period is running and the ads software is ready. */
    val canShowAds: StateFlow<Boolean> get() = adsController.canShowAds

    /** True when the app must offer a way to reopen the consent choices. */
    val privacyOptionsRequired: StateFlow<Boolean> get() = adsController.privacyOptionsRequired

    /** True only for a production build with real ad units; shown in the build label. */
    val adsAreLive: Boolean get() = adsController.adsAreLive

    /** When the ad-free period ends, in epoch milliseconds; 0 or in the past when there is none. */
    val adFreeUntilMs: StateFlow<Long> get() = adsController.adFreeUntilMs

    /** True when the user can be offered the ad-free period now. */
    val canOfferAdFree: StateFlow<Boolean> get() = adsController.canOfferAdFree

    /** Runs the consent flow and starts advertising. Does nothing in the open build. */
    fun start(activity: Activity) = adsController.start(activity)

    /** Plays the rewarded video; finishing it starts the ad-free period. */
    fun showAdFreeOffer(activity: Activity) = adsController.showAdFreeOffer(activity)

    /** Reopens the consent choices. */
    fun showPrivacyOptions(activity: Activity) = adsController.showPrivacyOptions(activity)
}
