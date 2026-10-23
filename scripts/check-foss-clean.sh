#!/usr/bin/env bash
# Proves the open (foss) build contains no advertising software, and that this check can tell:
# the commercial build must contain it. Run from the repository root. A single transitive dependency can
# bring an ads library back, so run it whenever dependencies change. This app has two flavor dimensions (edition
# and distribution), so the variants are coreFoss and coreCommercial.
#   scripts/check-foss-clean.sh              the full check
#   scripts/check-foss-clean.sh --foss-only  skips the positive controls (only until the commercial build has the
#                                            ads library; remove this option once it has)
set -euo pipefail

FOSS_ONLY=false
[ "${1:-}" = "--foss-only" ] && FOSS_ONLY=true

fail() { echo "FAIL: $1" >&2; exit 1; }

echo "== Dependencies"
./gradlew --quiet :app:dependencies --configuration coreFossReleaseRuntimeClasspath > /tmp/foss-deps.txt
if ! $FOSS_ONLY; then
  ./gradlew --quiet :app:dependencies --configuration coreCommercialReleaseRuntimeClasspath > /tmp/commercial-deps.txt
  grep -Eiq "play-services-ads|user-messaging-platform" /tmp/commercial-deps.txt \
    || fail "the commercial build does not list the ads libraries, so this check cannot be trusted"
fi
if grep -Ei "play-services-ads|user-messaging-platform" /tmp/foss-deps.txt; then
  fail "the foss build lists an ads library"
fi

echo "== Merged manifests"
./gradlew --quiet :app:processCoreFossReleaseMainManifest :app:processCoreCommercialReleaseMainManifest
manifest_of() {
  # The merged manifest of the variant; falls back to any manifest under a folder named for it
  local found
  found=$(find app/build/intermediates -path "*merged_manifest*/$1/*" -name AndroidManifest.xml | head -1)
  [ -n "$found" ] || found=$(find app/build/intermediates -path "*/$1/*" -name AndroidManifest.xml | head -1)
  echo "$found"
}
foss_manifest=$(manifest_of coreFossRelease)
commercial_manifest=$(manifest_of coreCommercialRelease)
[ -n "$foss_manifest" ] || fail "no merged manifest found for foss"
[ -n "$commercial_manifest" ] || fail "no merged manifest found for commercial"
if ! $FOSS_ONLY; then
  grep -q "com.google.android.gms.ads.APPLICATION_ID" "$commercial_manifest" \
    || fail "the commercial manifest has no AdMob app id, so this check cannot be trusted"
fi
if grep -Ei "gms.ads|android.permission.AD_ID|ACCESS_ADSERVICES" "$foss_manifest"; then
  fail "the foss manifest has ads entries"
fi

echo "OK: the foss build contains no ads software."
