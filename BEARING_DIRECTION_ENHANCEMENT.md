# Bearing/Direction Enhancement for Location Tracking

## Overview
Added bearing (direction) tracking to capture the direction of movement, enabling distance calculations, route visualization, and navigation analytics.

## What Was Added

### 1. **Bearing Field in LocationEntity** 
```kotlin
data class LocationEntity(
    // ...existing fields...
    val bearing: Float? // Direction of travel in degrees (0-360), null if stationary
)
```
- Stored in database for persistence
- Nullable: null when device is stationary

### 2. **Bearing Field in LocationData Domain Model**
```kotlin
data class LocationData(
    // ...existing fields...
    val bearing: Float? = null
) {
    /**
     * Get compass direction from bearing
     * @return Direction string (N, NE, E, SE, S, SW, W, NW) or null if no bearing
     */
    fun getCompassDirection(): String?
}
```
- Added `getCompassDirection()` method to convert bearing to readable directions
- Returns: N, NE, E, SE, S, SW, W, NW

### 3. **LocationUtils Object**
Complete utility class for location calculations:

**Distance Calculations:**
- `calculateDistance(from, to)` - Haversine formula for accurate distance
- `calculateTotalDistance(locations)` - Total distance for a route
- `formatDistance(meters)` - Format as "1.5 km" or "250 m"

**Bearing Calculations:**
- `calculateBearing(from, to)` - Calculate bearing between two points
- Returns bearing in degrees (0-360)

**Speed Calculations:**
- `calculateAverageSpeed(locations)` - Average speed for a route
- Returns speed in m/s

### 4. **Enhanced LocationSession**
Added analytics methods:
```kotlin
data class LocationSession(
    // ...existing fields...
) {
    fun getTotalDistance(): Double
    fun getFormattedDistance(): String
    fun getAverageSpeed(): Double?
    fun getFormattedAverageSpeed(): String
}
```

### 5. **Updated UI Display**
Location items now show:
- 📍 Accuracy (e.g., "10.5m")
- 🚀 Speed (e.g., "45.2 km/h")
- 🧭 Direction (e.g., "N", "SE", "W")

## Bearing Degrees Mapping

| Bearing Range | Direction |
|--------------|-----------|
| 337.5° - 22.5° | N (North) |
| 22.5° - 67.5° | NE (Northeast) |
| 67.5° - 112.5° | E (East) |
| 112.5° - 157.5° | SE (Southeast) |
| 157.5° - 202.5° | S (South) |
| 202.5° - 247.5° | SW (Southwest) |
| 247.5° - 292.5° | W (West) |
| 292.5° - 337.5° | NW (Northwest) |

## Use Cases Enabled

### 1. **Route Visualization**
```kotlin
// Display route with direction arrows
locations.forEach { location ->
    location.bearing?.let { bearing ->
        drawDirectionArrow(location, bearing)
    }
}
```

### 2. **Distance Tracking**
```kotlin
val session: LocationSession = ...
val totalDistance = session.getTotalDistance() // meters
val formatted = session.getFormattedDistance() // "2.5 km"
```

### 3. **Navigation Analytics**
```kotlin
// Calculate bearing between consecutive points
for (i in 0 until locations.size - 1) {
    val bearing = LocationUtils.calculateBearing(
        locations[i], 
        locations[i + 1]
    )
    // Analyze direction changes
}
```

### 4. **Speed Monitoring**
```kotlin
val avgSpeed = session.getAverageSpeed() // m/s
val speedKmh = avgSpeed?.let { it * 3.6 } // km/h
```

## Data Flow

```
Android Location API
        ↓
location.bearing (Float)
        ↓
LocationData (domain model)
        ↓
LocationEntity (database)
        ↓
Persisted in SQLite
```

## Database Schema Update

**locations table now includes:**
```sql
CREATE TABLE locations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sessionId INTEGER NOT NULL,
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    timestamp INTEGER NOT NULL,
    accuracy REAL,
    altitude REAL,
    speed REAL,
    bearing REAL,  -- NEW FIELD
    FOREIGN KEY(sessionId) REFERENCES location_sessions(sessionId) ON DELETE CASCADE
)
```

## UI Enhancements

### Before:
```
📍 Location Item
Lat: 12.345678, Lng: 78.901234
11:44:56
Accuracy: 10.5m
```

### After:
```
📍 Location Item
Lat: 12.345678, Lng: 78.901234
11:44:56
📍 10.5m  🚀 45.2 km/h  🧭 NE
```

## Mathematical Formulas Used

### Haversine Formula (Distance)
```
a = sin²(Δlat/2) + cos(lat1) × cos(lat2) × sin²(Δlon/2)
c = 2 × atan2(√a, √(1−a))
d = R × c
```
Where R = Earth's radius (6,371 km)

### Bearing Formula
```
y = sin(Δlon) × cos(lat2)
x = cos(lat1) × sin(lat2) − sin(lat1) × cos(lat2) × cos(Δlon)
bearing = atan2(y, x)
```

## Files Modified

1. ✅ **LocationEntity.kt** - Added bearing field
2. ✅ **LocationData.kt** - Added bearing + getCompassDirection()
3. ✅ **LocationMappers.kt** - Updated to map bearing
4. ✅ **LocationViewModel.kt** - Captures bearing from GPS
5. ✅ **LocationTrackingScreen.kt** - Displays bearing/speed/accuracy
6. ✅ **LocationSession.kt** - Added distance/speed analytics
7. ✅ **LocationUtils.kt** - NEW: Complete utility for calculations

## Benefits

✅ **Accurate Distance Tracking** - Haversine formula for spherical distances  
✅ **Route Visualization** - Can draw direction arrows on maps  
✅ **Navigation Insights** - Track heading changes and turns  
✅ **Speed Analytics** - Average speed and velocity vectors  
✅ **User-Friendly Display** - Compass directions (N, SE, etc.)  
✅ **Persistent Storage** - Bearing saved in database  
✅ **Null Safety** - Handles stationary devices gracefully  

## Future Enhancements

### 1. **Route Simplification**
Use bearing changes to detect significant turns and simplify routes:
```kotlin
fun simplifyRoute(locations: List<LocationData>, angleThreshold: Double = 15.0)
```

### 2. **Turn Detection**
Detect left/right turns based on bearing changes:
```kotlin
fun detectTurns(locations: List<LocationData>): List<Turn>
```

### 3. **Geofencing**
Use bearing to predict when user will enter/exit geofence:
```kotlin
fun predictGeofenceEntry(location: LocationData, bearing: Float, geofence: Geofence)
```

### 4. **Map Visualization**
Draw route with direction arrows:
```kotlin
fun drawRouteWithDirections(canvas: Canvas, locations: List<LocationData>)
```

## Status

✅ Bearing field added to all layers  
✅ Database schema updated  
✅ Mappers updated  
✅ ViewModel captures bearing  
✅ UI displays direction  
✅ Distance/speed utilities implemented  
✅ Compass direction conversion  
✅ All files compile successfully  

**Ready for production use!** 🚀
