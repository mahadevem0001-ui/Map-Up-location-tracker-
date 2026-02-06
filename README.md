# MapUp - Android Developer Assessment

## Setup
- Prereqs: Android Studio Ladybug+ with JDK 17, Android SDK 34, Google Play Services, and an emulator or device with location turned on.
- Secrets: set `MAPS_API_KEY` in `local.properties` (or through your environment) so the manifest placeholder resolves for Maps.
- Build/run: open the project root in Android Studio, let it sync Gradle, then run the `app` configuration. CLI: `./gradlew :app:installDebug` and launch on a connected device/emulator.
- Permissions to grant: fine + coarse + background location, notifications (Android 13+). For automatic reboot restart, grant "Allow all the time" and allow auto-start/battery-whitelist on OEM skins.
- Optional exports: CSV/GPX are written to app-scoped storage and surfaced from the drawer actions on the tracking screen.

## Architecture Overview
- UI: Jetpack Compose screens for permissions and tracking; drawer scaffold hosts exports and theme toggle. See [app/src/main/java/com/mahi/kr/mapup_androiddeveloperassessment/MainActivity.kt](app/src/main/java/com/mahi/kr/mapup_androiddeveloperassessment/MainActivity.kt) and [app/src/main/java/com/mahi/kr/mapup_androiddeveloperassessment/feature/location/presentation/screen/LocationTrackingScreen.kt](app/src/main/java/com/mahi/kr/mapup_androiddeveloperassessment/feature/location/presentation/screen/LocationTrackingScreen.kt).
- State: ViewModels expose immutable state + event flows; permissions gate the tracking UI. See [app/src/main/java/com/mahi/kr/mapup_androiddeveloperassessment/feature/location/presentation/viewmodel/LocationViewModel.kt](app/src/main/java/com/mahi/kr/mapup_androiddeveloperassessment/feature/location/presentation/viewmodel/LocationViewModel.kt) and [app/src/main/java/com/mahi/kr/mapup_androiddeveloperassessment/feature/permission/presentation/screen/PermissionScreen.kt](app/src/main/java/com/mahi/kr/mapup_androiddeveloperassessment/feature/permission/presentation/screen/PermissionScreen.kt).
- Services: Foreground `LocationService` streams locations, geocodes with timeout fallback, and persists sessions; restarts on BOOT_COMPLETED when allowed. See [app/src/main/java/com/mahi/kr/mapup_androiddeveloperassessment/feature/location/data/service/LocationService.kt](app/src/main/java/com/mahi/kr/mapup_androiddeveloperassessment/feature/location/data/service/LocationService.kt) and [app/src/main/java/com/mahi/kr/mapup_androiddeveloperassessment/feature/location/data/receiver/BootRestartReceiver.kt](app/src/main/java/com/mahi/kr/mapup_androiddeveloperassessment/feature/location/data/receiver/BootRestartReceiver.kt).
- Data: Room stores `LocationSession` and `LocationData`; DataStore keeps tracking flag/interval/theme. See [app/src/main/java/com/mahi/kr/mapup_androiddeveloperassessment/core/data/local/AppPreferencesManager.kt](app/src/main/java/com/mahi/kr/mapup_androiddeveloperassessment/core/data/local/AppPreferencesManager.kt).
- DI: Koin modules provide clients, repositories, and use cases.

## What Changed (concise)
- Tracking resilience: foreground service persists tracking state/interval and can resume after process death or reboot when background permission is granted.
- Session integrity: stop handlers close active sessions, clear state, and emit UI events for permission/provider loss.
- Logging and observability: LocationService, LocationViewModel, PermissionScreen, and MainActivity log lifecycle and cleanup paths.
- UI safety: permission revocation stops service and clears active session; interval changes persist and restore; snackbar events flow through `LocationEvent`.
- Theming/UX: theme toggle persists; drawer shows sessions and exports; map screen lists sessions and locations.

## Known Limitations
- Background restart requires ACCESS_BACKGROUND_LOCATION and OEM auto-start/battery exemptions; without it, BootRestartReceiver will not relaunch the service after reboot.
- Geocoder may return null when timing out (2s) or offline; notification/location rows will omit address in that case.
- OEMs with aggressive task killers may still stop the foreground service; advise whitelisting if persistent tracking is needed.
- Exports write to app-scoped storage; sharing relies on installed handlers that can read the provided URI grants.

## Demo Links
- Video walkthrough: https://www.loom.com/share/fca93eff0ed542f49173e4a8301c4f85
- APK (debug build): https://drive.google.com/drive/folders/1-WijCzNYXNc-MgkJC734dyzDcNMkSbnL?usp=sharing
