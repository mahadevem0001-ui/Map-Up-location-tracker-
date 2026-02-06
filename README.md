# MapUp - Android Developer Assessment

## What changed (concise)
- Tracking resilience: foreground service persists tracking state and interval to DataStore and resumes after process death or device reboot via BootRestartReceiver.
- Session integrity: service stop handlers and ViewModel close active sessions, clear state, and emit UI events for permission/provider loss.
- Logging and observability: LocationService, LocationViewModel, PermissionScreen, and MainActivity log start/stop, permission loss, provider status, and session cleanup.
- UI safety: permission revocation stops service and clears active session; interval changes persist and restore; snackbar events routed via LocationEvent channel.
- Theming/UX: theme toggle uses DataStore; drawer scaffold shows sessions and exports; map screen shows sessions and locations.

## How it is handled
- Data persistence: location sessions stored in Room; tracking flags/interval stored in DataStore via AppPreferencesManager.
- Background tracking: LocationService runs as a foreground service with notification, handles ACTION_START/ACTION_STOP, and throttles notification updates.
- Process death/reboot: service reads persisted flags on null intent; BootRestartReceiver restarts service on BOOT_COMPLETED/LOCKED_BOOT_COMPLETED when tracking was active.
- Permission/provider changes: ViewModel watches location stream/provider monitor; on revoke/disable it stops service, closes sessions, clears state, and emits events to UI.
- Interval control: interval updates restart collection and persist to DataStore; restored on ViewModel init.
- Exports: CSV/GPX exports built from current + past sessions.

## Key files
- Location service: [app/src/main/java/com/mahi/kr/mapup_androiddeveloperassessment/feature/location/data/LocationService.kt](app/src/main/java/com/mahi/kr/mapup_androiddeveloperassessment/feature/location/data/LocationService.kt)
- Boot restart: [app/src/main/java/com/mahi/kr/mapup_androiddeveloperassessment/feature/location/data/BootRestartReceiver.kt](app/src/main/java/com/mahi/kr/mapup_androiddeveloperassessment/feature/location/data/BootRestartReceiver.kt)
- ViewModel/state/events: [app/src/main/java/com/mahi/kr/mapup_androiddeveloperassessment/feature/location/presentation/viewmodel/LocationViewModel.kt](app/src/main/java/com/mahi/kr/mapup_androiddeveloperassessment/feature/location/presentation/viewmodel/LocationViewModel.kt) and [app/src/main/java/com/mahi/kr/mapup_androiddeveloperassessment/feature/location/presentation/model/LocationEvent.kt](app/src/main/java/com/mahi/kr/mapup_androiddeveloperassessment/feature/location/presentation/model/LocationEvent.kt)
- UI screens: [app/src/main/java/com/mahi/kr/mapup_androiddeveloperassessment/feature/location/presentation/screen/LocationTrackingScreen.kt](app/src/main/java/com/mahi/kr/mapup_androiddeveloperassessment/feature/location/presentation/screen/LocationTrackingScreen.kt) and [app/src/main/java/com/mahi/kr/mapup_androiddeveloperassessment/feature/permission/presentation/screen/PermissionScreen.kt](app/src/main/java/com/mahi/kr/mapup_androiddeveloperassessment/feature/permission/presentation/screen/PermissionScreen.kt)
- DataStore manager: [app/src/main/java/com/mahi/kr/mapup_androiddeveloperassessment/core/data/local/AppPreferencesManager.kt](app/src/main/java/com/mahi/kr/mapup_androiddeveloperassessment/core/data/local/AppPreferencesManager.kt)



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

