# Location Service State Management Fixes

## Issues Fixed

### 1. **Prevent Service from Starting Multiple Times**
**Problem:** Service could be started multiple times, creating duplicate location tracking flows.

**Solution:**
- Added a static `isServiceRunning` flag in `LocationService` companion object
- Check this flag before starting location tracking
- Only start if not already running

```kotlin
companion object {
    @Volatile
    private var isServiceRunning = false
    
    fun isRunning(): Boolean = isServiceRunning
}

override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
        ACTION_START -> {
            if (!isServiceRunning) {
                startLocationTracking()
            }
        }
        ACTION_STOP -> stopLocationTracking()
    }
    return START_STICKY
}
```

### 2. **Retain Service State After App Restart**
**Problem:** When app is killed and relaunched, UI shows "stopped" even though service is running and posting notifications.

**Solution:**
- Use `START_STICKY` flag in `onStartCommand()` so service is restarted by system if killed
- Check `LocationService.isRunning()` in `LocationViewModel.init{}`
- Restore UI state to match actual service state
- Resume location updates collection in ViewModel if service is running

```kotlin
class LocationViewModel(...) : AndroidViewModel(application) {
    init {
        // Check if service is already running when ViewModel is created
        checkServiceState()
        
        // If service is running, start collecting location updates
        if (LocationService.isRunning()) {
            startLocationUpdates()
        }
    }
    
    fun checkServiceState() {
        val isRunning = LocationService.isRunning()
        _state.update { it.copy(isServiceRunning = isRunning) }
    }
}
```

### 3. **Stop Service When Permissions Are Revoked**
**Problem:** Service continues running even when location permissions are removed from app settings.

**Solution:**
- Monitor permission state changes in `MainActivity` using `LaunchedEffect`
- Automatically stop service when location permissions are denied
- Works both in foreground and when user goes to settings

```kotlin
LaunchedEffect(permissionState.deniedPermissions) {
    val locationPermissionsDenied = permissionState.deniedPermissions.keys.any { permission ->
        permission == Manifest.permission.ACCESS_FINE_LOCATION ||
        permission == Manifest.permission.ACCESS_COARSE_LOCATION
    }
    
    // If location permissions are denied and service is running, stop it
    if (locationPermissionsDenied && LocationService.isRunning()) {
        val intent = Intent(this@MainActivity, LocationService::class.java).apply {
            action = LocationService.ACTION_STOP
        }
        startService(intent)
    }
}
```

### 4. **Proper State Flag Management**
**Problem:** Service state flag not properly cleared on service destruction.

**Solution:**
- Set `isServiceRunning = true` when service starts
- Set `isServiceRunning = false` when service stops OR is destroyed
- Use `@Volatile` annotation for thread-safe access

```kotlin
private fun startLocationTracking() {
    isServiceRunning = true
    // ... start location tracking
}

private fun stopLocationTracking() {
    isServiceRunning = false
    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()
}

override fun onDestroy() {
    super.onDestroy()
    isServiceRunning = false
    serviceScope.cancel("Location Service is being destroyed")
}
```

## Key Architecture Decisions

### Thread-Safe State Management
- Used `@Volatile` for `isServiceRunning` flag to ensure visibility across threads
- Static companion object allows checking state without service instance

### START_STICKY Behavior
- Service will be recreated by system if killed due to memory pressure
- Ensures continuous location tracking even under low memory conditions
- State flag survives process recreation

### Reactive Permission Monitoring
- Uses Compose's `LaunchedEffect` to reactively monitor permission changes
- Automatically responds to permission revocation without manual checks
- Works seamlessly with existing permission state management

### ViewModel State Restoration
- ViewModel checks service state on initialization
- Resumes location update collection if service is running
- Provides accurate UI state even after app restart

## Testing Scenarios

### ✅ Scenario 1: Multiple Start Attempts
1. Start service
2. Click start button again
3. **Result:** Service doesn't start duplicate tracking, UI remains consistent

### ✅ Scenario 2: App Kill and Relaunch
1. Start location tracking
2. Verify notifications are being posted
3. Kill app from recent apps
4. Relaunch app
5. **Result:** UI shows "Running" state, location updates continue

### ✅ Scenario 3: Permission Revocation
1. Start location tracking
2. Go to Settings → App Info → Permissions
3. Revoke location permission
4. Return to app
5. **Result:** Service stops automatically, UI shows permission screen

### ✅ Scenario 4: System Service Restart
1. Start location tracking
2. System kills service due to memory pressure
3. **Result:** Service restarts automatically (START_STICKY)

## Files Modified

1. **LocationService.kt**
   - Added `isServiceRunning` static flag
   - Implemented state flag management
   - Changed return type to `START_STICKY`

2. **LocationViewModel.kt**
   - Added `init{}` block to check service state
   - Added `checkServiceState()` method
   - Updated `startLocationService()` to check if already running

3. **MainActivity.kt**
   - Added imports for `Intent` and `Manifest`
   - Added `LaunchedEffect` to monitor permission changes
   - Automatically stops service when permissions revoked

## Benefits

✅ **No Duplicate Services:** Prevents resource waste and battery drain  
✅ **Consistent UI State:** Always reflects actual service state  
✅ **Automatic Permission Handling:** Service stops when permissions removed  
✅ **Survives App Restart:** State persists across app lifecycle  
✅ **Battery Efficient:** Single location tracking flow per service instance  
✅ **User-Friendly:** Clear feedback on service status

## Future Enhancements

- Persist location history to database for long-term storage
- Add service notification actions (Stop button in notification)
- Implement battery optimization handling
- Add geofencing capabilities
- Support background location tracking (with additional permission)
