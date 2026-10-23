package io.github.genesjc.microclimatefinder.ui.ads

import android.app.Activity
import io.github.genesjc.microclimatefinder.ads.AdsController
import io.github.genesjc.microclimatefinder.testutil.FakeAdsController
import io.github.genesjc.microclimatefinder.testutil.StopKoinRule
import io.github.genesjc.microclimatefinder.testutil.restartKoin
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** The ViewModel only passes the controller's flags and calls through; the controller is the build flavor's. */
class AdsViewModelTest {

    @get:Rule
    val stopKoin = StopKoinRule()

    private val controller = FakeAdsController()
    private val activity = mockk<Activity>(relaxed = true)

    private fun viewModel(): AdsViewModel {
        restartKoin { single<AdsController> { controller } }
        return AdsViewModel()
    }

    @Test
    fun theFlagsFollowTheController() {
        val vm = viewModel()
        assertFalse(vm.canShowAds.value)
        controller.canShowAds.value = true
        assertTrue(vm.canShowAds.value)
        controller.canOfferAdFree.value = true
        assertTrue(vm.canOfferAdFree.value)
        controller.privacyOptionsRequired.value = true
        assertTrue(vm.privacyOptionsRequired.value)
        controller.adFreeUntilMs.value = 1_234L
        assertEquals(1_234L, vm.adFreeUntilMs.value)
    }

    @Test
    fun theOfferCallReachesTheController() {
        val vm = viewModel()
        vm.showAdFreeOffer(activity)
        vm.showAdFreeOffer(activity)
        assertEquals(2, controller.adFreeOffersShown)
    }

    @Test
    fun nothingIsReadFromTheControllerUntilAskedFor() {
        // Nothing is registered: building the ViewModel must not need the controller yet
        restartKoin { }
        assertFalse(AdsViewModel().toString().isEmpty())
    }
}
