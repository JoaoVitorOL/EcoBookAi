# Android UI Review

Date: `2026-05-23`

## Official References Consulted

- State hoisting: https://developer.android.com/develop/ui/compose/state-hoisting
- Accessibility API defaults: https://developer.android.com/develop/ui/compose/accessibility/api-defaults?hl=en
- Adaptive layouts for different screen sizes: https://developer.android.com/develop/ui/compose/layouts/adaptive/support-different-screen-sizes?hl=en

## What Was Reviewed

- screen-level Compose layout structure
- accessibility semantics for custom clickable/toggleable components
- responsiveness on wider displays
- Android lint output for navigation/runtime issues
- local run/build instructions for the Android app

## Changes Applied

1. Added an adaptive content container for the main flows so forms and lists stop stretching edge-to-edge on larger displays.
2. Expanded the tap target semantics for consent rows by making the full label + control row toggleable for `Checkbox` and `Switch`.
3. Gave the custom filter chips selection semantics through `toggleable`, which improves accessibility services feedback.
4. Gave material cards an explicit button role and click label for assistive technologies.
5. Fixed the Compose Navigation lint bug in `NavGraph` by remembering `getBackStackEntry(...)` with the destination entry as key.
6. Removed a silent main-thread preference write by replacing `SharedPreferences.commit()` with `apply()` in the runtime backend override helper.
7. Removed the obsolete `SDK_INT >= O` check in the FCM service because the app already has `minSdk = 26`.
8. Removed two unused Android resources from `api_config.xml`.
9. Hardened `Invoke-GradleAsciiPath.ps1` so temporary ASCII drive aliases no longer race each other under concurrent Gradle entry points.

## Validation

- `powershell -ExecutionPolicy Bypass -File .\EcoBookAiAndroid\scripts\Invoke-GradleAsciiPath.ps1 app:compileDebugKotlin`
- `powershell -ExecutionPolicy Bypass -File .\EcoBookAiAndroid\scripts\Invoke-GradleAsciiPath.ps1 app:lintDebug`
- `powershell -ExecutionPolicy Bypass -File .\EcoBookAiAndroid\scripts\Invoke-GradleAsciiPath.ps1 app:testDebugUnitTest`

Result:

- Android lint: `0` errors
- Android lint warnings: `20`
- Remaining warnings are dependency freshness plus `targetSdk = 34`; no new runtime/lifecycle/accessibility blocker remained after this review

## Suggestions For The Next Round

1. Upgrade `targetSdk` and the AndroidX/Firebase dependency set in a controlled batch, because lint still flags stale versions.
2. Expand the adaptive review to tablets/foldables with screenshot validation in landscape and split-screen.
3. Move more visible UI strings to `res/values/strings.xml` if localization becomes a short-term goal.
4. Consider an explicit edge-to-edge/insets pass in `MainActivity` and the form screens once the design is frozen.
5. Consider adopting more explicit screen-level state holders for the busiest flows if the profile/discovery screens grow further, to stay aligned with the state-hoisting guidance used in this review.
6. Plan a small security-storage refresh, because the current `EncryptedSharedPreferences`/`MasterKey` usage now emits deprecation warnings during the validated Android assemble path.
