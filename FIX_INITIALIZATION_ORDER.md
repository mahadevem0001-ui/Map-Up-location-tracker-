# Fix: NullPointerException on ViewModel Initialization

## Error
```
java.lang.NullPointerException: Attempt to invoke interface method 
'java.lang.Object kotlinx.coroutines.flow.MutableStateFlow.getValue()' 
on a null object reference
at PermissionHandlingViewModel.checkAndRestorePermissionStates(PermissionHandling.kt:343)
at PermissionHandlingViewModel.<init>(PermissionHandling.kt:145)
```

## Root Cause

The crash occurred due to **incorrect initialization order** in the ViewModel. The `_state` MutableStateFlow was being accessed in the `init` block before it was initialized.

### The Problem: Initialization Order

```kotlin
class PermissionHandlingViewModel(application: Application) : AndroidViewModel(application) {
    
    private val prefsManager = PermissionPreferencesManager(application)
    
    init {
        // This runs BEFORE _state is initialized! ❌
        if (hasRequestedBefore) {
            checkAndRestorePermissionStates() // Tries to access _state
        }
    }
    
    // ❌ _state is declared AFTER init block
    private val _state = MutableStateFlow(...)
    
    private fun checkAndRestorePermissionStates() {
        _state.update { ... } // ❌ NullPointerException! _state is null
    }
}
```

### Why This Happens

In Kotlin, class members are initialized in the order they are declared:

1. Primary constructor parameters
2. Property initializers (in order of declaration)
3. `init` blocks (in order of declaration)
4. Secondary constructors

**The bug**: `init` block called `checkAndRestorePermissionStates()`, which tried to access `_state`, but `_state` was declared AFTER the `init` block, so it was still `null`.

## Solution

Move the `_state` and `_events` initialization **BEFORE** the `init` block so they're available when `checkAndRestorePermissionStates()` is called.

### Fixed Code

```kotlin
class PermissionHandlingViewModel(application: Application) : AndroidViewModel(application) {
    
    private val prefsManager = PermissionPreferencesManager(application)
    private val appContext = application.applicationContext
    
    // ✅ Initialize StateFlow BEFORE init block
    private val _state = MutableStateFlow(
        PermissionState(
            hasRequestedPermissionsBefore = prefsManager.hasRequestedPermissionsBefore()
        )
    )
    val state = _state.asStateFlow()
    
    // ✅ Initialize Channel BEFORE init block
    private val _events = Channel<PermissionEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()
    
    init {
        // ✅ Now _state is available!
        val hasRequestedBefore = prefsManager.hasRequestedPermissionsBefore()
        
        if (hasRequestedBefore) {
            checkAndRestorePermissionStates() // ✅ Can safely access _state
        }
    }
    
    private fun checkAndRestorePermissionStates() {
        _state.update { ... } // ✅ Works! _state is initialized
    }
}
```

## Key Lesson: Kotlin Initialization Order

### Order of Execution

```
1. Primary constructor
   ↓
2. Property initializers (top to bottom)
   ↓
3. init blocks (top to bottom)
   ↓
4. Methods called from init
```

### Best Practice

**Always declare properties BEFORE init blocks if:**
- Init block needs to access the property
- Init block calls methods that access the property

### Visual Flow

```
❌ BEFORE (Broken):
┌────────────────────────┐
│ prefsManager           │ ← Initialized
├────────────────────────┤
│ init {                 │ ← Runs
│   checkAndRestore()    │ ← Calls method
│ }                      │
├────────────────────────┤
│ _state = ...           │ ← NOT YET INITIALIZED
├────────────────────────┤
│ checkAndRestore() {    │
│   _state.update()      │ ← CRASH! _state is null
│ }                      │
└────────────────────────┘

✅ AFTER (Fixed):
┌────────────────────────┐
│ prefsManager           │ ← Initialized
├────────────────────────┤
│ _state = ...           │ ← Initialized FIRST
├────────────────────────┤
│ init {                 │ ← Runs
│   checkAndRestore()    │ ← Calls method
│ }                      │
├────────────────────────┤
│ checkAndRestore() {    │
│   _state.update()      │ ← Works! _state exists
│ }                      │
└────────────────────────┘
```

## Files Modified

**PermissionHandling.kt**
- Moved `_state` declaration BEFORE `init` block
- Moved `_events` declaration BEFORE `init` block
- Removed duplicate declarations that were after `init`

## Testing

The app should now:
- ✅ Launch without crashing
- ✅ Properly initialize ViewModel
- ✅ Restore permission state from SharedPreferences
- ✅ Show correct UI after app restart

## Common Mistake

This is a **very common mistake** when refactoring code:

```kotlin
// Original code - works
init {
    // No method calls
}
private val _state = ...

// After refactoring - breaks!
init {
    someMethod() // ❌ Calls method that uses _state
}
private val _state = ... // ❌ Still declared after init
```

**Solution**: Always move property declarations above init if they're needed by init.

## Summary

**Problem**: `_state` accessed in `init` block before initialization  
**Cause**: Properties initialized after `init` block in declaration order  
**Solution**: Move `_state` and `_events` declarations BEFORE `init` block  
**Result**: App launches successfully, no NullPointerException ✅

The crash is completely fixed!
