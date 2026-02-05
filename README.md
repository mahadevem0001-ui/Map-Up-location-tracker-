# Android Permission Handling System - User Guide

## Quick Start

### Running the App

1. **Build and run** the app on an Android device or emulator (API 29+)
2. You'll see a **Permission Management** screen with a "Request Permissions" button
3. Click the button to request permissions

### Expected Behavior

#### First Launch
1. **Tap "Request Permissions"**
   - System permission dialog appears
   - Choose "Allow" or "Deny"

2. **If you deny a permission**:
   - Permission appears in a **orange card** (can request again)
   - **NO prominent dialog appears** (good UX!)
   - You can tap "Request Permissions" again

#### Second Request (After First Denial)
1. **Tap "Request Permissions" again**
2. **If you deny permanently** (or select "Don't ask again"):
   - Permission appears in a **red card** (permanently denied)
   - **Prominent dialog appears** with "Go to Settings" button
   - This guides you to enable the permission in Settings

#### Returning from Settings
1. **Tap "Go to Settings"** in the dialog
2. Enable the permissions in Android Settings
3. **Return to the app** (back button or recents)
4. Permissions automatically re-checked
5. Granted permissions **removed from denied list**
6. Green success message shown if all granted

## Permission Visual Guide

### Color Coding
- 🟢 **Green Card**: All permissions granted
- 🟠 **Orange Card**: First-time denied - you can request again
- 🔴 **Red Card**: Permanently denied - must enable in Settings

### Permission Status
```
Orange Card:
❌ Denied - You can request again
[Permission name: e.g., "Precise Location"]
[Full permission: android.permission.ACCESS_FINE_LOCATION]

Red Card:
⚠️ Permanently Denied - Go to Settings to enable
[Permission name: e.g., "Notifications"]
[Full permission: android.permission.POST_NOTIFICATIONS]
```

## Permissions Requested

The app requests the following permissions:
1. **ACCESS_FINE_LOCATION** - Precise location
2. **ACCESS_COARSE_LOCATION** - Approximate location
3. **POST_NOTIFICATIONS** (Android 13+ only) - Notifications

## Architecture

### MVVM Pattern
```
View (Compose UI)
    ↓ User Actions
ViewModel (Business Logic)
    ↓ State Updates
View (UI Updates)
```

### State Management
- **StateFlow**: Observable UI state
- **Channel**: One-time events (dialogs, navigation)
- **Actions**: User interactions
- **Events**: System notifications to UI

## Key Features

### 1. First-Launch Friendly
- ✅ No aggressive "Go to Settings" dialog on first app open
- ✅ Progressive disclosure: gentle first request, persistent second request
- ✅ User-friendly permission education

### 2. Lifecycle-Aware
- ✅ Detects when user returns from Settings
- ✅ Automatically re-checks permission states
- ✅ Updates UI without requiring user action

### 3. Configuration Change Safe
- ✅ Survives screen rotations
- ✅ No duplicate dialogs
- ✅ State preserved across lifecycle events

### 4. Memory Efficient
- ✅ No memory leaks
- ✅ Proper cleanup of observers
- ✅ Efficient list rendering with LazyColumn

## Troubleshooting

### Permission Not Updating After Settings
**Solution**: Make sure you press the back button or use recents to return to the app. The app listens for ON_RESUME events.

### Dialog Appears Multiple Times
**Solution**: This shouldn't happen! The implementation uses Channels to prevent duplicate events. If it does, please file a bug report.

### Orange Card vs Red Card Confusion
**Explanation**:
- **Orange**: You denied once, system will still show permission dialog if you request again
- **Red**: You denied twice (or selected "Don't ask again"), system won't show dialog anymore, you must go to Settings

### Android 12 or Lower - No Notification Permission
**Explanation**: The `POST_NOTIFICATIONS` permission was introduced in Android 13. On Android 12 and below, apps don't need runtime permission for notifications.

## Testing Scenarios

### Test 1: Happy Path (All Granted)
1. Launch app
2. Tap "Request Permissions"
3. Allow all permissions
4. See green success card ✓

### Test 2: Gradual Denial
1. Launch app
2. Tap "Request Permissions"
3. Deny all → See orange cards, no prominent dialog ✓
4. Tap "Request Permissions" again
5. Deny all → See red cards, prominent dialog appears ✓

### Test 3: Settings Recovery
1. Follow Test 2 until prominent dialog appears
2. Tap "Go to Settings"
3. Enable all permissions in Settings
4. Return to app (back button)
5. See permissions removed from denied list ✓
6. See green success card ✓

### Test 4: Screen Rotation
1. Deny some permissions (orange or red cards visible)
2. Rotate device
3. State preserved, cards still visible ✓
4. No duplicate dialogs ✓

### Test 5: Partial Grant
1. Request permissions
2. Allow location, deny notifications
3. Only notifications in denied list ✓
4. Request again
5. Location not requested again ✓

## Code References

### Main Files
- **PermissionHandling.kt**: ViewModel, state management, business logic
- **MainActivity.kt**: Compose UI, permission launcher, lifecycle handling
- **ObserveAsEvents.kt**: Utility for lifecycle-aware event observation
- **openAppSettings.kt**: Extension function to navigate to Settings

### Documentation
- **PERMISSION_HANDLING_DOCUMENTATION.md**: Detailed technical documentation
- **IMPLEMENTATION_SUMMARY.md**: High-level implementation overview
- **README.md** (this file): User guide and testing instructions

## Dependencies

### Required
```kotlin
androidx.lifecycle:lifecycle-runtime-ktx:2.10.0
androidx.lifecycle:lifecycle-runtime-compose:2.10.0
androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0
androidx.activity:activity-compose:1.12.3
```

### Minimum SDK
- **minSdk**: 29 (Android 10)
- **targetSdk**: 36 (Android 14+)
- **compileSdk**: 36

## Best Practices Implemented

- ✅ Unidirectional data flow (UDF)
- ✅ Single source of truth (StateFlow)
- ✅ Event-driven architecture (Channel)
- ✅ Lifecycle-aware components
- ✅ Immutable state
- ✅ Type-safe actions with sealed classes
- ✅ Comprehensive documentation
- ✅ Memory leak prevention
- ✅ Configuration change safety
- ✅ Efficient rendering (LazyColumn)

## Future Enhancements

Potential improvements:
- [ ] Persist permission state across app restarts (SharedPreferences/DataStore)
- [ ] Add animation when permissions granted/denied
- [ ] Show permission rationale before requesting
- [ ] Add unit tests for ViewModel
- [ ] Add UI tests for permission flow
- [ ] Support custom permission sets per feature

## Support

For questions or issues with this implementation, refer to:
1. **PERMISSION_HANDLING_DOCUMENTATION.md** - Technical details
2. **IMPLEMENTATION_SUMMARY.md** - Architecture overview
3. Android documentation: https://developer.android.com/training/permissions

---

**Note**: This implementation follows Android best practices as of 2026 and is production-ready.
