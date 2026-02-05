# Clean Architecture - Project Restructuring

## Overview
The project has been restructured to follow **Clean Architecture** and **MVVM** principles, making it scalable, maintainable, and ready for new feature additions.

## New Package Structure

```
app/src/main/java/com/mahi/kr/mapup_androiddeveloperassessment/
│
├── core/                                    # Core/Shared layer (used across features)
│   ├── data/
│   │   └── local/
│   │       └── PreferencesManager.kt        # SharedPreferences management
│   └── util/
│       ├── compose/
│       │   └── ObserveAsEvents.kt          # Compose utility for event observation
│       └── extensions/
│           └── ActivityExtensions.kt       # Activity extension functions
│
├── feature/                                 # Feature modules (organized by feature)
│   └── permission/                          # Permission management feature
│       ├── data/                           # Data layer (future: repositories impl)
│       │   └── repository/
│       │
│       ├── domain/                         # Domain layer (business logic, models)
│       │   ├── model/
│       │   │   ├── DeniedPermissionInfo.kt # Domain model for denied permission
│       │   │   └── PermissionState.kt      # Domain model for permission state
│       │   ├── repository/                 # Repository interfaces (future)
│       │   └── usecase/                    # Use cases (future)
│       │
│       └── presentation/                   # Presentation layer (UI, ViewModels)
│           ├── components/                 # Reusable UI components (future split)
│           │   ├── DeniedPermissionItem.kt
│           │   ├── LocationPermissionInfoCard.kt
│           │   ├── PermissionInfoCard.kt
│           │   └── ProminentDeniedPermissionsDialog.kt
│           ├── model/                      # Presentation models
│           │   ├── PermissionAction.kt     # User actions (sealed class)
│           │   ├── PermissionEvent.kt      # One-time events (sealed class)
│           │   └── PermissionTextProvider.kt # Text provider interface & impl
│           ├── screen/                     # Screen composables
│           │   └── PermissionScreen.kt     # Main permission screen
│           └── viewmodel/
│               └── PermissionViewModel.kt  # ViewModel for permission management
│
└── ui/
    └── theme/                              # App theme (colors, typography, shapes)
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```

## Architecture Layers

### 1. Core Layer
**Purpose**: Shared utilities and infrastructure used across all features

**Components**:
- `PreferencesManager`: Centralized SharedPreferences management
- `ObserveAsEvents`: Compose utility for lifecycle-aware event observation
- `ActivityExtensions`: Extension functions for Android components

**Key Principles**:
- No feature-specific logic
- Reusable across all features
- Infrastructure and utility code only

### 2. Feature Layer (Permission Feature)
Each feature is self-contained with its own layers:

#### a) Domain Layer
**Purpose**: Business logic and models, framework-independent

**Components**:
- `DeniedPermissionInfo`: Domain model representing a denied permission
- `PermissionState`: Domain model representing the complete permission state
- **Future**: Repository interfaces, Use cases

**Key Principles**:
- No Android framework dependencies (pure Kotlin)
- Business rules and domain models
- Interfaces for data layer

#### b) Data Layer (Future)
**Purpose**: Data access and repository implementations

**Future Components**:
- `PermissionRepositoryImpl`: Implementation of permission repository
- Data sources (if needed)

#### c) Presentation Layer
**Purpose**: UI and ViewModel logic

**Components**:
- **ViewModel**: `PermissionViewModel` - manages UI state and business logic
- **Models**: Actions, Events, TextProviders for UI layer
- **Components**: Reusable UI components (cards, dialogs, items)
- **Screens**: Full screen composables

**Key Principles**:
- ViewModels depend on domain layer
- UI observes ViewModel state
- One-way data flow (UDF)

## Benefits of This Structure

### 1. Separation of Concerns ✅
- Each layer has a single responsibility
- Clear boundaries between layers
- Easy to understand and navigate

### 2. Scalability ✅
```
Adding a new feature (e.g., "profile"):

feature/
└── profile/
    ├── data/
    ├── domain/
    └── presentation/
```

### 3. Testability ✅
- Domain layer: Pure Kotlin, easy to unit test
- ViewModel: Can be tested without UI
- UI components: Can be tested in isolation

### 4. Maintainability ✅
- Changes in one layer don't affect others
- Easy to locate specific functionality
- Clear dependencies

### 5. Reusability ✅
- Core utilities shared across features
- Domain models can be reused
- UI components are modular

## Data Flow

```
┌─────────────────────────────────────────────────────────┐
│                        UI Layer                         │
│                   (PermissionScreen)                    │
└─────────────────────────────────────────────────────────┘
                            ↓ ↑
                    User Actions │ State Updates
                            ↓ ↑
┌─────────────────────────────────────────────────────────┐
│                   Presentation Layer                    │
│                  (PermissionViewModel)                  │
└─────────────────────────────────────────────────────────┘
                            ↓ ↑
                    Domain Logic │ Domain Models
                            ↓ ↑
┌─────────────────────────────────────────────────────────┐
│                     Domain Layer                        │
│         (PermissionState, DeniedPermissionInfo)         │
└─────────────────────────────────────────────────────────┘
                            ↓ ↑
                    Repository Interface (Future)
                            ↓ ↑
┌─────────────────────────────────────────────────────────┐
│                      Data Layer                         │
│              (PreferencesManager - Core)                │
└─────────────────────────────────────────────────────────┘
```

## Migration Guide

### Old Structure → New Structure

| Old Location | New Location | Reason |
|--------------|--------------|--------|
| `PermissionHandling.kt` (root) | `feature/permission/presentation/viewmodel/PermissionViewModel.kt` | Better organization, MVVM pattern |
| `PermissionTextProvider.kt` (root) | `feature/permission/presentation/model/PermissionTextProvider.kt` | Presentation-specific model |
| `util/LocalPreferencesManager.kt` | `core/data/local/PreferencesManager.kt` | Core data infrastructure |
| `util/openAppSettings.kt` | `core/util/extensions/ActivityExtensions.kt` | Core utility extension |
| `util/ObserveAsEvents.kt` | `core/util/compose/ObserveAsEvents.kt` | Core Compose utility |
| Domain models (in PermissionHandling.kt) | `feature/permission/domain/model/` | Clean Architecture separation |
| Actions/Events (in PermissionHandling.kt) | `feature/permission/presentation/model/` | Presentation layer models |

### Import Changes Required

```kotlin
// Old imports
import com.mahi.kr.mapup_androiddeveloperassessment.PermissionHandlingViewModel
import com.mahi.kr.mapup_androiddeveloperassessment.util.openAppSettings

// New imports
import com.mahi.kr.mapup_androiddeveloperassessment.feature.permission.presentation.viewmodel.PermissionViewModel
import com.mahi.kr.mapup_androiddeveloperassessment.core.util.extensions.openAppSettings
```

## Next Steps for MainActivity Refactoring

The MainActivity needs to be updated to:
1. Import from new package locations
2. Use `PermissionViewModel` instead of `PermissionHandlingViewModel`
3. Update all component imports

## Adding New Features

### Example: Adding a "Map" Feature

1. **Create feature structure**:
```
feature/
└── map/
    ├── data/
    │   ├── repository/
    │   │   └── MapRepositoryImpl.kt
    │   └── source/
    │       └── MapDataSource.kt
    ├── domain/
    │   ├── model/
    │   │   └── MapLocation.kt
    │   ├── repository/
    │   │   └── MapRepository.kt
    │   └── usecase/
    │       └── GetNearbyPlacesUseCase.kt
    └── presentation/
        ├── components/
        │   └── MapView.kt
        ├── model/
        │   ├── MapAction.kt
        │   └── MapEvent.kt
        ├── screen/
        │   └── MapScreen.kt
        └── viewmodel/
            └── MapViewModel.kt
```

2. **Follow same patterns**:
- Domain models in `domain/model/`
- Business logic in ViewModels
- UI components in `presentation/components/`
- Reuse core utilities

## Best Practices

### 1. Dependency Rules
- **Domain** depends on: Nothing (pure Kotlin)
- **Data** depends on: Domain
- **Presentation** depends on: Domain
- **Core** depends on: Nothing (shared utilities)

### 2. Naming Conventions
- **Domain models**: Plain names (`PermissionState`, `User`)
- **Presentation models**: Suffixed (`PermissionAction`, `MapEvent`)
- **ViewModels**: Feature + `ViewModel` (`PermissionViewModel`)
- **Screens**: Feature + `Screen` (`PermissionScreen`)

### 3. File Organization
- One class/interface per file
- File name matches class name
- Group related classes in same package

## Summary

This restructuring provides:
- ✅ **Clear Architecture**: MVVM + Clean Architecture
- ✅ **Scalability**: Easy to add new features
- ✅ **Maintainability**: Clear separation of concerns
- ✅ **Testability**: Layers can be tested independently
- ✅ **Industry Standard**: Follows Android best practices

The project is now ready for feature additions with a solid architectural foundation!
