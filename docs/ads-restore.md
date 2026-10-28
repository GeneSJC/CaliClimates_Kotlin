# Ads, parked: what is kept and how to bring it into develop

**Status (2026-10-27).** The app is not being published with ads for now. **None of the ads work is on `develop`:** there is
no ads code, no ads library and no ads manifest entry on it, so the `commercial` build is ads-free. The work so far, and
the kit it was based on, is kept by a tag so ads can be brought back later without starting again.

- **Tag:** `ads-parked-2026-10-27` (annotated; it never moves), pointing at commit
  `ab64422eaab918656f732b3a22339f5deb99ec39` (Build 97.3, on top of `develop` at Build 96.1, `f2151d2`). If the tag is ever
  lost, the SHA still works: `git show ab64422:<path>`.
- **Everything below that says "in the tag" is not on `develop`.** Read a file without checking anything out with
  `git show ads-parked-2026-10-27:<path>`; for example the kit's own guide is
  `git show ads-parked-2026-10-27:docs/ads-kit/README.md`, and the tag's copy of this guide is
  `git show ads-parked-2026-10-27:docs/ads-restore.md`.
- **Do not merge the tag.** Treat it as a frozen reference and restore by copying files onto a fresh branch (below).

## Honest status of the parked work

- **Written, never built.** Step 1 of the plan (the seam, with no ads software) is written, and it has never been built
  or run: the environment it was written in could not run Gradle. Before relying on it, build both distributions and run
  both unit-test tasks (`testCoreFossDebugUnitTest`, `testCoreCommercialDebugUnitTest`).
- **The real ads are not written.** Steps 2 to 4 (the AdMob controller, consent, banner, rewarded video, the ad-free
  hour, the Gradle ids and production switch, the screen placement) were planned but not started. The code for them is in
  the kit under `docs/ads-kit/`, which came from another project's working implementation (in the tag). Its authors say it was not
  compiled in this form and its SDK class names were verified only by their own builds.

## What is in the tag

Seam (no ads library; compiles without one):

| File | Role |
|---|---|
| `ads/AdsController.kt` | The only thing shared code knows about ads: flags (`canShowAds`, `privacyOptionsRequired`, `adsAreLive`, `adFreeUntilMs`, `canOfferAdFree`) and three calls that need an Activity. |
| `ads/NoAdsController.kt` | The do-nothing implementation, in shared code so every build can bind it. |
| `foss/.../ads/AdsModule.kt` | Binds `NoAdsController`. The open build never has ads. |
| `commercial/.../ads/AdsModule.kt` | Temporary: also binds `NoAdsController`; the one place the ads switch is to be read. |
| `foss` and `commercial` `.../ui/ads/AdBanner.kt` | Empty banner with the signature the real one will have. |
| `ui/ads/AdsViewModel.kt` | On `AppBaseViewModel`; exposes the flags and the offer calls to the screens. |
| `ui/ads/AdBannerSlot.kt`, `AdFreeOffer.kt` | The animated banner slot, the offer line and `adFreeMinutesLeft`. Not placed on any screen yet. |
| `ui/ContextExtensions.kt` | `findActivity()` for the real banner. |
| `CaliClimates.kt`, `MainActivity.kt` | Load `adsModule`; call `adsViewModel.start(this)` once. |
| `res/values/strings_ui.xml` | `menu_privacy_options`, `action_hide_ads`, `ad_free_remaining`, `ad_banner_offline`. |
| Tests | `NoAdsControllerTest`, `AdsViewModelTest`, `AdFreeMinutesLeftTest`, `AdsBoundaryTest` (nothing outside `src/commercial` may import an AdMob or UMP package, or read the ads switch), `FakeAdsController`; `ViewModelCreationTest` also covers `AdsViewModel` and `AdsController`. |
| `scripts/check-foss-clean.sh` | Proves the open build has no ads library or manifest entry; adapted to the two flavor dimensions (`coreFoss`, `coreCommercial`). `--foss-only` skips the positive controls until the commercial build has the library. |

The ads switch (Build 97.3): `adsEnabled` in `app/build.gradle.kts` (a private property read from `local.properties`,
`~/.gradle/gradle.properties` or `-PadsEnabled=true`; off by default), exposed as `BuildConfig.ADS_ENABLED` in the
`commercial` flavor only, and a line printed at the start of every build saying whether ads are on or off.

The kit, verbatim, in the tag under `docs/ads-kit/` (16 source files, Gradle and string snippets, the foss-clean script, two design
documents). Package placeholders: `PACKAGE_PATH` in paths and `__PACKAGE__` in code; this app's package is
`io.github.genesjc.microclimatefinder`. The kit's `README.md` is its own integration guide.

## Before bringing ads back (the reasons it was parked)

1. An AdMob account in good standing, and the app (`io.github.genesjc.microclimatefinder.commercial`) registered with a banner unit and a
   rewarded unit.
2. A privacy policy that covers advertising, and a consent message configured in AdMob for the EU and UK.
3. The store declarations: the Play Console asks whether the app contains ads and whether it uses the advertising ID.
   The Google ads library adds the advertising-ID permission to the merged manifest even if the app never lists it.
   Answer from what the shipped file contains. Check the current policy text before submitting.
4. Read `docs/ads-kit/docs/ads-testing-rules.md`: test builds only, never tap your own live ad, never ask anyone to, and
   check the account standing first.

## How to restore

Do not merge the tag. Copy files from the tag onto a fresh branch from the then-current `develop`:

1. `git checkout develop && git pull`, then `git checkout -b feature-set/ads` (and a working branch for each step).
2. Copy the **new** files from the tag. In this repository's layout they are:
   ```
   git checkout ads-parked-2026-10-27 -- \
     app/src/main/java/io/github/genesjc/microclimatefinder/ads \
     app/src/main/java/io/github/genesjc/microclimatefinder/ui/ads \
     app/src/main/java/io/github/genesjc/microclimatefinder/ui/ContextExtensions.kt \
     app/src/foss/java/io/github/genesjc/microclimatefinder/ads \
     app/src/foss/java/io/github/genesjc/microclimatefinder/ui/ads \
     app/src/commercial/java/io/github/genesjc/microclimatefinder/ads \
     app/src/commercial/java/io/github/genesjc/microclimatefinder/ui/ads \
     app/src/test/java/io/github/genesjc/microclimatefinder/ads \
     app/src/test/java/io/github/genesjc/microclimatefinder/ui/ads \
     app/src/test/java/io/github/genesjc/microclimatefinder/testutil/FakeAdsController.kt \
     scripts/check-foss-clean.sh \
     docs/ads-kit
   ```
3. **Hand-merge** the six existing files the parked work changed (do not overwrite them; `develop` will have moved on):
   - `CaliClimates.kt`: `modules(appModule, adsModule)` and the `adsModule` import.
   - `MainActivity.kt`: `private val adsViewModel: AdsViewModel by viewModels()` and `adsViewModel.start(this)` at the top of
     `onCreate`, with the two imports.
   - `res/values/strings_ui.xml`: the four ad strings (an "Ads" block before the Regions screen block).
   - `androidTest/.../ViewModelCreationTest.kt`: `AdsViewModel` and `AdsController` in the two lists.
   - `app/build.gradle.kts`: the ads switch block, the `ADS_ENABLED` build-config field in the `commercial` flavor, and the
     ads line in the label-printing task. See the commit "Ads switch as a Gradle setting" on the tag.
   - `README.md`: the "Ads" section under Build variants.
4. Build both distributions and run both test tasks (name both flavors in every Gradle task; bare names are ambiguous).
   Run `scripts/check-foss-clean.sh --foss-only`.
5. Then carry on with the plan's steps 2 to 4 (below). Check the current versions of the Google Mobile Ads library and
   how it fits the Android Gradle plugin and compile SDK in use; the kit pins `play-services-ads 25.4.0` in its snippet.

## Steps 2 to 4 (not done): what to do with the kit

Use the kit files as they are, after the package swap, then build; expect to adjust an SDK name or two.

- **Commercial implementation.** Copy from `docs/ads-kit/files/commercial/`: `AdFreePeriod.kt`, `AdMobAdsController.kt`,
  the real `AdBanner.kt` (replacing the empty one) and `AndroidManifest.xml` (the AdMob app-id meta-data), and
  `files/testCommercial/` (`AdFreePeriodTest`). Replace the commercial `AdsModule` with the kit's, bound only when
  `BuildConfig.ADS_ENABLED` is true, `NoAdsController` otherwise. Put `commercialImplementation(libs.play.services.ads)` and
  the version-catalog lines from `docs/ads-kit/snippets/dependencies.txt` in. Add the Gradle blocks from
  `snippets/build.gradle.kts.advertising.txt` (sample ids, the production switch, the three-id guard, `privateProperty`:
  this app already has `privateProperty` and `localProperties` from the ads switch, so reuse them) and the three
  `buildConfigField`s from `snippets/build.gradle.kts.flavors.txt` (keep this app's commercial application id), and the
  `manifestPlaceholders["admobAppId"]` line in each build type. Tie the production flag to the switch: a production build
  only counts when `adsEnabled` is true, so with ads off no real ids are needed.
- **Backups.** This app has `allowBackup="true"` but the manifest does not reference the two backup rule files in
  `res/xml`, which are still empty samples. Add `android:fullBackupContent="@xml/backup_rules"` and
  `android:dataExtractionRules="@xml/data_extraction_rules"` and the exclusion of `ad_free_prefs.xml` from
  `snippets/backup-rules.txt`, so a restored backup cannot hand out an ad-free hour.
- **Placement.** The banner slot at the bottom of Station List and Regional Sections first (Home, a full-screen map, as its
  own small step to judge on a device), the offer line under the Station List title plus a menu-drawer row, the
  privacy-options row (shown only when `privacyOptionsRequired`), `LIVE ADS` in the drawer's build label when `adsAreLive`.
  Check system insets: the app is not edge-to-edge today and targets SDK 37.
- **Tests.** `AdFreePeriodTest` (src/testCommercial) under virtual time; the shared tests are already here.
- **Docs.** Adapt `docs/ads-kit/docs/ADS_DESIGN_NOTES.md` into this project's conventions.

## Decisions already made

- One `AdsViewModel` (on `AppBaseViewModel`), not the controller added to each screen ViewModel, so `HomeViewModel` keeps its
  four helpers.
- The ads switch is a Gradle setting read in one place (the commercial `adsModule`), off by default, not a constant in the
  code and not a product flavor.
- The ad-free period is one hour. The offer is worded only as time without ads; it never asks anyone to tap or view an ad.
- The ad-free end time is kept in its own small preferences file excluded from backups, an exception to this app's one
  settings store.
- Open at parking time: whether the ads library should ship in the commercial build while ads are off. Parking the SDK out
  of the build avoids the question; if ads return in a build with the switch off, decide it then.

## Effort estimate

With the kit, the restore is a few hours of copying and adjusting, then a build-and-fix pass for the SDK names, then a
device check (consent form, test banner, sample video, offline text, the banner returning after the hour). The account,
policy and store declarations take longer than the code.
