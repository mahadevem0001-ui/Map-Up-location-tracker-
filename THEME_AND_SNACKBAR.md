# Theme Support & Snackbar Implementation

## ✅ Implementation Complete!

### What Was Added

#### 1. **Light & Dark Theme Support**
- ✅ Full Material 3 theme support
- ✅ Dynamic colors for Android 12+
- ✅ Proper color schemes for both themes
- ✅ Theme persistence using DataStore

#### 2. **Theme Toggle Feature**
- ✅ Floating Action Button (FAB) for theme switching
- ✅ Moon icon in light mode, sun icon in dark mode
- ✅ Smooth transition between themes
- ✅ Theme preference persists across app restarts

#### 3. **Snackbar Feedback**
- ✅ Shows confirmation message when theme changes
- ✅ "Switched to Light Mode" / "Switched to Dark Mode"
- ✅ Auto-dismisses after a short duration

#### 4. **Theme-Aware Components**
- ✅ Permission cards adapt to theme
- ✅ Denied permission items use theme colors
- ✅ Proper text contrast in both themes

## Architecture

### New Files Created

```
core/
├── data/local/
│   └── ThemePreferencesManager.kt     # DataStore for theme persistence
├── presentation/
│   ├── viewmodel/
│   │   └── ThemeViewModel.kt          # Manages theme state
│   └── components/
│       └── ThemeToggleFAB.kt          # Theme toggle button
```

### Updated Files

1. **MainActivity.kt**
   - Observes theme state from ViewModel
   - Passes dark mode flag to theme composable

2. **PermissionScreen.kt**
   - Added theme toggle FAB
   - Added Snackbar support
   - Shows theme change feedback

3. **DeniedPermissionItem.kt**
   - Uses theme-aware colors instead of hardcoded colors
   - Different colors for light/dark themes

4. **Color.kt**
   - Added permission status colors for both themes

5. **build.gradle.kts**
   - Added DataStore dependency

## How It Works

### Theme State Management

```kotlin
// 1. User clicks theme toggle FAB
ThemeToggleFAB(
    isDarkMode = isDarkMode,
    onToggle = {
        // 2. Toggle theme in ViewModel
        themeViewModel.toggleTheme(isDarkMode)
        
        // 3. Show Snackbar feedback
        scope.launch {
            snackbarHostState.showSnackbar(
                message = if (isDarkMode) "Switched to Light Mode" else "Switched to Dark Mode"
            )
        }
    }
)

// 4. ThemeViewModel persists preference to DataStore
suspend fun toggleTheme(currentIsDark: Boolean) {
    themePrefsManager.setDarkMode(!currentIsDark)
}

// 5. MainActivity observes theme state
val themeMode by themeViewModel.isDarkMode.collectAsStateWithLifecycle()
val darkTheme = themeMode ?: systemInDarkTheme

// 6. Theme is applied
MapUpAndroidDeveloperAssessmentTheme(darkTheme = darkTheme) {
    PermissionScreen()
}
```

### Data Persistence

```kotlin
// DataStore saves theme preference
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

// Read theme preference (Flow)
val isDarkMode: Flow<Boolean?> = context.dataStore.data
    .map { preferences -> preferences[IS_DARK_MODE] }

// Write theme preference
suspend fun setDarkMode(isDark: Boolean?) {
    context.dataStore.edit { preferences ->
        if (isDark == null) {
            preferences.remove(IS_DARK_MODE)  // Use system theme
        } else {
            preferences[IS_DARK_MODE] = isDark
        }
    }
}
```

## Theme Colors

### Permission Status Colors

**Light Theme:**
- ✅ Granted: Light Green (#E8F5E9)
- ⚠️ Denied: Light Orange (#FFE0B2)
- ❌ Permanent: Light Red (#FFCDD2)

**Dark Theme:**
- ✅ Granted: Dark Green (#2E7D32)
- ⚠️ Denied: Dark Orange (#EF6C00)
- ❌ Permanent: Dark Red (#C62828)

### Material 3 Color Schemes

**Light Mode:**
```kotlin
lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)
```

**Dark Mode:**
```kotlin
darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)
```

## User Experience

### First Launch
```
1. App opens with system theme (light/dark based on device)
2. User sees FAB with moon/sun icon in bottom-right
3. Permission cards use appropriate theme colors
```

### Toggle Theme
```
1. User taps FAB
2. Theme immediately switches
3. Snackbar appears: "Switched to [Light/Dark] Mode"
4. FAB icon changes (moon ↔ sun)
5. All colors adapt instantly
6. Preference saved to DataStore
```

### App Restart
```
1. Kill and relaunch app
2. Previously selected theme is restored
3. No theme flicker or reset
4. Smooth, persistent experience
```

## Features

### ✅ Theme Toggle FAB
- **Location**: Bottom-right of screen
- **Icon**: Moon (light mode) / Sun (dark mode)
- **Color**: Uses primary container color
- **Feedback**: Snackbar message on toggle

### ✅ Snackbar
- **Message**: "Switched to Light/Dark Mode"
- **Duration**: Short (2 seconds)
- **Position**: Bottom of screen
- **Style**: Material 3 default

### ✅ Theme Persistence
- **Storage**: DataStore Preferences
- **File**: "app_settings"
- **Key**: "is_dark_mode"
- **Values**: true (dark) / false (light) / null (system)

### ✅ Theme-Aware Components
- Permission info cards
- Denied permission items
- Dialog backgrounds
- Text colors
- Button colors
- FAB appearance

## Benefits

### 1. User Preference ✅
- Users can choose their preferred theme
- Not forced to use system theme
- Choice persists across sessions

### 2. Better UX ✅
- Immediate visual feedback (Snackbar)
- Smooth transitions
- Consistent Material 3 design
- Proper contrast in all modes

### 3. Accessibility ✅
- Dark mode reduces eye strain
- Light mode better in bright environments
- Clear visual distinctions
- Readable text in both themes

### 4. Modern Design ✅
- Material 3 guidelines
- Dynamic colors (Android 12+)
- Professional appearance
- Industry standard

## Testing Checklist

### Theme Toggle
- [ ] FAB appears in bottom-right
- [ ] Icon changes on toggle (moon ↔ sun)
- [ ] Snackbar shows correct message
- [ ] Theme changes immediately
- [ ] All colors adapt properly

### Persistence
- [ ] Theme saved on toggle
- [ ] Theme restored on app restart
- [ ] No flicker on launch
- [ ] Survives app kill

### Visual Consistency
- [ ] Permission cards readable in both themes
- [ ] Denied items have proper contrast
- [ ] Dialog text is clear
- [ ] FAB colors appropriate
- [ ] No hardcoded black/white text

### Edge Cases
- [ ] Works with system theme changes
- [ ] Handles first launch correctly
- [ ] DataStore read/write errors handled
- [ ] Configuration changes (rotation) work

## Code Quality

### Clean Architecture ✅
- Theme logic in core layer
- ViewModel for state management
- DataStore for persistence
- UI observes state reactively

### Separation of Concerns ✅
- ThemePreferencesManager: Data access
- ThemeViewModel: Business logic
- MainActivity: Theme application
- PermissionScreen: Theme UI interaction

### Best Practices ✅
- StateFlow for reactive state
- Coroutines for async operations
- Lifecycle-aware collection
- Proper resource management

## Summary

The app now includes:
- ✅ **Full Light/Dark Theme Support**
- ✅ **Theme Toggle FAB**
- ✅ **Snackbar Feedback**
- ✅ **Theme Persistence** (DataStore)
- ✅ **Theme-Aware Colors**
- ✅ **Material 3 Design**
- ✅ **Clean Architecture**

Users can toggle between light and dark modes with a single tap, receive immediate visual feedback via Snackbar, and their preference is saved permanently! 🎉
