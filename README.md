# CobblemonPatches
 
**CobblemonPatches** is a high-performance server and client optimization mod for **Cobblemon (Minecraft 1.21.1)**. It employs surgical **Mixins** to optimize hot paths, resolve memory leaks, fix critical crashes, eliminate lag machines, prevent battle freezes, and stop duplication exploits—**all without altering core gameplay mechanics or removing features**.
 
---
 
## 📌 Key Highlights
 
- 🚀 **High-Impact Optimizations**: Drastically cuts CPU usage and tick times across Spawning rules, Campfire, Berry Blocks, PC storage, Pasture Blocks, Showdown IDs, MoveSet, and EVs serialization.
- ⚔️ **Battle Softlock Prevention & Turn Timer**: Eliminates AI freezes, packet race conditions, ghost battle locks, and includes a configurable Action Bar turn countdown with auto-move selection.
- 🛡️ **Anti-Lag & Exploit Protection**: Eliminates snowball accumulation exploits (e.g. 100k+ lingering entities) and container duplication exploits.
- 👤 **Asynchronous NPC Skins**: Background skin downloading and two-tier caching eliminates 1-2 second server freezes when quest/trainer NPCs spawn.
- ⏱️ **Smooth Background Autosave**: Spreads player party and PC serialization across ticks, completely eliminating periodic 600ms+ autosave lag spikes.
- 🐛 **Crash Preventions & Thread Safety**: Fixes Fossil Machine crashes, asynchronous entity removal crashes, and NullPointerExceptions on player disconnects.
- ⚡ **Zero Gameplay Compromises**: Preserves 100% vanilla Cobblemon behavior, data structures, and compatibility.
- 🔍 **Spark Profiling Friendly**: Method prefixes (`cobblemonpatches$`) allow easy tracking in Spark/async-profiler samplers.
 
---
 
## 🧩 Summary of Features & Patches
 
For a detailed technical breakdown of every patch, see [`FIXEDS.md`](FIXEDS.md).
 
### ⚔️ Battle Stability & Freeze Fixes
* **🛡️ AI Choice Softlock Fix**: Fixed uninvoked Kotlin lambda bug when `request == null`, preventing AI/wild Pokémon from hanging turns.
* **🛑 Anti-Deadlock Watchdog**: Automatically recovers and terminates battles that remain stuck for over 120 seconds.
* **👥 Thread-Safe Packets**: Enforces main server thread execution for `BattleSelectActionsHandler` and `ChallengeResponseHandler`.
* **🔒 Battle Registry & Despawn Safety**: Eliminates ghost battle states and prevents battling entities from being discarded by despawn queues.
 
### 🚀 Performance & Memory
* **⏱️ Paper-Style Tick-Spreading Autosave**: Distributes dirty party and PC stores across ticks incrementally, eliminating autosave lag spikes.
* **🌿 Spawning Condition Memoization**: $O(1)$ concurrent memoization caches for biome and tag conditions, eliminating the largest CPU consumer in Cobblemon spawning.
* **👤 Async NPC Skin Loading & Caching**: Asynchronous HTTP profile fetching and two-tier caching (`username -> texture` and `URI -> bytes`) for zero-lag NPC spawns.
* **🍳 Campfire Engine**: Caches recipe lookups and item fingerprints to bypass expensive per-tick recipe checks.
* **🫐 Berry Blocks & Trees**: Early-cancels redundant stage timer ticks and caches berry types to eliminate HashMap lookups.
* **🔑 Showdown & Form ID Caching**: Caches `showdownId` and `formOnlyShowdownId` on Pokémon, species, and forms.
* **🧬 Aspect Set Construction**: Direct in-place `LinkedHashSet` generation, eliminating intermediate collection overhead.
* **⚔️ MoveSet Iterator**: Non-allocating move iterator bypassing Kotlin `.filterNotNull()` list allocations.
* **📊 EVs Serialization**: Serializes stats directly from backing maps without cloning to new `HashMap` instances.
* **📦 PC Storage & Box Iterations**: Replaced full `ArrayList` duplication with stream-lined iterator lookups.
* **🏞️ Pasture Blocks**: Caches tethered Pokémon `PCPosition` to avoid scanning entire PC boxes every tick.
* **⚡ Fast-Path DataFixerCodec**: Injects version metadata in-place and bypasses redundant DFU decode passes for current-version data.
* **👥 Entity Cramming & Owner Resolution**: Short-circuits owner resolution for wild Pokémon and caches resolved owners per tick.
 
### 🛡️ Security & Anti-Exploit
* **❄️ Snowball Anti-Lag**: Discards lingering snowballs exceeding 200 ticks (10s) or stagnant stationary projectiles (>40 ticks), preventing 100k+ entity lag attacks.
* **📦 Gilded Chest Anti-Dupe**: Enforces strict distance checks (`canPlayerUse`) to prevent container duplication exploits.
* **🗂️ Modular Exploit Architecture**: Structured modular package system (`lag`, `dupe`, `crash`) for server security.
 
### 🐛 Stability & Bug Fixes
* **🦕 Fossil Machine**: Fixed server crashes when inserting enchanted items or specific survival inventories.
* **🎟️ ChunkTicketManager**: Fixed `NullPointerException` when players leave unregistered chunks.
* **🔴 Empty Poké Ball**: Fixed `NullPointerException` during capture sequences when players disconnect mid-flight.
* **🌳 Saccharine Tree**: Fixed asynchronous entity removal crashes on hybrid/multi-threaded servers.
* **👥 Concurrency Safety**: Forces party Pokémon removal onto the main thread to prevent race conditions.
 
---
 
## ⚙️ Configuration (`config/cobblemonpatches.json`)
 
```json
{
  "debug": false,
  "battleInactivityTimeoutMessage": "&cBattle timed out due to inactivity."
}
```
 
---
 
## 🔧 Installation & Requirements
 
1. Ensure **Minecraft 1.21.1** and **Fabric Loader** (>= 0.16.x) are installed.
2. Ensure **Cobblemon** (1.8.1+) is present in your `mods` folder.
3. Place the `CobblemonPatches-1.1.9.jar` file into your `mods/` directory.
4. Start your client or server.
 
---
 
## 📄 Documentation & Links
 
- 📋 [**Detailed Fixes & Improvements**](FIXEDS.md)
- 📝 [**Changelog**](CHANGELOG.md)
- 🤝 [**Contributing Guidelines**](CONTRIBUTING.md)
- 📜 [**Contributor License Agreement (CLA)**](CLA.md)
- ⚖️ [**License**](LICENSE)
 
---
 
✨ *Built with care to provide the cleanest, fastest, and most stable Cobblemon experience.*
