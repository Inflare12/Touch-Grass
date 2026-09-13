# Touch Grass — Audit & Fix Report

Date: 2026-09-13
Branch/PR: `audit/fix-touch-grass-v1` / PR #1

## Fixed

- Fixed foreground-app intervention so a monitored app is only intercepted after its configured daily UsageStats limit is exceeded.
- Added per-package intervention throttling instead of one global throttle.
- Added proper coroutine lifecycle cleanup to the Accessibility Service.
- Hardened camera error handling and stopped silently swallowing frame/binding failures.
- Made liveness require a temporal sequence of camera motion instead of treating the first frame as live.
- Added static-photo/screen-replay heuristic rejection messages.
- Made grass detection conservative by requiring lower-frame ground evidence and exposing `bottomGreenRatio`.
- Improved tree-canopy and artificial-turf heuristics.
- Hardened contact verification so it requires repeated live, grass, hand, downward-approach, and contact evidence rather than only a hand Y coordinate.
- Made contact hold duration respect the user's configured setting.
- Added regression tests for photo cheating and false contact verification.
- Replaced the fake rewarded-ad timer with Google's real rewarded-ad API using Google's official test app/ad IDs.
- Added AdMob SDK initialization and manifest application ID metadata.
- Added an explicit in-app Accessibility disclosure/consent step before opening Android Accessibility settings.
- Removed `QUERY_ALL_PACKAGES`; app selection now uses launcher-visible applications and requires explicit user opt-in for monitoring.
- Removed misleading fake “time saved” and fake ad-bypass duration metrics.
- Validated stored app limits to 1–1440 minutes.
- Added GitHub Actions Android assemble/unit-test workflow.

## Architecture reviewed

- Kotlin + Jetpack Compose UI
- CameraX preview/analysis
- UsageStats monitoring
- Accessibility foreground-app intervention
- Room persistence
- DataStore settings
- Local camera processing
- Verification state machine
- Rewarded ads
- Notifications/foreground monitoring
- Onboarding/settings/app selection/stats/home/challenge flows
- Unit/Robolectric/instrumentation test structure
- Gradle/version catalog/manifest/release configuration

## Important limitation

The current grass and hand detectors are lightweight local heuristics, not trained semantic vision models. The new verifier is substantially harder to fool with a static image or a simple green object, but it is **not a cryptographic liveness system** and cannot guarantee detection of sophisticated video replay, artificial grass, or every non-grass green surface.

A production-grade anti-cheat release should replace the heuristic grass/hand components with a trained/bundled on-device model and add device-level replay/3D challenge testing.

## Verification status

Static source audit: completed.

GitHub Actions workflow: added, but no workflow run was exposed by the connected GitHub integration during this audit, so a successful CI build/test result cannot honestly be claimed here.

Still required before Play Store release:

1. Build the project in Android Studio or a CI runner.
2. Install on a physical Android device.
3. Test UsageStats permission and exact daily-limit behavior.
4. Test Accessibility intervention across target Android versions/OEMs.
5. Test overlay behavior and battery/background restrictions.
6. Test CameraX permission, rotation, low light, outdoor light, and device-specific cameras.
7. Test photo, screen replay, artificial turf, tree, green wall, and no-contact scenarios.
8. Test rewarded ads with Google's test ad units, then configure production AdMob IDs and consent/age/privacy settings before release.
9. Review Google Play AccessibilityService and foreground-service declarations before submission.
10. Replace the test AdMob app/ad IDs with production IDs.
