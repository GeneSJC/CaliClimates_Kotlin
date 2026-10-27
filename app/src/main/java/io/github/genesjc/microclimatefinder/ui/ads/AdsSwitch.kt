package io.github.genesjc.microclimatefinder.ui.ads

/**
 * The one switch for ads in the app. While it is false (the decision for now: the app is not published with ads), no
 * banner is drawn, no ad-free offer or privacy row is shown, and the commercial build binds the controller that does
 * nothing, so no consent form is shown and no ad is requested. All the ads work stays in the code: set it to true, build
 * the commercial flavor, and everything the AdMob set added is live again. The open (foss) build never has ads, whatever
 * this says. Like the dark mode flag in MenuDrawer.kt, it is a constant, so the choice is made in the code and reviewed.
 */
const val ADS_ENABLED = false
