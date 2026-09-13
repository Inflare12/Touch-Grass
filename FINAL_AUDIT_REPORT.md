# Touch Grass — Final Audit Report

Date: 2026-09-13

## Scope

Full repository source audit of the Android app, including Gradle configuration, manifest, UsageStats monitoring, AccessibilityService intervention, Room/DataStore persistence, CameraX analysis, local grass/hand/liveness detectors, contact verification state machine, rewarded ads, onboarding/settings flows, and existing tests.

## Fixes applied

1. **Limit enforcement** — Accessibility intervention checks the configured daily limit before launching the intervention.
2. **Rewarded bypass correctness** — A successful rewarded ad now grants a persisted, per-package 10-minute grace period so the same app is not immediately blocked again. The blocked package is also recorded correctly.
3. **UsageStats accuracy** — daily usage buckets for the same package are summed rather than taking only the maximum bucket.
4. **Temporal verification** — contact verification requires repeated evidence for liveness, grass, hand, approach, contact, and the configured hold duration.
5. **Liveness** — the first camera frame is not considered live; a short temporal motion sequence is required. Static-photo and basic screen-replay heuristics are rejected.
6. **Grass detection** — lower-frame ground evidence, tree-canopy detection, and artificial-turf heuristics reduce obvious false positives. This remains a heuristic detector, not a trained semantic segmentation model.
7. **Hand detection** — current skin segmentation plus temporal movement is retained as a lightweight offline fallback. It is not equivalent to a hand-landmark model.
8. **Camera errors** — frame and camera binding failures are logged instead of silently disappearing.
9. **Ads** — rewarded bypass uses the real Google Mobile Ads rewarded-ad API; development uses Google's test identifiers.
10. **Privacy/package visibility** — broad package visibility was removed; app selection uses launcher-visible apps and monitoring requires explicit user opt-in.
11. **Accessibility disclosure** — the app explains why Accessibility access is needed before directing the user to Android settings.
12. **Metrics** — misleading synthetic time-saved and fake ad-duration metrics were removed.
13. **Testing** — regression coverage includes verification behavior and rewarded-bypass expiry.
14. **CI** — GitHub Actions builds the debug APK and runs unit tests on JDK 17/Gradle 9.3.1.

## Release blockers that cannot be completed from source control alone

- Production AdMob app/unit identifiers must be supplied by the owner and configured instead of Google's test identifiers.
- A production signing key must be configured outside the repository; private signing credentials must never be committed.
- The app must be built and installed on real Android hardware.
- UsageStats and Accessibility behavior must be tested on target Android versions/OEMs.
- Camera verification must be tested in bright/dim conditions, different camera hardware, rotation, and real outdoor scenes.
- Anti-cheat must be tested with photos, screen replay, artificial grass, trees, green walls, and no-contact attempts.
- Google Play Console declarations and policy eligibility for AccessibilityService, foreground-service usage, ads, privacy, and target SDK must be reviewed before submission.

## Security/privacy conclusion

No camera frame is intentionally uploaded by the local verification path. The verification design is local-first. The app still requests sensitive Android capabilities (UsageStats and AccessibilityService), so the Play Store disclosure and consent UX must accurately describe their use.

## Final engineering assessment

**Source quality: ready for CI/device validation.**

**Public Play Store release: not yet certified.** The remaining blockers require real device/Play Console/production account configuration and cannot honestly be marked complete by static GitHub inspection.
