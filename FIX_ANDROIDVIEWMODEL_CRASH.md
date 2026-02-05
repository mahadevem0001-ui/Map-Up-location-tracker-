# Fix: AndroidViewModel Instantiation Crash

## Error
```
java.lang.RuntimeException: Cannot create an instance of class 
com.mahi.kr.mapup_androiddeveloperassessment.PermissionHandlingViewModel
```

## Root Cause

The crash occurred because `AndroidViewModel` requires an `Application` parameter in its constructor, but the default `viewModel()` function in Compose doesn't know how to provide it.

```kotlin
// This causes crash ❌
@Composable
fun PermissionHandlingScreen(
    viewModel: PermissionHandlingViewModel = viewModel()  // Can't create AndroidViewModel!
)

class PermissionHandlingViewModel(application: Application) : AndroidViewModel(application)
```

## Solution

Created a `ViewModelProvider.Factory` to properly instantiate the `AndroidViewModel` with the required `Application` parameter.

### 1. Added Factory to ViewModel

```kotlin
class PermissionHandlingViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        // ... existing code ...

        /**
         * Factory for creating PermissionHandlingViewModel instances
         * Required because AndroidViewModel needs Application parameter
         */
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                // Get the Application object from CreationExtras
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) {
                    "Application not available in CreationExtras"
                }
                return PermissionHandlingViewModel(application) as T
            }
        }
    }
}
```

### 2. Updated Composable to Use Factory

```kotlin
@Composable
fun PermissionHandlingScreen(
    viewModel: PermissionHandlingViewModel = viewModel(factory = PermissionHandlingViewModel.Factory)  // ✅ Works!
) {
    // ... rest of the code
}
```

### 3. Added Required Import

```kotlin
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
```

## How It Works

1. **CreationExtras**: Compose provides `CreationExtras` which contains the `Application` instance
2. **Factory.create()**: Extracts `Application` from `CreationExtras`
3. **ViewModel Creation**: Uses `Application` to create `PermissionHandlingViewModel`

## Files Modified

1. **PermissionHandling.kt**
   - Added `ViewModelProvider` import
   - Added `CreationExtras` import
   - Added `Factory` companion object

2. **MainActivity.kt**
   - Updated `viewModel()` call to use `factory = PermissionHandlingViewModel.Factory`

## Why AndroidViewModel Needs This

- `ViewModel`: No-arg constructor → Works with default factory
- `AndroidViewModel`: Requires `Application` → Needs custom factory

## Testing

The app should now:
- ✅ Launch without crashing
- ✅ Properly instantiate `PermissionHandlingViewModel`
- ✅ Load persisted state from SharedPreferences
- ✅ Work correctly after app restart

## Alternative Approaches (Not Used)

### 1. ViewModelProvider in Activity
```kotlin
// Could do this in Activity
val viewModel: PermissionHandlingViewModel by viewModels()
```

### 2. Hilt/Koin DI
```kotlin
// With DI framework
@HiltViewModel
class PermissionHandlingViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application)
```

We chose the Factory approach because:
- ✅ No additional dependencies needed
- ✅ Compose-friendly
- ✅ Standard Android approach
- ✅ Easy to understand

## Summary

**Problem**: `viewModel()` can't create `AndroidViewModel` automatically  
**Solution**: Provide custom `Factory` that supplies `Application` parameter  
**Result**: App launches successfully, ViewModel properly instantiated ✅

The crash is completely fixed!
