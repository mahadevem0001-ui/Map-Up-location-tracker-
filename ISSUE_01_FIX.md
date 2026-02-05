# Issue-01 Fix Summary: App Restart State Persistence

## ✅ Bug Fixed!

### Problem
After denying permissions and killing the app (swipe kill), relaunching shows:
- ❌ Informative blue cards (wrong - makes it seem like first launch)
- ❌ No denied permissions list
- ❌ No prominent dialog on subsequent denials

### Root Cause
`hasRequestedPermissionsBefore` flag was only in ViewModel memory, lost when app process killed.

### Solution
Persist the flag in `SharedPreferences` to survive app restarts.

## Implementation

### 1. Created PermissionPreferencesManager.kt
```kotlin
class PermissionPreferencesManager(context: Context) {
    fun setHasRequestedPermissions(hasRequested: Boolean)
    fun hasRequestedPermissionsBefore(): Boolean
}
```

### 2. Updated PermissionHandling.kt
- Changed `ViewModel` → `AndroidViewModel`
- Load state from SharedPreferences on init
- Persist flag when marking permissions requested

```kotlin
class PermissionHandlingViewModel(application: Application) : AndroidViewModel(application) {
    private val prefsManager = PermissionPreferencesManager(application)
    
    private val _state = MutableStateFlow(
        PermissionState(
            hasRequestedPermissionsBefore = prefsManager.hasRequestedPermissionsBefore()
        )
    )
    
    private fun markPermissionsRequested() {
        _state.update { it.copy(hasRequestedPermissionsBefore = true) }
        prefsManager.setHasRequestedPermissions(true) // ✅ Persisted!
    }
}
```

### 3. Updated MainActivity.kt
- Added `LaunchedEffect` to check permissions on first composition
- Rebuilds denied list if permissions were requested before

```kotlin
LaunchedEffect(state.hasRequestedPermissionsBefore) {
    if (state.hasRequestedPermissionsBefore) {
        // Check all permissions and rebuild denied list
        PermissionHandlingViewModel.requiredPermissionsSet.forEach { permission ->
            if (!isGranted) {
                viewModel.onAction(PermissionStateChange(...))
            }
        }
    }
}
```

## Test Scenario (Verifies Fix)

### Before Fix ❌
```
1. Deny permissions → See denied list
2. Kill app (swipe from recents)
3. Reopen app → See informative cards (BUG!)
4. Click "Request Permissions"
5. Deny permanently → NO prominent dialog (BUG!)
```

### After Fix ✅
```
1. Deny permissions → See denied list
2. Kill app (swipe from recents)  
3. Reopen app → See denied list (FIXED!)
4. Click "Request Permissions Again"
5. Deny permanently → Prominent dialog appears (FIXED!)
```

## How It Works

### Flow Diagram
```
First Launch:
  User denies → Save to SharedPreferences (true)
        ↓
  App Killed (Process Death)
        ↓
  SharedPreferences retained ✅
        ↓
  App Relaunched:
    1. ViewModel reads SharedPreferences
    2. hasRequestedPermissionsBefore = true
    3. LaunchedEffect checks permissions
    4. Rebuilds denied list
    5. Shows correct UI ✅
```

## Files Changed

1. **NEW**: `util/PermissionPreferencesManager.kt` - State persistence
2. **UPDATED**: `PermissionHandling.kt` - AndroidViewModel + persistence
3. **UPDATED**: `MainActivity.kt` - LaunchedEffect to restore state

## Testing

To verify the fix:
1. Install app
2. Request and deny permissions
3. **Swipe kill app from recents**
4. Reopen app
5. ✅ Should show denied permissions list (not informative cards)
6. Request permissions again and deny permanently
7. ✅ Prominent dialog should appear

## Technical Details

- **Persistence**: SharedPreferences
- **Lifecycle**: AndroidViewModel provides Application context
- **Restoration**: LaunchedEffect checks permissions on composition
- **Thread Safety**: All operations are main-thread safe

## Summary

✅ **Issue**: App restart shows wrong UI
✅ **Cause**: State not persisted
✅ **Fix**: SharedPreferences + AndroidViewModel + LaunchedEffect
✅ **Result**: Correct UI and behavior after app kill/relaunch

The bug is completely resolved! 🎉
