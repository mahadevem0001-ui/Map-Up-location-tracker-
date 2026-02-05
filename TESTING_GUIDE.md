# Room Database Integration - Testing Guide

## What Was Integrated

### ✅ **LocationViewModel Updates**
1. **Constructor** - Now accepts `LocationSessionRepository`
2. **Database Loading** - `loadSessionsFromDatabase()` loads all sessions reactively
3. **Active Session Check** - `checkForActiveSession()` restores active session on app restart
4. **Session Creation** - `startLocationService()` saves new session to database
5. **Location Saving** - Each location update is saved to database via `addLocationToSession()`
6. **Session Ending** - `stopLocationService()` updates session with end time
7. **Clear All** - `clearAllSessions()` deletes all sessions from database

### 🔄 **Data Flow**

#### Starting Tracking:
```
User clicks "Start" 
  → Create LocationSession
  → Save to database (repository.createSession)
  → Start LocationService
  → Collect location updates
  → Each location saved (repository.addLocationToSession)
  → UI updates from database Flow
```

#### Stopping Tracking:
```
User clicks "Stop"
  → Update session with endTime
  → Save to database (repository.updateSession)
  → Stop LocationService
  → Cancel location updates
  → UI shows completed session
```

#### App Restart:
```
App launches
  → Load sessions from database (reactive Flow)
  → Check for active session
  → If active: restore session and resume tracking
  → UI shows all persisted sessions
```

## Testing Checklist

### 🧪 Test 1: Basic Session Creation
- [ ] Click "Start Tracking"
- [ ] Verify session appears in UI
- [ ] Wait for location updates
- [ ] Verify locations appear under session
- [ ] Click "Stop Tracking"
- [ ] Verify session shows end time

### 🧪 Test 2: Persistence After App Kill
- [ ] Start tracking
- [ ] Collect some locations
- [ ] Kill app from recent apps
- [ ] Relaunch app
- [ ] **Expected:** Session still shows with all locations
- [ ] **Expected:** If was active, shows as active

### 🧪 Test 3: Multiple Sessions
- [ ] Start session 1, collect locations, stop
- [ ] Start session 2, collect locations, stop
- [ ] Start session 3, collect locations, stop
- [ ] **Expected:** All 3 sessions visible in UI
- [ ] **Expected:** Each has correct timestamps and locations

### 🧪 Test 4: Direction/Bearing Display
- [ ] Start tracking and move around
- [ ] Verify bearing displayed as compass direction (N, SE, etc.)
- [ ] Verify speed displayed in km/h
- [ ] Verify accuracy displayed in meters

### 🧪 Test 5: Session Analytics
- [ ] Complete a session with multiple locations
- [ ] Expand session details
- [ ] Verify distance calculation shows (e.g., "2.5 km")
- [ ] Verify average speed shows (e.g., "35.2 km/h")
- [ ] Verify duration shows (e.g., "05:30")

### 🧪 Test 6: Clear All Sessions
- [ ] Create multiple sessions
- [ ] Click "Clear All" button
- [ ] **Expected:** All sessions deleted from UI
- [ ] Kill and relaunch app
- [ ] **Expected:** Sessions still cleared (database deleted)

### 🧪 Test 7: Error Handling
- [ ] Start tracking without location permissions
- [ ] **Expected:** Error message shows
- [ ] Revoke location permission while tracking
- [ ] **Expected:** Service stops, error displayed

### 🧪 Test 8: Service State Persistence
- [ ] Start tracking
- [ ] Note notification is showing
- [ ] Kill app
- [ ] Relaunch app
- [ ] **Expected:** UI shows "Running" state
- [ ] **Expected:** Notification still visible
- [ ] **Expected:** Locations continue to be collected

## Database Verification

### Check Database with ADB:
```bash
# Pull database from device
adb pull /data/data/com.mahi.kr.mapup_androiddeveloperassessment/databases/location_tracking.db

# Or use Android Studio Device File Explorer:
# View → Tool Windows → Device File Explorer
# Navigate to: data/data/com.mahi.kr.mapup_androiddeveloperassessment/databases/
```

### Inspect Tables:
```sql
-- View all sessions
SELECT * FROM location_sessions ORDER BY startTime DESC;

-- View locations for a session
SELECT * FROM locations WHERE sessionId = [SESSION_ID] ORDER BY timestamp ASC;

-- Count total locations
SELECT COUNT(*) FROM locations;

-- View session with location count
SELECT 
    s.sessionId,
    s.startTime,
    s.endTime,
    COUNT(l.id) as locationCount
FROM location_sessions s
LEFT JOIN locations l ON s.sessionId = l.sessionId
GROUP BY s.sessionId;
```

## Expected UI Behavior

### Session Display:
```
🔴 Active Session              02:15
Started: Feb 05, 2026 11:44:56
15 locations

[Show Locations ▼]
  📍 Lat: 12.345678, Lng: 78.901234
     11:44:56
     📍 10.5m  🚀 45.2 km/h  🧭 NE
```

### Completed Session:
```
📍 Session                     05:30  
Started: Feb 05, 2026 11:30:00
Ended: Feb 05, 2026 11:35:30
42 locations | Distance: 2.5 km

[Hide Locations ▲]
```

## Common Issues & Solutions

### Issue 1: Room Not Generating Code
**Solution:** Run `./gradlew clean build` to force KSP code generation

### Issue 2: Database Not Found
**Solution:** Uninstall app and reinstall to create fresh database

### Issue 3: Sessions Not Loading
**Solution:** Check Logcat for database errors, verify Koin module is loaded

### Issue 4: Bearing Shows Null
**Solution:** Ensure device is moving, bearing is null when stationary

### Issue 5: Duplicate Sessions
**Solution:** Check `isServiceRunning()` check in `startLocationService()`

## Logcat Filters

```
# Filter for database operations
adb logcat | grep -i "room"

# Filter for location updates
adb logcat | grep -i "location"

# Filter for ViewModel logs
adb logcat | grep -i "LocationViewModel"

# Filter for Koin DI
adb logcat | grep -i "koin"
```

## Performance Metrics

### Expected Performance:
- **Database Write:** < 10ms per location
- **Database Read:** < 50ms for 100 sessions
- **UI Update:** Real-time with Flow
- **Memory:** < 50MB for 1000 locations
- **Battery:** ~5-10% per hour tracking

### Monitor Performance:
```bash
# Monitor memory usage
adb shell dumpsys meminfo com.mahi.kr.mapup_androiddeveloperassessment

# Monitor battery usage
adb shell dumpsys batterystats com.mahi.kr.mapup_androiddeveloperassessment
```

## Success Criteria

✅ Sessions persist across app restarts  
✅ Location updates saved in real-time  
✅ Bearing/direction displayed correctly  
✅ Distance and speed calculated accurately  
✅ UI responsive with Flow updates  
✅ No memory leaks (< 100MB for extended use)  
✅ Clear all successfully deletes from database  
✅ Active session restored on app restart  

## Next Steps After Testing

1. ✅ **Verify persistence** - Kill app, verify data retained
2. ✅ **Test edge cases** - No permissions, no GPS signal
3. ✅ **Performance test** - Track for 1 hour, check battery/memory
4. ✅ **UI polish** - Add loading states, better error messages
5. ✅ **Add export** - Export sessions to GPX/KML format
6. ✅ **Add map view** - Visualize routes on Google Maps

## Status

✅ ViewModel integrated with repository  
✅ All CRUD operations implemented  
✅ Reactive data flow with Flow  
✅ Error handling with Result type  
✅ Bearing/direction tracking active  
⏳ **Building project with KSP...**  
⏳ **Ready for testing!**
