# Changelog

## [1.1.1] - 26-01-2026

Compatibility with Cobblemon 1.7.2

### Bug Fixes

- A crash in the Fossil Machine was fixed when inserting an enchanted item into the machine. This can only be reproduced
  if the machine allows you to insert enchantable items in Survival.

- GildedChest: fixed a duplication exploit that allowed items to be duplicated. The issue consisted of moving far enough
  away from the chest until it was in unloaded chunks. Distance checks (in
  blocks) and dimension checks were added to prevent the use of commands such as Home or delays that would allow
  teleporting to the same coordinates in different dimensions.

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
