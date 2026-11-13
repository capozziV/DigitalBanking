// The Caller type alone, without Spring: the application module of each service depends on this project only,
// while its adapters depend on :libs:security (which re-exports it).
plugins { id("digitalbanking.kotlin-library") }
