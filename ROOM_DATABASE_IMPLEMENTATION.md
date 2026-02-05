# Room Database Implementation for Location Tracking

## Overview
Complete Room database implementation to persist location sessions with all related data. Replaces in-memory caching with persistent storage using SQLite.

## Database Schema

### Tables

#### 1. **location_sessions**
```sql
CREATE TABLE location_sessions (
    sessionId INTEGER PRIMARY KEY NOT NULL,
    startTime INTEGER NOT NULL,
    endTime INTEGER,
    locationCount INTEGER NOT NULL DEFAULT 0
)
```

#### 2. **locations**
```sql
CREATE TABLE locations (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    sessionId INTEGER NOT NULL,
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    timestamp INTEGER NOT NULL,
    accuracy REAL,
    altitude REAL,
    speed REAL,
    FOREIGN KEY(sessionId) REFERENCES location_sessions(sessionId) ON DELETE CASCADE
)
CREATE INDEX index_locations_sessionId ON locations(sessionId)
```

## Architecture

### Entities
- **LocationSessionEntity** - Represents a tracking session
- **LocationEntity** - Represents individual location points

### DTOs
- **SessionWithLocations** - Combines session with all its locations using Room's @Relation

### DAOs
- **LocationSessionDao** - CRUD operations for sessions
- **LocationDao** - CRUD operations for locations

### Repository
- **LocationSessionRepository** (interface) - Contract for data operations
- **LocationSessionRepositoryImpl** - Implementation using Room DAOs

### Mappers
- **LocationMappers.kt** - Bidirectional mapping between entities and domain models

## Key Features

### ✅ Relational Data
- One-to-Many relationship (Session → Locations)
- CASCADE delete: Deleting a session automatically deletes all its locations
- Foreign key constraints ensure data integrity

### ✅ Type-Safe Error Handling
- All repository methods return `Result<T, DataError>`
- Consistent error handling across data layer
- Maps exceptions to domain errors

### ✅ Reactive Data Access
- Flow-based API for real-time updates
- `getAllSessions()` returns `Flow<Result<List<LocationSession>, DataError>>`
- UI automatically updates when database changes

### ✅ Efficient Queries
- Indexed foreign keys for fast lookups
- Transaction support for atomic operations
- Optimized queries with proper ordering

## Repository API

```kotlin
interface LocationSessionRepository {
    // Create/Update
    suspend fun createSession(session: LocationSession): EmptyResult<DataError>
    suspend fun updateSession(session: LocationSession): EmptyResult<DataError>
    suspend fun addLocationToSession(sessionId: Long, location: LocationData): EmptyResult<DataError>
    
    // Read
    suspend fun getSessionById(sessionId: Long): Result<LocationSession?, DataError>
    fun getAllSessions(): Flow<Result<List<LocationSession>, DataError>>
    suspend fun getRecentSessions(limit: Int): Result<List<LocationSession>, DataError>
    suspend fun getActiveSession(): Result<LocationSession?, DataError>
    
    // Delete
    suspend fun deleteSession(sessionId: Long): EmptyResult<DataError>
    suspend fun deleteAllSessions(): EmptyResult<DataError>
}
```

## Usage Examples

### Creating a Session
```kotlin
val session = LocationSession(
    sessionId = System.currentTimeMillis(),
    startTime = System.currentTimeMillis(),
    endTime = null,
    locations = emptyList()
)

repository.createSession(session)
    .onSuccess { /* Session created */ }
    .onError { error -> showError(error.toUiText()) }
```

### Adding Location to Session
```kotlin
val location = LocationData(
    latitude = 12.345,
    longitude = 67.890,
    timestamp = System.currentTimeMillis(),
    accuracy = 10f,
    altitude = null,
    speed = null
)

repository.addLocationToSession(sessionId, location)
    .onSuccess { /* Location added */ }
    .onError { error -> showError(error.toUiText()) }
```

### Observing Sessions (Reactive)
```kotlin
viewModelScope.launch {
    repository.getAllSessions()
        .collect { result ->
            when (result) {
                is Result.Success -> updateUI(result.data)
                is Result.Error -> showError(result.error.toUiText())
            }
        }
}
```

### Getting Active Session
```kotlin
val result = repository.getActiveSession()
when (result) {
    is Result.Success -> {
        val activeSession = result.data // LocationSession? or null
    }
    is Result.Error -> showError(result.error.toUiText())
}
```

## Dependency Injection (Koin)

```kotlin
val locationModule = module {
    // Room Database
    single {
        Room.databaseBuilder(
            androidContext(),
            LocationDatabase::class.java,
            LocationDatabase.DATABASE_NAME
        ).build()
    }
    
    // DAOs
    single { get<LocationDatabase>().locationSessionDao() }
    single { get<LocationDatabase>().locationDao() }
    
    // Repository
    singleOf(::LocationSessionRepositoryImpl).bind<LocationSessionRepository>()
}
```

## Files Created

### Database Layer
1. **LocationSessionEntity.kt** - Session entity
2. **LocationEntity.kt** - Location entity
3. **SessionWithLocations.kt** - DTO for session with locations
4. **LocationSessionDao.kt** - DAO for sessions
5. **LocationDao.kt** - DAO for locations
6. **LocationDatabase.kt** - Room database definition

### Data Layer
7. **LocationMappers.kt** - Entity ↔ Domain mapping
8. **LocationSessionRepository.kt** - Repository interface
9. **LocationSessionRepositoryImpl.kt** - Repository implementation

### Domain Layer (Previously Created)
10. **Error.kt** - Base error interface
11. **Result.kt** - Result type for error handling
12. **DataError.kt** - Data layer errors
13. **DataErrorExt.kt** - DataError to UiText mapping

### Presentation Layer
14. **UiText.kt** - UI text abstraction

### Resources
15. **strings.xml** - Error message strings

## Next Steps

### 1. Update ViewModel to Use Repository
Replace in-memory state management with database operations:
```kotlin
class LocationViewModel(
    private val repository: LocationSessionRepository,
    // ... other dependencies
) : AndroidViewModel(application) {
    
    // Observe sessions from database
    val sessions: StateFlow<List<LocationSession>> = 
        repository.getAllSessions()
            .map { result ->
                when (result) {
                    is Result.Success -> result.data
                    is Result.Error -> emptyList()
                }
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    fun startSession() {
        viewModelScope.launch {
            val session = LocationSession(...)
            repository.createSession(session)
        }
    }
    
    fun addLocation(location: LocationData) {
        viewModelScope.launch {
            currentSessionId?.let { sessionId ->
                repository.addLocationToSession(sessionId, location)
            }
        }
    }
}
```

### 2. Handle Database Migrations
If schema changes in future:
```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add migration logic
    }
}

Room.databaseBuilder(...)
    .addMigrations(MIGRATION_1_2)
    .build()
```

### 3. Add Database Cleanup
Implement periodic cleanup of old sessions:
```kotlin
suspend fun deleteSessionsOlderThan(timestamp: Long): EmptyResult<DataError>
```

## Benefits

✅ **Persistent Storage** - Data survives app restarts  
✅ **Type-Safe** - Compile-time error checking  
✅ **Reactive** - UI automatically updates with Flow  
✅ **Efficient** - Indexed queries, optimized performance  
✅ **Clean Architecture** - Repository pattern, separation of concerns  
✅ **Error Handling** - Functional error handling with Result type  
✅ **Testable** - Repository interface allows easy mocking  
✅ **Scalable** - Relational design supports future features  

## Status

✅ Database schema defined  
✅ Entities created  
✅ DAOs implemented  
✅ Repository interface and implementation complete  
✅ Mappers created  
✅ Koin module configured  
✅ Error handling integrated  
✅ All files compile successfully  

**Ready for integration with ViewModel!**
