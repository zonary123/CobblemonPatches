# Changelog

## [1.1.8] - 11-09-2026

> [!WARNING]
> **Testing Required**: Please test this build thoroughly in a testing/staging environment before deploying to
> production servers.

### Optimizations & Anti-Lag

- **Asynchronous NPC Player Texture Loading**: Fixed massive server tick stalls and lag spikes (e.g. 1000ms+ freezes) caused by `NPCEntity#loadTextureFromGameProfileName` performing synchronous Mojang API and session profile lookups directly on the main server thread.
- **Dual-Layer Texture Caching**: Implemented high-performance Caffeine in-memory caching for resolved player profile textures (`username -> NPCPlayerTexture`) and downloaded skin image payloads (`URI -> byte[]`), completely eliminating redundant web requests.
- **In-Flight Lookup Deduplication**: Added concurrent deduplication for in-flight profile texture lookups to prevent parallel requests when multiple NPCs with the same player skin spawn simultaneously.
- **Network I/O Safety & Timeouts**: Offloaded all external HTTP requests to worker I/O threads with strict connect and read timeouts to prevent thread starvation and server hangs.

## [1.1.7] - 10-09-2026

> [!WARNING]
> **Testing Required**: Please test this build thoroughly in a testing/staging environment before deploying to
> production servers.

### Added

- **Move Suggestions in Commands**: Added autocomplete suggestions for the `moves` property in commands like `/pokegive`, `/pokespawn`, and `/pokemonedit`. Players can now easily tab-complete up to 4 moves separated by commas (e.g. `moves=tackle,growl,scratch`).
- **Server-Only Support**: Works 100% server-side. Players connecting with vanilla Cobblemon will automatically get full move suggestions without needing any client-side mods installed.

### Optimizations

- **Autocompletion Performance**: Optimized move list caching and text parsing to ensure instant suggestion popups with zero server lag.

## [1.1.6] - 06-09-2026

> [!WARNING]
> **Testing Required**: Please test this build thoroughly in a testing/staging environment before deploying to
> production servers.

### Bug Fixes

- **Scoreboard Team Desync**: Fixed a client and server crash (`IllegalStateException`) caused when removing players or
  entities from scoreboard teams that were already cleared or desynchronized.

## [1.1.5] - 06-09-2026

> [!WARNING]
> **Testing Required**: Please test this build thoroughly in a testing/staging environment before deploying to
> production servers.

### Optimizations

- **Type Gem Growth**: Significantly improved world generation speed and server performance by optimizing how Type Gem
  Cores grow clusters, eliminating lag and memory overhead during chunk generation and random ticks.

## [1.1.4] - 02-09-2026

> [!WARNING]
> **Testing Required**: Please test this build thoroughly in a testing/staging environment before deploying to
> production servers.

### Security & Anti-Lag

- **Snowballs**: Fixed server lag caused by infinite lingering snowballs (automatically removes snowballs that are stuck
  or flying for too long).
- **Anti-Exploit System**: Added a structured module to organize and handle anti-lag, anti-dupe, and server security
  patches.

### Optimizations

- **Campfires**: Significantly reduced server lag caused by campfires by skipping redundant recipe and cooking checks
  every tick.
- **Showdown IDs**: Improved performance when calculating Pokémon battle IDs by caching them.
- **Pokémon Aspects**: Optimized how Pokémon visual aspects and forms are processed to lower memory usage.
- **Moves & EVs**: Optimized Pokémon move lists and EV stat synchronization to reduce lag and memory allocation during
  saves and syncs.
- **Profiling**: Improved method naming to make performance tracking easier when profiling with Spark.

### Changed

- **Cobblemon 1.8.0 Compatibility**: Updated compatibility and dependencies to support Cobblemon 1.8.0 (Minecraft
  1.21.1).
- **Pasture Block Improvements**: Refactored implementation for better stability and efficiency.

### Bug Fixes

- **Null Entity Handling**: Fixed issues with null entity cases during tracking start/end operations.
- **Thread Safety**: Ensured entity tracking operations run on the correct server thread.

### Removed

- **Berry Block Entity Mixin**: Removed the berry block entity mixin and its performance optimizations.
- **Gilded Chest Mixin**: Removed the mixin handling gilded chest duplication.

## [1.1.3] - 21-08-2026

### Improvements upon existing patches

- **PastureBlocks**: PC positions of tethered Pokémon are now retrieved directly instead of manually searching the
  entire PC.

## [1.1.2] - 25-06-2026

### Bug Fixes

- **ChunkTicketManager**: Fixed a crash (NullPointerException) that occurred when a player disconnected or changed
  dimensions from an unregistered chunk.

## [1.1.1] - 26-01-2026

Compatibility with Cobblemon 1.7.2 and 1.7.3

### Bug Fixes

- **Fossil Machine**: Fixed a crash when inserting enchanted items into the machine in Survival.

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
