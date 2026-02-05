# Refactoring: Removing Redundant Code in ViewModel Init

## The Question
> "Now do we need hasRequestedPermissionsBefore, checkAndRestorePermissionStates in the init block still? Anyway we are using it in the _state initialization."

## Analysis

### Before Refactoring ❌
```kotlin
private val _state = MutableStateFlow(
    PermissionState(
        hasRequestedPermissionsBefore = prefsManager.hasRequestedPermissionsBefore() // ← Loaded here
    )
)

init {
    val hasRequestedBefore = prefsManager.hasRequestedPermissionsBefore() // ← ❌ REDUNDANT! Loaded again
    Log.d(TAG, "Loaded persisted state - hasRequestedPermissionsBefore: $hasRequestedBefore")
    
    if (hasRequestedBefore) {
        checkAndRestorePermissionStates()
    }
}
```

**Problems:**
1. ❌ **Redundant SharedPreferences read**: Calling `prefsManager.hasRequestedPermissionsBefore()` twice
2. ❌ **Unnecessary variable**: `hasRequestedBefore` duplicates what's already in `_state.value`
3. ❌ **Code duplication**: Same value loaded in two places

### After Refactoring ✅
```kotlin
private val _state = MutableStateFlow(
    PermissionState(
        hasRequestedPermissionsBefore = prefsManager.hasRequestedPermissionsBefore() // ← Loaded once
    )
)

init {
    Log.d(TAG, "Loaded persisted state - hasRequestedPermissionsBefore: ${_state.value.hasRequestedPermissionsBefore}") // ← Use value from state
    
    if (_state.value.hasRequestedPermissionsBefore) { // ← ✅ Use state directly
        checkAndRestorePermissionStates()
    }
}
```

**Benefits:**
1. ✅ **Single source of truth**: Only read from SharedPreferences once
2. ✅ **No redundancy**: Use `_state.value` directly in init block
3. ✅ **Cleaner code**: Remove unnecessary variable
4. ✅ **Better performance**: One less SharedPreferences read

## What We Still Need

### ✅ checkAndRestorePermissionStates() - STILL NEEDED

**Why?**
- The `_state` initialization only loads the **flag** (`hasRequestedPermissionsBefore`)
- It does **NOT** restore the actual **denied permissions list**
- We need `checkAndRestorePermissionStates()` to:
  1. Check current permission states using `appContext.checkSelfPermission()`
  2. Rebuild the `deniedPermissions` map
  3. Populate the state with denied permissions

**Example:**
```kotlin
// State initialization - only loads the flag
private val _state = MutableStateFlow(
    PermissionState(
        hasRequestedPermissionsBefore = true,  // ✅ Loaded
        deniedPermissions = emptyMap()          // ❌ Empty! Need to restore
    )
)

init {
    if (_state.value.hasRequestedPermissionsBefore) {
        checkAndRestorePermissionStates()  // ✅ Rebuilds deniedPermissions map
    }
}
```

## Complete Flow After Refactoring

### App Killed and Relaunched
```
1. ViewModel Created
   ↓
2. _state initialized
   - Read SharedPreferences once
   - hasRequestedPermissionsBefore = true ✅
   - deniedPermissions = emptyMap() (not restored yet)
   ↓
3. init block runs
   - Check: _state.value.hasRequestedPermissionsBefore == true
   - Call: checkAndRestorePermissionStates()
   ↓
4. checkAndRestorePermissionStates()
   - Check each permission: appContext.checkSelfPermission()
   - Add denied permissions to map
   - Update _state with denied permissions
   ↓
5. UI observes state
   - Shows denied permissions list ✅
   - Shows correct UI after restart ✅
```

## Code Comparison

### Variable Usage
```kotlin
// Before ❌
val hasRequestedBefore = prefsManager.hasRequestedPermissionsBefore()
if (hasRequestedBefore) { ... }

// After ✅
if (_state.value.hasRequestedPermissionsBefore) { ... }
```

### SharedPreferences Reads
```kotlin
// Before ❌
// Read 1: In _state initialization
hasRequestedPermissionsBefore = prefsManager.hasRequestedPermissionsBefore()

// Read 2: In init block (REDUNDANT!)
val hasRequestedBefore = prefsManager.hasRequestedPermissionsBefore()

// After ✅
// Read 1: In _state initialization (ONLY ONCE)
hasRequestedPermissionsBefore = prefsManager.hasRequestedPermissionsBefore()

// No second read - use _state.value instead
```

## Summary

### What Changed
- ❌ Removed: Redundant `hasRequestedBefore` variable in init
- ❌ Removed: Duplicate SharedPreferences read
- ✅ Keep: `checkAndRestorePermissionStates()` call (still needed!)
- ✅ Use: `_state.value.hasRequestedPermissionsBefore` directly

### Why This is Better
1. **Single Read**: SharedPreferences accessed only once
2. **Single Source of Truth**: State is the only place we check the flag
3. **Cleaner Code**: No redundant variables
4. **Better Performance**: Fewer I/O operations
5. **Maintainability**: Less code duplication

### What Still Happens
- ✅ Flag loaded from SharedPreferences on ViewModel creation
- ✅ Denied permissions list restored if flag is true
- ✅ Correct UI shown after app restart
- ✅ All functionality preserved

## Key Takeaway

**The flag and the denied list are different:**
- **Flag** (`hasRequestedPermissionsBefore`): Loaded once in `_state` init
- **Denied list** (`deniedPermissions`): Must be rebuilt in `checkAndRestorePermissionStates()`

Both are needed, but we don't need to read the flag twice!

**Great catch!** This refactoring makes the code cleaner and more efficient. 🎯
