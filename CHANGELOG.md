# Changelog

## [1.1.4] - 02-09-2026

> [!WARNING]
> **Testing Required**: Please test this build thoroughly in a testing/staging environment before deploying to production servers.

### Security & Anti-Lag

- **Snowballs**: Fixed server lag caused by infinite lingering snowballs (automatically removes snowballs that are stuck or flying for too long).
- **Anti-Exploit System**: Added a structured module to organize and handle anti-lag, anti-dupe, and server security patches.

### Optimizations

- **Campfires**: Significantly reduced server lag caused by campfires by skipping redundant recipe and cooking checks every tick.
- **Berry Plants**: Reduced lag from berry plants by avoiding unnecessary tick updates and improving berry type lookups.
- **Showdown IDs**: Improved performance when calculating Pokémon battle IDs by caching them.
- **Pokémon Aspects**: Optimized how Pokémon visual aspects and forms are processed to lower memory usage.
- **Moves & EVs**: Optimized Pokémon move lists and EV stat synchronization to reduce lag and memory allocation during saves and syncs.
- **Profiling**: Improved method naming to make performance tracking easier when profiling with Spark.

## [1.1.3] - 21-08-2026

### Improvements upon existing patches

- **PastureBlocks**: PC positions of tethered Pokémon are now retrieved directly instead of manually searching the entire PC.

## [1.1.2] - 25-06-2026

### Bug Fixes

- **ChunkTicketManager**: Fixed a crash (NullPointerException) that occurred when a player disconnected or changed dimensions from an unregistered chunk.

## [1.1.1] - 26-01-2026

Compatibility with Cobblemon 1.7.2 and 1.7.3

### Bug Fixes

- **Fossil Machine**: Fixed a crash when inserting enchanted items into the machine in Survival.
- **Gilded Chest**: Fixed an item duplication exploit with gilded chests.
- **Fossil Machine**: Fixed a rare server crash during fossil operations.

## [1.1.0] - 2025-12-19

### Bug Fixes

- **ServerCommandSource**: Removed command source caching to prevent potential memory leaks.

## [1.0.0] - 2025-12-01

### Bug Fixes

- **Saccharine Tree**: Fixed a server crash caused by asynchronous entity removal.
- **Empty Poké Ball**: Fixed a crash (NullPointerException) when capturing Pokémon in edge cases.
- **Entity Collections**: Fixed crashes while ticking Cobblemon entities in FastUtil collections.

### Optimizations

- **Pasture Blocks**: Optimized Pasture Block ticking by caching PC positions to avoid scanning the entire PC.
- **Showdown ID**: Added caching to speed up Pokémon ID lookups.
- **Berry Blocks**: Improved berry handling by caching plant types and reducing unnecessary block updates.
- **PC Storage**: Optimized PC box iteration to reduce memory duplication and lag.
