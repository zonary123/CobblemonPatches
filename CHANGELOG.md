# Changelog

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
