# Fixes and Improvements

A comprehensive list of all optimizations, bug fixes, crash preventions, and anti-exploit patches included in **CobblemonPatches**.

---

## 🚀 Performance & Memory Optimizations

### 🍳 Campfire Tick Optimization (`CampfireServerTickMixin`)
- **Problem**: `CampfireBlockEntity#serverTick` constantly recalculates crafting recipes, item assemblies, and seasonings on every single tick even when ingredients don't change or during mid-cook states.
- **Solution**: Implemented fingerprint caching for inventory items. Bypasses redundant recipe lookups and cooking checks during inactive or mid-cooking intervals.

### 🫐 Berry Blocks & Berry Trees (`BerryBlockEntityMixin`, `BerryBlockMixin`)
- **Problem**: `BerryBlockEntity` performed expensive block state lookups (`State.get(IS_ROOTED)`) and `growHelper` executions every single tick, while `BerryBlock` searched `HashMap`s repeatedly for berry types.
- **Solution**: Early-cancels `setStageTimer` when `stageTimer > 0` to skip redundant tick evaluations. Caches the berry type directly on block instances to avoid map lookups.

### 🔑 Showdown ID & Form Caching (`FormDataMixin`, `PokemonMixin`, `SpeciesMixin`)
- **Problem**: Generating `showdownId` and `formOnlyShowdownId` for Pokémon species and forms required repetitive string operations and parsing on hot paths (tooltips, battles, rendering).
- **Solution**: Added high-performance caching for `showdownId` and form identifiers, invalidated only when form, species, or relevant properties change.

### 🧬 Aspects Set Direct Construction (`PokemonMixin`)
- **Problem**: `updateAspects()` allocated multiple intermediate collections, flattening and copying sets on every aspect modification.
- **Solution**: Directly populates a `LinkedHashSet` in place, significantly reducing garbage collection overhead and intermediate object allocation.

### ⚔️ MoveSet Custom Non-Allocating Iterator (`MoveSetMixin`)
- **Problem**: Kotlin's `.filterNotNull()` during MoveSet serialization/deserialization generated temporary `ArrayList` instances on every call.
- **Solution**: Overrode `iterator()` with a custom non-allocating iterator that traverses active moves without temporary list allocations.

### 📊 EVs Map Direct Serialization (`EVsMixin`)
- **Problem**: `getCODEC()` and `getSTREAM_CODEC()` duplicated the entire backing map of `PokemonStats` into a new `HashMap` on every network sync and save.
- **Solution**: Serializes directly using the existing backing map of `PokemonStats`, eliminating unnecessary map cloning.

### 📦 PC Storage & PC Box Iterations (`PCStoreMixin`, `PCBoxMixin`, `PokemonStoreMixin`)
- **Problem**: Iterating through player PC boxes duplicated entire `ArrayList`s, causing severe tick stalls on servers with high player counts.
- **Solution**: Replaced list duplication with direct, memory-friendly iterator lookups and optimized store data retrieval.

### 🏞️ Pasture Blocks PC Lookup (`PokemonPastureBlockEntityTetheringMixin`)
- **Problem**: Tethered Pokémon in pasture blocks iterated box-by-box through the player's whole PC on every tick to find coordinates.
- **Solution**: Directly retrieves and caches the `PCPosition` of tethered Pokémon, eliminating full-PC scans.

### ⚡ Battle Performance Improvements (`PokemonBattleMixin`, `BattleRegistryMixin`)
- **Problem**: Battle queries (`isPvN()`, `isPvP()`, `isPvW()`) and registry lookups performed redundant state evaluations.
- **Solution**: Optimized query paths and registry lookups for smoother battle processing.

---

## 🛡️ Security, Exploits & Anti-Lag

### ❄️ Snowball Lingering & Accumulation Anti-Lag (`SnowballEntityMixin`)
- **Problem**: Snowballs in Minecraft have no natural expiration timer (unlike arrows). When fired into unloaded chunks, bubble columns, or spawned via dispensers/snow golems, they persist forever—often reaching 100,000+ entities and crashing server TPS.
- **Solution**: Automatically discards snowballs exceeding a maximum lifetime of 200 ticks (10 seconds) or stagnant projectiles remaining stationary after 40 ticks (`velocity < 1.0E-6`).

### 📦 Gilded Chest Duplication Exploit (`GildedChestBlockEntityMixin`)
- **Problem**: Players could exploit container interactions from arbitrary distances or desynced states to duplicate items.
- **Solution**: Enforced strict distance verification (`canPlayerUse`) before allowing container interaction.

### 🗂️ Modular Anti-Exploit Architecture (`mixins.exploits`)
- **Purpose**: Dedicated modular package structure (`lag`, `dupe`, `crash`) for all security, anti-grief, and server exploit mitigations.

---

## 🐛 Bug Fixes & Stability

### 🦕 Fossil Machine Crashes (`FossilMultiblockStructureMixin`)
- **Fixed**: Resolved server crash when inserting enchanted items or invalid inventory combinations into the Fossil Machine.

### 🎟️ ChunkTicketManager NPE (`ChunkTicketManagerMixin`)
- **Fixed**: Fixed `NullPointerException` in `handleChunkLeave` when a player disconnected or changed dimensions from an unregistered chunk.

### 🔴 Empty PokeBall NPE (`EmptyPokeballEntityMixin`)
- **Fixed**: Fixed `NullPointerException` in `beginCapture()` when a player throws a Poké Ball and disconnects before the capture sequence executes.

### 🌳 Saccharine Tree Feature Async Removal (`SaccharineTreeFeatureMixin`)
- **Fixed**: Fixed server crash in hybrid/multi-threaded servers caused by asynchronous entity removal during tree generation.

### 👥 Party Pokemon Concurrency Safety (`PokemonMixin`)
- **Fixed**: Ensured party Pokémon removal executes on the main server thread, preventing concurrency race conditions and inventory desyncs.

### ⚡ FastUtil Collection Tick Safety
- **Fixed**: Prevented `ConcurrentModificationException` and state corruption while ticking Cobblemon entities inside FastUtil collections.
