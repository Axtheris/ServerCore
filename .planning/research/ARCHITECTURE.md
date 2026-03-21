# Architecture Patterns

**Domain:** Paper 1.21 plugin hardening — nine-system server enhancement plugin
**Researched:** 2026-03-21

---

## Recommended Architecture

The existing Manager/Instance/Tick/Listener pattern is sound and must be preserved. Hardening changes are additive — they add safety layers within each layer's existing responsibility, they do not move responsibilities between layers. Every fix should be locatable to a single component boundary and motivated by a specific failure mode observed in the audit.

### Component Map (Existing)

```
ServerCore.onEnable()
    │
    ├─► [Config Layer] ServerCoreConfig, CosmeticConfig, HologramConfig, ...
    │       Reads YAML, populates registries, validated types
    │
    ├─► [Manager Layer] CosmeticManager, HologramManager, QuestManager, ...
    │       Registry + lifecycle state machine (HashMap<UUID, List<Instance>>)
    │       Entry point for all business logic
    │
    ├─► [Instance Layer] CosmeticInstance, PetInstance, TimelineInstance
    │       Links owner entity to visual representation
    │       Executes per-tick position/state update
    │       Returns false when expired → triggers removal in manager
    │
    ├─► [Tick Task Layer] CosmeticTickTask, PetTickTask, HologramTickTask, ...
    │       BukkitRunnable → calls manager.tickAll() every 1 tick
    │       Sole path for per-frame state mutation
    │
    ├─► [Listener Layer] CosmeticLifecycleListener, HologramLifecycleListener, ...
    │       Reacts to Bukkit events (death, chunk unload, interact)
    │       Triggers manager cleanup and data persistence
    │
    ├─► [Data Store Layer] CosmeticStore, PetStore, QuestStore
    │       YAML serialization / deserialization
    │       Pending state for unloaded entities
    │
    ├─► [Hook Layer] PlaceholderHook, VaultHook, ModelEngineHook
    │       Optional — only loaded when soft dep present
    │
    └─► [API Layer] ServerCoreAPI
            Static accessor to all managers for external plugins
```

### Component Boundaries

| Component | Responsibility | Communicates With | Must Not |
|-----------|---------------|-------------------|----------|
| Manager | Registry + lifecycle state | Instance, Store, API events | Access disk directly |
| Instance | Per-tick update, entity tracking | Bukkit entity API | Hold strong entity refs across ticks |
| Tick Task | Schedule manager.tickAll() | Manager only | Hold any state |
| Listener | React to Bukkit events | Manager, Store | Call tickAll() or tick() directly |
| Config | Parse YAML into objects | Manager (register) | Hold mutable state after load |
| Store | Serialize/deserialize state | Manager (read), filesystem | Fire events or call manager logic |
| Hook | Bridge to third-party API | Manager (read), third-party | Modify manager state |
| API | Expose managers to callers | Manager (read-only preferred) | Create or destroy entities |

---

## Data Flow for Hardened Operations

### Tick Path (must stay fast, all main thread)

```
BukkitScheduler (every 1 tick)
    │
    ▼
{System}TickTask.run()
    │
    ▼
Manager.tickAll()
    │
    ├─► for each Instance:
    │       instance.tick()
    │           ├─ null-check entity refs (WeakReference)
    │           ├─ dead-check: entity.isDead()
    │           ├─ world null-check: entity.getWorld() != null
    │           └─ return false → manager removes from map (safe: iterator-based removal)
    │
    └─► HologramVisibilityTracker.update() (holograms only)
            ├─ null-check hologram.getLocation().getWorld()  [MISSING — BUG]
            └─ for each player: evaluateConditions()
```

### Cleanup Path (event-driven, all main thread)

```
EntityDeathEvent / EntitiesUnloadEvent
    │
    ▼
{System}LifecycleListener.on*()
    │
    ├─ guard: manager.has*(uuid) before calling remove  [already present in cosmetic]
    ├─ idempotent: remove() does nothing if not found   [needs verification per system]
    └─ manager.remove*(uuid)
            │
            ├─ removes from activeMap
            ├─ removes from standIndex (if present)
            ├─ calls instance.destroy() (removes entity from world)
            └─ optionally triggers store.save() [candidate for debounce]
```

### Async Save Path (proposed addition, does not exist yet)

```
Manager.applyCosmetic() / Manager.summonPet()
    │
    ▼
store.markDirty()           ← sets volatile boolean, no I/O
    │
    (periodic timer, every 100 ticks)
    ▼
Store.flushIfDirty()
    │
    ├─ snapshot data on main thread (copy map to local variable)
    ├─ runTaskAsynchronously: serialize snapshot to YAML string
    └─ runTask (back to main): write file / log result
```

---

## Where Safety Fixes Apply (Mapped to Architecture)

### Layer 1: Instance Layer — Entity Lifecycle Safety

**Target files:** `CosmeticInstance.java`, `PetInstance.java`

The instance `tick()` method is already the right place for dead-entity checks. The existing pattern (`mob == null || mob.isDead()`) is correct. What is missing:
- World null check before any location operation (`mob.getWorld() != null`)
- Exception guard around `stand.teleport()` in case stand is removed mid-tick by another thread or plugin

**Fix pattern** (add to `tick()` before teleport):
```java
if (mob.getWorld() == null || stand.getWorld() == null) {
    destroy();
    return false;
}
```

**Confidence:** HIGH — Paper API: `getWorld()` returns null for unloaded entities.

---

### Layer 2: Manager Layer — Event Cancellation Orphan Fix

**Target file:** `CosmeticManager.applyCosmetic()`

Current bug: The armor stand is spawned before `CosmeticApplyEvent` is fired. If another plugin cancels the event, the stand exists in the world but is never tracked. Fix: fire the event first, spawn the stand only if not cancelled.

**Fix pattern:**
```java
// Fire event BEFORE spawning stand
CosmeticApplyEvent event = new CosmeticApplyEvent(mob, item);
Bukkit.getPluginManager().callEvent(event);
if (event.isCancelled()) return false;

// Now safe to spawn
ArmorStand stand = mob.getWorld().spawn(...);
```

This is purely internal to the manager — no interface changes required.

---

### Layer 3: Manager Layer — Hologram Cooldown Memory Leak

**Target file:** `HologramInteractListener.java`

The `cooldowns` HashMap grows forever. Fix: purge expired entries either on access or periodically. A periodic sweep in `HologramTickTask` is the cleanest approach — keeps the listener simple.

**Fix pattern (in-listener on-access purge):**
```java
// At top of onInteract, before put:
cooldowns.entrySet().removeIf(e -> e.getValue() < System.currentTimeMillis());
```

Or delegate to tick task with a `cleanExpiredCooldowns()` method. Either keeps the change contained to the hologram subsystem.

---

### Layer 4: Config Layer — NPC View Distance Validation

**Target file:** `ServerCore.java` (line 259) or `NPCViewTracker` constructor

`viewDistance` from config is used without bounds check. A zero or negative value produces `viewDistanceSq = 0`, meaning no NPCs ever spawn (silent failure).

**Fix pattern (in ServerCore.onEnable before passing to NPCViewTracker):**
```java
int viewDistance = Math.max(1, serverCoreConfig.getNpcViewDistance());
```

Or add the guard inside `NPCViewTracker` constructor with a logged warning. Prefer the constructor guard so the invariant is enforced regardless of how the tracker is constructed.

---

### Layer 5: Manager Layer — Quest FETCH Objective Tracking

**Target file:** `QuestManager.java`, `areObjectivesComplete()`

FETCH objectives call `countMaterial()` on every completion check instead of using stored progress. This means abandoning/re-accepting does not reset the counter because there is no counter — it re-reads inventory each time. This is a semantic bug, not a performance bug.

Two options:
1. **Snapshot on accept** — store item count at `acceptQuest()` time, use stored count only
2. **Inventory-based is fine, but make it explicit** — document the on-demand design; fix is to set `progress[i]` from inventory count when FETCH is checked, so `areObjectivesComplete` and `completeQuest` are consistent

Option 2 is lower risk — changes are isolated to `areObjectivesComplete`. Option 1 requires schema change to `QuestProgress`.

---

### Layer 6: Visibility Tracker — Null World Safety

**Target file:** `HologramVisibilityTracker.java`, line 42

`player.getLocation().distanceSquared(holoLoc)` throws if the hologram's location has a null world (world unloaded). The existing world name equality check (line 41) guards against mismatched worlds but not null world.

**Fix pattern:**
```java
if (hologram.getLocation() == null || hologram.getLocation().getWorld() == null) continue;
```

Add before the `distanceSquared` call. This is a one-line guard in an existing hot path.

---

### Layer 7: Config Layer — Quest Explore Coordinate Pre-parsing

**Target file:** `QuestManager.handleExplore()` + `QuestConfig.java`

`handleExplore()` splits and parses `"world,x,y,z"` on every player movement event. Pre-parse during `QuestConfig.loadAll()` into a `Location` stored on `QuestObjective`. Requires adding a field to `QuestObjective` (or a wrapper), but no interface changes to `QuestManager` callers.

**Fix location:** `QuestObjective` class gains a `parsedLocation` field (nullable). `QuestConfig` populates it. `handleExplore` reads it directly, skipping the split/parse hot path.

---

### Layer 8: Data Store Layer — Save Debouncing

**Target files:** `CosmeticManager.applyCosmetic()`, `CosmeticManager.removeCosmetics()`, `PetManager`, corresponding Stores

Synchronous `store.save()` on every operation blocks main thread with YAML serialization. Pattern: introduce a `dirty` flag and a periodic flush task.

**Fix pattern:**
```java
// Store gains:
private volatile boolean dirty = false;
public void markDirty() { dirty = true; }
public void flushIfDirty(CosmeticManager manager) {
    if (!dirty) return;
    dirty = false;
    // snapshot on main thread, serialize async
    Map<UUID, List<CosmeticInstance>> snapshot = new HashMap<>(manager.getActiveCosmetics());
    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
        // build YAML from snapshot — no Bukkit API access here
        saveYamlString(serializeSnapshot(snapshot));
    });
}
```

`ServerCore.onEnable()` schedules a repeating task every 100 ticks calling `store.flushIfDirty()`. `onDisable()` calls synchronous `store.save()` directly (still needed for shutdown).

**Thread safety note:** Standard Paper servers run all Bukkit event handlers and tick tasks on the single main thread. The HashMaps in managers are only accessed from the main thread — the async task receives a snapshot copy. The snapshot copy itself is made on the main thread before the async task is launched. This is safe because HashMap read+copy is atomic from the perspective of a single-threaded caller.

---

### Layer 9: NPC Layer — Fragile Init Pattern

**Target file:** `NPCManager.java`

`renderer` and `viewTracker` are null until `init()` is called. `tickAll()` guards only for `renderer == null` but not `viewTracker`. If `init()` is never called (PacketEvents absent), the tick task still calls `tickAll()`, and line 68 (`viewTracker.getViewers()`) would NPE if the renderer guard didn't already return.

**Fix:** The renderer null-guard already covers the case. Make this explicit by combining:
```java
public void tickAll() {
    if (renderer == null || viewTracker == null) return;
    // ...
}
```

Or, more robustly: do not register the tick task if PacketEvents is absent (in `ServerCore.onEnable`). The tick task is currently registered inside the `if (packetEventsPresent)` block at line 270 — so this is already correct. The guard inside `tickAll()` is defensive coding for unexpected call sites.

---

### Layer 10: Hologram Layer — Chunk Key Duplication

**Target files:** `Hologram.java` (getChunkKey), `HologramLifecycleListener.java` (inline formula twice)

Same `((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL)` formula in three places (Hologram.getChunkKey(), listener onLoad, listener onUnload). If one changes, the others silently diverge. Fix: listener should call `hologram.getChunkKey()` directly rather than recomputing:

```java
// In HologramLifecycleListener:
long chunkKey = ((long) event.getChunk().getX() << 32) | (event.getChunk().getZ() & 0xFFFFFFFFL);
// Should instead use a shared utility or compare to hologram.getChunkKey()
```

The formula is currently identical across all three sites so there is no active divergence, but extracting to a utility method (e.g., `ChunkKey.of(int chunkX, int chunkZ)`) eliminates the risk.

---

## Patterns to Follow

### Pattern 1: Guard-then-Act in Listeners

**What:** Before calling any manager method in a listener, check existence. Manager removal is idempotent (returns null or does nothing if not found), but the guard prevents unnecessary event calls (CosmeticRemoveEvent, etc.) and store saves.

**When:** All lifecycle listeners (death, chunk unload).

**Example (already used in CosmeticLifecycleListener):**
```java
if (manager.hasCosmetics(entity.getUniqueId())) {
    manager.removeCosmetics(entity.getUniqueId());
}
```

Apply the same pattern to all systems that lack it (check PetLifecycleListener, EmitterLifecycleListener).

---

### Pattern 2: Fail-Fast Config Validation

**What:** Validate config values at load time with explicit warnings. Do not silently fall back to broken defaults.

**When:** Any numeric config value that has a valid range constraint (viewDistance > 0, cooldown >= 0, etc.).

**Example:**
```java
int viewDistance = config.getInt("systems.npcs.view-distance", 48);
if (viewDistance <= 0) {
    plugin.getLogger().warning("systems.npcs.view-distance must be > 0, defaulting to 48");
    viewDistance = 48;
}
```

This pattern applies at the Config layer, not the Manager layer.

---

### Pattern 3: Snapshot-then-Async for I/O

**What:** When saving data asynchronously, snapshot the mutable state on the main thread before handing off to the async task. The async task operates only on the immutable snapshot.

**When:** Any store.save() call that blocks the main thread.

**Example:**
```java
// On main thread:
Map<UUID, List<ItemStack>> snapshot = deepCopy(activeCosmetics);
// Off main thread:
Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> writeYaml(snapshot));
```

---

### Pattern 4: Event-Before-Spawn (cancellable events)

**What:** Fire cancellable events before creating world entities. If cancelled, no entity is spawned.

**When:** Any `Manager.apply*()` or `Manager.summon*()` method that fires a cancellable event.

**Current violation:** `CosmeticManager.applyCosmetic()` spawns the ArmorStand before checking `event.isCancelled()`.

---

## Anti-Patterns to Avoid

### Anti-Pattern 1: ConcurrentHashMap as a Thread Safety Catch-All

**What:** Replacing HashMap with ConcurrentHashMap in managers without understanding the threading model.

**Why bad:** All Bukkit event handlers, tick tasks, and commands run on the main server thread. Concurrent data structures solve cross-thread access races, not single-thread iteration/modification patterns. `ConcurrentHashMap` does not prevent `ConcurrentModificationException` from within the same thread if you iterate while removing. The `removeIf()` pattern already used in `CosmeticManager.tickAll()` is the correct single-threaded approach.

**Instead:** Keep HashMaps. Use iterator-based removal (`removeIf()`, `iterator.remove()`) in tick loops. Use `ConcurrentHashMap` only if async tasks genuinely need to read manager state without a main-thread snapshot.

---

### Anti-Pattern 2: Accessing Bukkit API from Async Tasks

**What:** Calling `Bukkit.getEntity()`, `entity.getWorld()`, `player.getInventory()` from `runTaskAsynchronously`.

**Why bad:** Paper explicitly prohibits this. Most Bukkit state is not thread-safe. Silent data corruption or crashes result.

**Instead:** Read Bukkit state on main thread, copy to local variable, pass copy to async task. Return to main thread for any write-back.

---

### Anti-Pattern 3: Unbounded Collections in Long-Running Servers

**What:** Maps that grow with every player-hologram interaction or entity UUID pair and are never purged.

**Why bad:** Memory pressure accumulates over weeks of server uptime. OutOfMemoryError terminates the server.

**Instead:** Periodic cleanup via tick task (sweep every N ticks, remove expired entries). For cooldown maps, purge on-access when adding a new entry.

---

### Anti-Pattern 4: Silent Fallback on Validation Failure

**What:** Catching `IllegalArgumentException` from `EntityType.valueOf()` or `Material.matchMaterial()` and returning null/false without logging.

**Why bad:** Config errors (typos, wrong entity names) produce silent no-ops. Server operator cannot diagnose why a cosmetic or quest isn't working.

**Instead:** Log a warning with the invalid value and the config key location before returning the fallback.

---

### Anti-Pattern 5: Deferred Init Without Clear State Contract

**What:** Manager fields (`renderer`, `viewTracker` in NPCManager) that are null until `init()` is called, with callers checking null at use-sites rather than at initialization.

**Why bad:** If `init()` is forgotten or called too late, null checks become load-bearing correctness guards scattered across the class. Any new call site must remember to null-check.

**Instead:** Either guarantee `init()` is called before the manager is used (enforce via guard in NPCManager constructor or a state enum), or initialize with no-op defaults that safely do nothing.

---

## Scalability Considerations

| Concern | Now (small server) | At production scale | Mitigation |
|---------|-------------------|---------------------|------------|
| Hologram visibility O(n*m) | Fast | Slow with 50+ holograms and 100+ players | Chunk-based spatial index; already has `getChunkKey()` scaffolding |
| Reactive evaluation O(n*m) | Fast | Slow with many rules and players | Cache condition results per player per rule per tick interval |
| Stand UUID index growth | Negligible | Thousands of dead entries after weeks | Periodic purge of UUIDs not referenced by active instances |
| Hologram cooldown map | Negligible | Millions of entries with many players | on-access purge or scheduled sweep |
| Hologram spawnAll() on enable | Instant | Multi-second lag with 1000+ holograms | Batch spawn across ticks instead of synchronous loop |

---

## Suggested Fix Order (Dependencies Between Fixes)

The fixes below are ordered by dependency chain. Fixes early in the list must not depend on fixes later in the list. This order is also risk-ascending — simpler, lower-risk fixes first.

### Phase 1: Correctness (no behavior changes for correct configs)

1. **CosmeticManager orphan entity fix** — reorder event fire before stand spawn
   - Depends on: nothing
   - Risk: LOW — reordering two lines, no logic change for non-cancelled events

2. **HologramVisibilityTracker null world guard** — add world null check before distanceSquared
   - Depends on: nothing
   - Risk: LOW — one-line guard, early continue

3. **NPCViewTracker view distance validation** — clamp to >= 1 in constructor or onEnable
   - Depends on: nothing
   - Risk: LOW — adds a Math.max, no structural change

4. **NPCManager tickAll null guard** — add `viewTracker == null` guard
   - Depends on: nothing
   - Risk: LOW — defensive guard, already partially exists

5. **Hologram chunk key deduplication** — extract formula to `Hologram.getChunkKey()` call in listener
   - Depends on: nothing (Hologram.getChunkKey already exists)
   - Risk: LOW — replaces inline formula with method call

### Phase 2: Memory Leak Fixes

6. **Hologram cooldown map purge** — add on-access or scheduled sweep
   - Depends on: nothing
   - Risk: LOW — additive, does not change interaction logic

7. **Stand UUID index leak** — periodic purge of dead UUID entries in standIndex
   - Depends on: nothing
   - Risk: MEDIUM — must not remove entries for live instances; sweep must validate

### Phase 3: Logic Correctness

8. **Quest FETCH objective fix** — make FETCH progress consistent at accept time or during check
   - Depends on: nothing from earlier phases
   - Risk: MEDIUM — semantic change to quest progression; test manually

9. **Quest explore coordinate pre-parse** — add `parsedLocation` to QuestObjective, populate in config
   - Depends on: nothing
   - Risk: MEDIUM — requires QuestObjective field addition; no interface change to callers

10. **Null safety audit across managers** — systematic review of HologramManager, QuestManager hot paths
    - Depends on: phases 1-2 complete (to avoid fixing same file multiple times)
    - Risk: MEDIUM — broad scope

### Phase 4: Performance and I/O

11. **Data store debounce** — dirty flag + periodic async flush for CosmeticStore and PetStore
    - Depends on: correctness fixes done (no point debouncing a broken save)
    - Risk: MEDIUM — introduces async code; requires snapshot pattern

12. **PlaceholderAPI reflection replacement** — replace static reflection cache with API-stable approach
    - Depends on: nothing, but deferred because it requires verifying PAPI API surface
    - Risk: HIGH — touches hologram placeholder rendering; verify PAPI compatibility first

### Phase 5: Security

13. **NPC skin texture validation** — validate Base64 format before sending to clients
    - Depends on: nothing
    - Risk: LOW — additive validation; reject invalid strings early

14. **Hologram action permission check** — add optional per-action permission in config and listener
    - Depends on: nothing
    - Risk: LOW for optional permission; existing behavior unchanged if permission not configured

---

## Sources

- Paper 1.21 scheduler documentation: https://docs.papermc.io/paper/dev/scheduler/
- Paper 1.21 BukkitScheduler API: https://jd.papermc.io/paper/1.21.1/org/bukkit/scheduler/BukkitScheduler.html
- SpigotMC HashMap vs ConcurrentHashMap discussion: https://www.spigotmc.org/threads/hashmap-vs-concurrenthashmap.419461/
- Folia documentation (threading model reference): https://docs.papermc.io/folia/
- Paper entity lifecycle issues (chunk unload/load): https://github.com/PaperMC/Paper/issues/3030
- Code audit source: `.planning/codebase/CONCERNS.md`, `.planning/codebase/ARCHITECTURE.md`

*Architecture research: 2026-03-21*
