# Changelog

## [1.2.2] - 03-10-2026

## [1.2.1] - 01-10-2026

> [!WARNING]
> **Testing Required**: Please test this build thoroughly in a testing/staging environment before deploying to
> production servers.

### Removed

- **Off-Thread Entity Removal Marshaller**: Removed `EntityRemoveAsyncMixin` to avoid invasive monkey-patching of vanilla Minecraft's `Entity` class and masking third-party threading violations. Off-thread operations are reported with stack trace warnings (`AsyncCatcherFabric`) so offending mods can be identified and fixed in their source.

## [1.2.0] - 27-09-2026

> [!WARNING]
> **Testing Required**: Please test this build thoroughly in a testing/staging environment before deploying to
> production servers.

### Optimizations & Anti-Lag

- **Massive Reduction in World Save Lag**: Resolved the single biggest cause of server lag in Cobblemon (~14% of the entire main server thread). Saving chunks to disk no longer forces the game to repeatedly re-encode data for wild Pokémon whose health and stats haven't changed, eliminating heavy save spikes.
- **Smoother Pokémon Ticking & Data Sync**: The server no longer needlessly recalculates and synchronizes static attributes (species, Poké Balls, scale, friendship, marks, and held items) 20 times every second for every Pokémon. Static data is now synchronized once a second, keeping movement and animations fully responsive while freeing up substantial server CPU.
- **Lighter Despawn & Movement Checks**: Despawn checks and persistence evaluations are now throttled to run smoothly once a second instead of every tick. In addition, on-ground collision checks have been streamlined to eliminate constant memory allocations for unridden Pokémon.
- **Ultra-Fast Spawning Calculations**: Completely overhauled spawn position searches. The game no longer allocates thousands of temporary coordinate objects or scans hundreds of blocks in a cube when Pokémon spawn rules don't require nearby blocks.
- **Lag-Free Habitat Blocks & Sweet Trees**: Fixed massive lag spikes caused by Habitat blocks and honey-slathered logs scanning thousands of blocks in an oversized radius. Searches are now capped to a realistic radius and cached in memory, eliminating multi-second freeze spikes.
- **Smooth Habitat Ticking**: Habitat blocks now distribute their spawning checks evenly across game ticks instead of running heavy calculations all at once every single tick.
- **Safe & Thread-Safe Despawning**: Untracked Pokémon and Pokémon from disconnected players are safely transitioned to an inactive state and cleaned up at the end of the server tick, preventing ghost entities, memory leaks, and duplication exploits.
- **Instant Flee Resolution**: Wild Pokémon fleeing from battles now resolve immediately without stalling the battle pipeline.
- **Configurable Autosave Speed**: Added `autosaveStoresPerTick` (default `2`) in `cobblemonpatches.json`, allowing server admins to customize how many player data files are saved per tick.
- **Staggered Block Ticking**: PC and Pasture block light checks now distribute their work evenly across all coordinate boundaries, preventing localized tick lag around bases.

### Bug Fixes & Stability

- **Prevent Item Duplication Exploits**: Fixed an exploit where switching servers (e.g. via `/spawn`) during battle animations left party Pokémon stranded in the world, allowing duplicated items to be taken. Lingering orphaned Pokémon are now detected and cleaned up automatically.
- **Automatic Battle Recovery Watchdog**: Added a built-in 120-second watchdog timer that automatically detects and resolves stuck or frozen battles, sending a chat notification and restoring player control without requiring a server restart.
- **Prevent Multi-Thread Crashes**: Added protection to safely marshal off-thread entity removal calls from third-party trainer or NPC mods onto the main server thread, preventing server crashes and world corruption.
- **Server Restart Memory Leak Fix**: Added a server lifecycle listener that cleanly unloads static server references and data caches when the server stops, preventing memory leaks across reloads and restarts.
- **Battle Faint vs. Flee Resolution**: Fixed an issue where knocking out a wild Pokémon could erroneously trigger a flee event instead of normal victory resolution.
- **Storage Cache Consistency**: Cached Pokémon IDs are now properly refreshed whenever a storage box is rebuilt, eliminating ghost Pokémon lookups.
- **Fault-Tolerant Autosave Pipeline**: Added isolated error handling during player data saves so that a single corrupted file can never crash the entire server.

## [1.1.9] - 17-09-2026

> [!WARNING]
> **Testing Required**: Please test this build thoroughly in a testing/staging environment before deploying to
> production servers.

### Optimizations & Anti-Lag

- **Eliminated Autosave Lag Spikes**: Previously, the server saved all player Pokémon and PC storage in a single massive burst, causing noticeable server freezes and TPS drops every autosave interval. Storage is now saved smoothly across individual ticks in the background with zero lag spikes.
- **Faster Pokémon Spawning**: The game no longer recalculates biome and block conditions from scratch tens of thousands of times per second. Spawning rules are now remembered in fast memory, dramatically speeding up spawning calculations and freeing up server CPU.
- **Optimized Pokémon Collisions & Cramming**: Wild Pokémon no longer perform constant owner and team searches when bumping into each other or players. This significantly reduces lag when many wild Pokémon group together in dense areas.
- **Smoother Pokémon Despawning**: Wild Pokémon now check player distances efficiently instead of continuously scanning every player in the world 20 times a second, reducing server lag in busy dimensions.
- **Lag-Free PC & Pasture Blocks**: PC and Pasture blocks in loaded bases no longer scan for nearby players 20 times a second just to turn on their screen lights. Checks now run smoothly once a second and are staggered across game ticks so bases with multiple PCs stay lag-free.
- **Faster Pokémon Data Loading & Less Memory Usage**: Streamlined how Pokémon data is read and saved, bypassing unnecessary data conversion steps for modern Pokémon files and reducing memory allocation and garbage collection pauses.
- **Smoother NPC Visibility**: NPC visibility checks are now remembered per game tick, eliminating lag when players look at or interact with trainers and quest NPCs.

### Bug Fixes & Stability

- **Battle Freeze & Softlock Fixes**:
  - **AI & Wild Pokémon Turns**: Fixed battles getting permanently frozen when wild Pokémon or NPC trainers make a move.
  - **Battling Pokémon Despawning**: Fixed active battling Pokémon unexpectedly disappearing mid-battle and breaking the fight.
  - **Ghost Battles**: Fixed players getting permanently stuck in a "already in battle" state after disconnecting or finishing a match.
  - **Automatic Inactivity Recovery**: Added an automatic 120-second watchdog that safely concludes battles that get stuck or frozen, without needing a server restart.
- **Safe Pokémon Recall**: Fixed a server crash that could occur when recalling Pokémon while the server is loading or shutting down.

## [1.1.8] - 11-09-2026

> [!WARNING]
> **Testing Required**: Please test this build thoroughly in a testing/staging environment before deploying to
> production servers.

### Optimizations & Anti-Lag

- **Eliminated Server Freezes from NPC Skins**: When NPCs with custom player skins appear in the world (such as Gym Leaders, shopkeepers, quest NPCs, or town villagers), the server used to completely freeze for 1 to 2 seconds while downloading the skin from the internet. Skin loading now happens silently in the background with zero lag spikes.
- **Smart Skin Memory (Instant Reuse)**: Once a player skin is downloaded for an NPC, it is saved in fast memory. If the same skin is used by multiple NPCs (like identical shopkeepers or trainers), the skin is applied instantly without any repeated web downloads.
- **Lag-Free Skin Commands & Quests**: Using `/applyplayertexture` or changing NPC skins during dialogue trees and quest scripts no longer causes server hitching or TPS drops.
- **Protection Against Slow Skin Servers**: Added automatic timeouts so that even if Mojang's skin servers are slow, laggy, or temporarily offline, your server will never hang or freeze.

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
