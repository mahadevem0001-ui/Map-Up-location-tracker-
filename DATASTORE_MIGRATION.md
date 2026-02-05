# DataStore Migration: Unified Preferences Management

## ✅ Migration Complete!

### Decision: Single Unified DataStore

**Chosen Approach**: All preferences in a single `AppPreferencesManager` class using DataStore

### Why Single Unified Manager? ✅

#### Advantages:
1. **✅ Single Source of Truth** - All preferences in one place
2. **✅ Consistency** - Same API pattern across the app
3. **✅ Performance** - Single DataStore instance (better than multiple)
4. **✅ Maintainability** - Easier to manage and update
5. **✅ Type Safety** - All preference keys defined in one place
6. **✅ Testing** - Easier to mock and test
7. **✅ Reactive** - Flow-based API for all preferences
8. **✅ Thread Safe** - DataStore handles concurrency automatically

#### Disadvantages of Multiple Managers:
- ❌ Multiple DataStore instances (overhead)
- ❌ Scattered preference management
- ❌ Inconsistent APIs
- ❌ Harder to maintain
- ❌ More complex testing

## Implementation

### Old Structure (BEFORE) ❌
```
PreferencesManager.kt (SharedPreferences)
└── hasRequestedPermissionsBefore: Boolean

ThemePreferencesManager.kt (DataStore)
└── isDarkMode: Flow<Boolean?>
```

### New Structure (AFTER) ✅
```
AppPreferencesManager.kt (DataStore)
├── isDarkMode: Flow<Boolean?>
├── hasRequestedPermissionsBefore: Flow<Boolean>
└── Future preferences can be added here
```

## Files Created/Modified

### New File ✅
**AppPreferencesManager.kt** - Unified preferences manager
```kotlin
class AppPreferencesManager(context: Context) {
    // Theme Preferences
    val isDarkMode: Flow<Boolean?>
    suspend fun setDarkMode(isDark: Boolean?)
    suspend fun toggleTheme(currentIsDark: Boolean)
    
    // Permission Preferences
    val hasRequestedPermissionsBefore: Flow<Boolean>
    suspend fun setHasRequestedPermissions(hasRequested: Boolean)
    
    // Utility Functions
    suspend fun clearAll()
    suspend fun clearThemePreferences()
    suspend fun clearPermissionPreferences()
}
```

### Updated Files ✅
1. **ThemeViewModel.kt** - Uses AppPreferencesManager
2. **PermissionViewModel.kt** - Uses AppPreferencesManager with Flow

### Files to Delete ⚠️
- ❌ `PreferencesManager.kt` (old SharedPreferences version)
- ❌ `ThemePreferencesManager.kt` (separate DataStore version)

## Key Changes

### 1. Permission Preferences Migration

**Before (SharedPreferences):**
```kotlin
class PreferencesManager(context: Context) {
    fun hasRequestedPermissionsBefore(): Boolean  // Synchronous
    fun setHasRequestedPermissions(hasRequested: Boolean)
}
```

**After (DataStore):**
```kotlin
class AppPreferencesManager(context: Context) {
    val hasRequestedPermissionsBefore: Flow<Boolean>  // Reactive Flow
    suspend fun setHasRequestedPermissions(hasRequested: Boolean)
}
```

### 2. PermissionViewModel Updated

**Before:**
```kotlin
// Synchronous initialization
private val _state = MutableStateFlow(
    PermissionState(
        hasRequestedPermissionsBefore = prefsManager.hasRequestedPermissionsBefore()
    )
)
```

**After:**
```kotlin
// Reactive Flow-based initialization
init {
    viewModelScope.launch {
        prefsManager.hasRequestedPermissionsBefore.collect { hasRequested ->
            _state.update { it.copy(hasRequestedPermissionsBefore = hasRequested) }
            if (hasRequested) {
                checkAndRestorePermissionStates()
            }
        }
    }
}

private fun markPermissionsRequested() {
    viewModelScope.launch {
        prefsManager.setHasRequestedPermissions(true)
    }
}
```

### 3. Theme Preferences Unchanged

Theme preferences already used DataStore, so the API remains the same:
```kotlin
val isDarkMode: Flow<Boolean?>
suspend fun setDarkMode(isDark: Boolean?)
suspend fun toggleTheme(currentIsDark: Boolean)
```

## Benefits

### 🚀 Performance
- **Single DataStore instance** - Reduced memory footprint
- **Atomic operations** - Built-in transaction support
- **Efficient I/O** - DataStore batches writes automatically

### 🔒 Type Safety
```kotlin
companion object {
    // All keys in one place - compile-time safety
    private val KEY_IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
    private val KEY_HAS_REQUESTED_PERMISSIONS = booleanPreferencesKey("has_requested_permissions_before")
}
```

### 🔄 Reactive Updates
```kotlin
// Both preferences are reactive Flows
val isDarkMode: Flow<Boolean?>
val hasRequestedPermissionsBefore: Flow<Boolean>

// UI automatically updates when preferences change
```

### 🧪 Testing
```kotlin
// Easy to mock single manager
@Test
fun testPermissionFlow() {
    val mockPrefs = mockk<AppPreferencesManager>()
    every { mockPrefs.hasRequestedPermissionsBefore } returns flowOf(true)
    // Test with mock
}
```

### 📦 Maintainability
```kotlin
// Easy to add new preferences
companion object {
    private val KEY_IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
    private val KEY_HAS_REQUESTED_PERMISSIONS = booleanPreferencesKey("has_requested_permissions_before")
    
    // Just add here 👇
    private val KEY_USER_NAME = stringPreferencesKey("user_name")
    private val KEY_NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
}
```

## Migration Path

### For Future Preferences

Adding new preferences is simple:

```kotlin
// 1. Add key
companion object {
    private val KEY_NEW_PREF = stringPreferencesKey("new_pref")
}

// 2. Add Flow property
val newPref: Flow<String> = context.dataStore.data
    .map { preferences -> preferences[KEY_NEW_PREF] ?: "default" }

// 3. Add setter
suspend fun setNewPref(value: String) {
    context.dataStore.edit { preferences ->
        preferences[KEY_NEW_PREF] = value
    }
}
```

## DataStore vs SharedPreferences

| Feature | SharedPreferences | DataStore |
|---------|------------------|-----------|
| **Threading** | Synchronous (UI blocking) | Asynchronous (suspend) |
| **Type Safety** | Runtime (String keys) | Compile-time (Keys) |
| **Reactive** | ❌ No | ✅ Flow |
| **Errors** | Runtime exceptions | Flow catches |
| **Transactions** | Manual | Automatic |
| **Modern** | ❌ Legacy | ✅ Recommended |

## Architecture

```
┌─────────────────────────────────────────┐
│        AppPreferencesManager            │
│  (Single DataStore instance)            │
│                                         │
│  ┌─────────────────────────────────┐   │
│  │  Theme Preferences              │   │
│  │  - isDarkMode: Flow<Boolean?>   │   │
│  │  - setDarkMode()                │   │
│  │  - toggleTheme()                │   │
│  └─────────────────────────────────┘   │
│                                         │
│  ┌─────────────────────────────────┐   │
│  │  Permission Preferences         │   │
│  │  - hasRequested: Flow<Boolean>  │   │
│  │  - setHasRequested()            │   │
│  └─────────────────────────────────┘   │
│                                         │
│  ┌─────────────────────────────────┐   │
│  │  Future Preferences             │   │
│  │  - Easy to add                  │   │
│  └─────────────────────────────────┘   │
└─────────────────────────────────────────┘
         ↓                    ↓
   ThemeViewModel    PermissionViewModel
```

## Testing

### Unit Test Example
```kotlin
@Test
fun `theme toggle updates preference`() = runTest {
    val context = mockContext()
    val prefsManager = AppPreferencesManager(context)
    
    // Toggle theme
    prefsManager.toggleTheme(currentIsDark = false)
    
    // Verify
    prefsManager.isDarkMode.first() shouldBe true
}

@Test
fun `permission flag persists`() = runTest {
    val prefsManager = AppPreferencesManager(context)
    
    prefsManager.setHasRequestedPermissions(true)
    
    prefsManager.hasRequestedPermissionsBefore.first() shouldBe true
}
```

## Cleanup Steps

### Files to Delete
1. Delete `PreferencesManager.kt` (old SharedPreferences)
2. Delete `ThemePreferencesManager.kt` (separate DataStore)

### Verify
- ✅ No compilation errors
- ✅ Theme toggle works
- ✅ Permission state persists
- ✅ App restarts preserve state

## Summary

### What Changed
- ✅ **Unified Manager** - Single `AppPreferencesManager` class
- ✅ **Full DataStore** - All preferences use DataStore
- ✅ **Reactive API** - Flow-based for all preferences
- ✅ **Type Safe** - Compile-time key safety
- ✅ **Better Performance** - Single DataStore instance

### Benefits
- ✅ **Consistent** - Same API pattern
- ✅ **Maintainable** - Easy to add preferences
- ✅ **Testable** - Single mock point
- ✅ **Modern** - Latest Android best practices
- ✅ **Reactive** - Automatic UI updates

### Result
**Single unified DataStore manager** that handles all app preferences with a consistent, type-safe, reactive API! 🎉

---

**Recommendation**: ✅ **Always use a single unified preferences manager for new projects**
