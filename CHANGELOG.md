# Changelog

## [1.1.4] - 02-09-2026

> [!WARNING]
> **Testing Required**: Please test this build thoroughly in a testing/staging environment before deploying to production servers.

### Security & Anti-Lag

- **Snowball**: Added `SnowballEntityMixin` to prevent server lag and memory exhaustion from infinite lingering snowballs (discarded after 200 ticks / 10s of flight or if stationary after 40 ticks).
- **Exploits Module**: Created a structured package hierarchy under `org.kingpixel.cobblemonpatches.mixins.exploits` (`lag`, `dupe`, `crash`) for all anti-exploit, anti-dupe, and server protection patches.

### Optimizations

- **Campfire**: Optimized the `serverTick` of `CampfireBlockEntity` by caching recipe availability based on inventory item fingerprints, skipping expensive crafting inputs, assembly checks, and seasonings during inactive periods or mid-cook ticks.
- **BerryBlockEntity**: Optimized the `setStageTimer` mixin to cancel early when the timer is still active (`value > 0`), completely bypassing unnecessary per-tick execution of `growHelper` and the expensive block state lookup (`State.get()`).
- **ShowdownId**: Added `formOnlyShowdownId` and `showdownId` caching to `FormDataMixin` and optimized the caching logic in `PokemonMixin` and `SpeciesMixin`.
- **Aspects**: Optimized `updateAspects` in `PokemonMixin` to directly construct and populate `LinkedHashSet` instead of performing expensive intermediate collection flattening and set copying.
- **MoveSet**: Overrode `iterator()` in `MoveSet` to use a custom non-allocating iterator, bypassing intermediate list generation from Kotlin's `.filterNotNull()` during codec serialization/deserialization.
- **EVs**: Overrode `getCODEC()` and `getSTREAM_CODEC()` in `EVs` with optimized versions that serialize directly using the backing `stats` map of `PokemonStats`, avoiding copying elements to a new `HashMap` on every serialization/sync.
- **Profiling**: Renamed all showdown caching and aspect methods to use the `cobblemonPatches$` prefix for improved readability and tracking when profiling with Spark.

## [1.1.3] - 21-08-2026

### Improvements upon existing patches

- **PastureBlocks**: PC positions of the tethered Pokemon are now retrieved directly instead of instantiating the store position manually

## [1.1.2] - 25-06-2026

### Bug Fixes

- **ChunkTicketManager**: Fixed a NullPointerException in `handleChunkLeave` that occurred when a player disconnected or changed dimensions from a chunk they were not registered in.

## [1.1.1] - 26-01-2026

Compatibility with Cobblemon 1.7.2 and 1.7.3

### Bug Fixes

- A crash in the Fossil Machine was fixed when inserting an enchanted item into the machine. This can only be reproduced
  if the machine allows you to insert enchantable items in Survival.

- GildedChest: fixed a duplication exploit that allowed items to be duplicated.
- Fossil Machine: Fixed the next crash -> https://pastebin.com/R89mSgUG

## [1.1.0] - 2025-12-19 (Upcoming)

### Features

- N/A

### Bug Fixes

- **ServerCommandSource**: Removed command source caching for now because the current implementation was causing memory
  leaks, will evaluate later if it's necessary making a new system or if we should remove this optimization

## [1.0.0] - 2025-12-01

### Features

- N/A

### Bug Fixes

- **SaccharineTreeFeature**: Now no longer crashes when removing an entity asynchronously.
- **EmptyPokeBallEntity**: Prevents a NullPointerException that might occur in rare cases.
- Fixes some crashes while ticking entities related to Cobblemon entities in FastUtil collections

### Optimizations

- **PastureBlocks**: The tick is now optimized to avoid iterating over the entire PC. The PC position is now cached for
  better performance.
- **ShowdownId**: Now cached for faster access.
- **Berry**: **BerryBlockEntity** no longer searches in a HashMap for a specific Berry type. The type of berry is now
  cached.
- **PC**: Now avoids duplicating `ArrayList`. Iterates using an iterator for better efficiency.
