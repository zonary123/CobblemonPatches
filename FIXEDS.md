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

### 👤 NPC Player Texture Async Loading & Caching (`NPCEntityMixin`)
- **Problem**: `NPCEntity#loadTextureFromGameProfileName` executed synchronous blocking HTTP calls (`GameProfileRepository#findProfilesByNames`, `MinecraftSessionService#fetchProfile`, `URL#openStream`) on the server thread, causing severe server tick freezes (over 1200ms per lookup).
- **Solution**: Implemented two-tier in-memory caching (Caffeine) for player textures (`username -> NPCPlayerTexture`) and skin byte streams (`URI -> byte[]`), combined with in-flight lookup deduplication and asynchronous background network I/O with timeouts.

### ⏱️ Paper-Style Tick-Spreading Autosave (`FileBackedPokemonStoreFactoryMixin`)
- **Problem**: Saving player storage accumulated all dirty party and PC stores and serialized them synchronously on the server thread in a single tick every save interval, causing 600ms+ lag spikes.
- **Solution**: Distributes dirty store serialization across ticks incrementally (1-2 stores per tick), smoothing out CPU usage and eliminating autosave lag spikes completely.

### ⚡ Fast-Path & DFU Bypass DataFixerCodec (`CobblemonDataFixerCodecMixin`)
- **Problem**: `CobblemonDataFixerCodec` cloned full NBT compounds on encode to insert version metadata and always ran DFU updates and field removals on decode even when data was already at current version.
- **Solution**: Injects version metadata directly in-place on encode and bypasses DFU and NBT field manipulation entirely on decode when `inputVersion >= DATA_VERSION`.

### 👥 Entity Cramming & Owner Resolution Optimization (`PokemonEntityMixin`)
- **Problem**: During entity cramming and collision checks, `getScoreboardTeam()` and `getOwner()` searched the server's global player manager by UUID dozens of times per entity per tick, even for wild Pokémon.
- **Solution**: Short-circuits owner lookups for wild Pokémon and caches resolved owner entities per-tick on `PokemonEntity`.

### 🌿 Spawning Pipeline Tag & Identifier Condition Memoization (`RegistryLikeTagConditionMixin`, `RegistryLikeIdentifierConditionMixin`)
- **Problem**: In `FlatSpawnablePositionWeightedSelector.select()` and `Spawner.getMatchingSpawns()`, Cobblemon tests hundreds of spawn detail conditions against every position in the spawning zone. `RegistryLikeTagCondition.fits()` evaluated `t.isIn(tag)` continuously, triggering thousands of `ImmutableCollections$SetN.contains()` and `TagKey.equals()` comparisons per tick.
- **Solution**: Implemented thread-safe $O(1)$ concurrent memoization caches on `RegistryLikeTagCondition` and `RegistryLikeIdentifierCondition`. Once a registry entry (e.g., `minecraft:grass_block` or `minecraft:plains`) is evaluated against a tag or identifier condition, subsequent checks during the spawning pass hit the cache in $O(1)$ time, eliminating the largest CPU consumer in Cobblemon's spawning logic.

### ⏳ Optimized Entity Aging Despawner (`CobblemonAgingDespawnerMixin`)
- **Problem**: `CobblemonAgingDespawner.shouldDespawn()` executed every tick for every active Pokémon entity in the world, iterating through all world players and executing `Math.sqrt` distance calculations continuously (taking 1.30% / 3.9s of total server CPU).
- **Solution**: Throttled despawn checks to 1-second intervals (every 20 ticks) and implemented squared-distance comparisons (`squaredDistanceTo`) with early-exit near-boundary checks, reducing player list iterations and distance calculations by 95%+.

### 🖥️ Throttled PC & Pasture Block Entity Tickers (`PCBlockEntityTickerMixin`, `PokemonPastureBlockEntityTickerMixin`)
- **Problem**: Every PC block and Pasture block placed in loaded chunks executed `getInRangeViewerCount()` on every single server tick (20 Hz) to toggle cosmetic blockstate lights (`ON`), continuously iterating through all players in the dimension and consuming ~0.42% CPU on idle blocks.
- **Solution**: Throttled viewer count evaluations to once every 20 ticks (1 second), distributed across ticks by block position hash to eliminate tick stalls and reduce idle player scans by 95% while keeping visual states responsive.

### 👤 Per-Tick NPC Visibility Cache (`NPCEntityMixin`)
- **Problem**: `NPCEntity#shouldHideFrom` re-queried and parsed player MoLang data and permissions on every spectator and visibility check (`canBeSpectated`), consuming 0.29% CPU on redundant evaluations for the same player-NPC pairs.
- **Solution**: Caches visibility decisions per server tick per player UUID, eliminating repeated MoLang data queries and permission evaluations.

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

### ⚔️ Battle Freeze & Softlock Fixes (`AIBattleActorMixin`, `BattleSelectActionsHandlerMixin`, `ChallengeResponseHandlerMixin`, `PokemonBattleMixin`, `BattleRegistryMixin`, `PokemonEntityMixin`, `SentOutStateMixin`)
- **Fixed AI Choice Softlock (`AIBattleActorMixin`)**: Fixed an unexecuted Kotlin lambda bug in `AIBattleActor#onChoiceRequested` when `request == null` or during `IllegalActionChoiceException`. Previously, failing to return an action response left AI and wild actors in a perpetual `mustChoose = true` state, freezing the battle turn indefinitely. Now safely passes turn responses to keep the engine flowing.
- **Fixed Battling Entity Despawn Queue Lock (`PokemonEntityMixin`)**: Guarded `onStoppedTrackingBy` and the `END_WORLD_TICK` despawn queue from queueing or discarding entities currently participating in battles (`entity.isBattling()` / `getBattleId() != null`) or tamed Pokémon, preventing active battle participants from abruptly vanishing and crashing Showdown turns.
- **Fixed Netty Network Race Conditions (`BattleSelectActionsHandlerMixin`, `ChallengeResponseHandlerMixin`)**: Enforced main server thread execution (`server.execute(...)`) for `BattleSelectActionsHandler` and `ChallengeResponseHandler`, preventing off-thread packet processing from corrupting Showdown battle state and colliding with the server tick loop.
- **Fixed Ghost Battles & Desynced Registry (`BattleRegistryMixin`)**: Removed stale Caffeine cache and added strict `!battle.getEnded()` lifecycle validation to prevent players from getting permanently trapped in ghost battle states after battle termination or player disconnects.
- **Fixed Wild Entity Removal & MoLang Caching (`PokemonBattleMixin`)**: Corrected `PokemonBattle#checkFlee` to resolve and flee battles immediately when wild Pokémon are killed or despawn, and removed premature boolean memoization that previously broke `checkFlee()` during MoLang initialization.
- **Anti-Deadlock Inactivity Watchdog (`PokemonBattleMixin`)**: Added an automated 120-second inactivity watchdog that cleanly resolves and terminates hung battles without requiring server restarts.
- **Safe Pokémon Recall (`SentOutStateMixin`)**: Added null-safety checks for `CobblemonPatches.server` to prevent `NullPointerException` crashes during Pokémon recall.
