# Phase 3: Async Persistence and Performance - Context

**Gathered:** 2026-03-21
**Status:** Ready for planning

<domain>
## Phase Boundary

Data saves never block the main thread during play and quest explore checks allocate no garbage per tick. The three data stores (cosmetics, pets, quests) must use dirty-flag debounced saves with snapshot-then-async I/O. Quest explore objectives must pre-parse target coordinates at config load time. No new features — only persistence and allocation fixes within existing component boundaries.

**Requirements:** PERS-01, PERS-02, PERS-03, PERF-01

</domain>

<decisions>
## Implementation Decisions

### Dirty-flag scope (PERS-01)
- **D-01:** Add dirty-flag tracking to all three data stores (CosmeticStore, PetStore, QuestStore) for consistency, even though PetStore and QuestStore currently only save in `onDisable()`. The flag lets future mutation call sites trigger debounced saves without code-path audits.
- **D-02:** CosmeticStore is the critical fix — it currently calls `store.save(this)` on every `applyCosmetic()` (line 79) and `removeCosmetics()` (line 97), producing full-file YAML serialization and disk write on the main thread per operation.
- **D-03:** Mutation methods (`addPet`, `removePet`, quest accept/complete/abandon, cosmetic apply/remove) set the dirty flag instead of triggering immediate saves. The store tracks `private boolean dirty = false` with `markDirty()` and `isDirty()` accessors.

### Periodic flush interval (PERS-01)
- **D-04:** A periodic flush runs every 6000 ticks (5 minutes) for each store. This matches the Phase 2 sweep interval pattern and is standard for Bukkit plugin auto-save.
- **D-05:** The flush is driven by the existing tick task infrastructure — either a counter in the manager's `tickAll()` or a dedicated low-frequency BukkitRunnable. Claude's discretion on which approach is cleaner.
- **D-06:** The flush checks `isDirty()` before writing — if the store is clean, the flush is a no-op (no disk I/O for idle stores).

### Snapshot-then-async pattern (PERS-02)
- **D-07:** The save cycle is: (1) main thread snapshots data into a `YamlConfiguration` object, (2) async thread writes the YamlConfiguration to a temp file (`cosmetic-data.yml.tmp`), (3) async thread renames temp to real file. This ensures the main thread only does in-memory work and the previous file survives failed writes.
- **D-08:** The snapshot captures a deep copy of all data needed for serialization. For CosmeticStore, this means iterating `activeCosmetics` and `pending` on the main thread to build the YamlConfiguration. For PetStore, iterating `ownedPets`. For QuestStore, iterating `activeQuests` and `completedQuests`.
- **D-09:** Only one async write can be in flight per store at a time. A `private volatile boolean saving = false` guard prevents overlapping writes. If a flush finds `saving == true`, it skips that cycle (the next flush will pick it up).
- **D-10:** The async task uses `Bukkit.getScheduler().runTaskAsynchronously(plugin, ...)` — standard Paper async pattern.

### Synchronous flush in onDisable (PERS-03)
- **D-11:** `onDisable()` performs a SYNCHRONOUS save regardless of dirty flag — it writes all data directly on the main thread (the current behavior). This is required because async tasks submitted during `onDisable()` may not complete before process exit.
- **D-12:** The existing `onDisable()` pattern (cancel task → save → destroy) is correct and stays. The only change is that the save method must be callable synchronously (skipping the async path) when called from `onDisable()`.
- **D-13:** Add a `saveSync()` / `saveAsync()` distinction, or a boolean parameter `async` to the save method. `onDisable()` always calls the sync variant.

### Atomic write safety (PERS-02)
- **D-14:** Write to `<filename>.tmp` first, then use `Files.move(tmp, real, ATOMIC_MOVE, REPLACE_EXISTING)` to atomically swap. If `ATOMIC_MOVE` fails (some filesystems), fall back to `REPLACE_EXISTING` alone. A failed async write never corrupts the real file.
- **D-15:** On startup, if a `.tmp` file exists alongside the real file, delete the `.tmp` file (it's a failed write). If ONLY a `.tmp` file exists (real file missing), rename `.tmp` to real (the rename failed after a successful write).

### Explore coordinate pre-parsing (PERF-01)
- **D-16:** Add an immutable record `ExploreTarget(String worldName, double x, double y, double z)` to QuestObjective (or as a nested class/record).
- **D-17:** Parse the target string `"world,100,64,200"` at config load time in `QuestObjective.fromConfig()` (line 53). Store the result as a `private final ExploreTarget exploreTarget` field, accessible via `getExploreTarget()`. Returns null for non-EXPLORE objectives.
- **D-18:** `QuestManager.handleExplore()` (line 342) uses `obj.getExploreTarget()` instead of `obj.getTarget().split(",")` — eliminates String array allocation, three Double.parseDouble() calls, and NumberFormatException path per tick per explore objective.
- **D-19:** World lookup stays at check time (`Bukkit.getWorld(worldName)` or `location.getWorld().getName().equalsIgnoreCase(worldName)`) because worlds may not be loaded when the config is read at startup. The pre-parsed record stores the world NAME, not a World reference.
- **D-20:** Malformed explore targets (wrong part count, non-numeric coordinates) are caught at config load time with a WARNING log, and the objective is skipped. This is a fail-fast improvement over the current silent catch at line 360.

### Claude's Discretion
- Whether the periodic flush uses a counter in existing tick tasks or a separate BukkitRunnable
- Whether the snapshot method is on the Store class or extracted to a helper
- Whether `saveSync()`/`saveAsync()` are separate methods or a boolean parameter
- Exact placement of the `ExploreTarget` record (nested in QuestObjective vs. standalone class)
- Whether to combine all three store flushes into one periodic task or keep separate

</decisions>

<specifics>
## Specific Ideas

- Prior phases used tick counters inside existing tick tasks for periodic sweeps (Phase 2: D-05/D-06 using `++sweepCounter % 6000`). The save flush should follow the same pattern for consistency.
- The user wants thoroughness — all three stores should be audited even though PetStore/QuestStore don't have the per-mutation save bug.
- Phase 1 established that WARNING level is for operational issues and SEVERE for I/O failures (D-01/D-02). File write failures in the async path should log at SEVERE, matching the existing `save()` catch blocks.
- The atomic write pattern should handle both Linux (rename is atomic) and Windows (may need `REPLACE_EXISTING` fallback) since the dev environment is Windows.

</specifics>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Codebase analysis
- `.planning/codebase/CONCERNS.md` — Documents per-mutation save as performance concern
- `.planning/codebase/ARCHITECTURE.md` — Manager/Instance/Tick/Store layer responsibilities, data flow

### Prior phases
- `.planning/phases/01-correctness-and-stability/1-CONTEXT.md` — Phase 1 logging conventions (D-01/D-02: WARNING vs SEVERE), lifecycle patterns (D-06/D-07)
- `.planning/phases/02-memory-and-logic-correctness/2-CONTEXT.md` — Phase 2 tick counter pattern (D-05/D-06), sweep interval (6000 ticks)

### Requirements
- `.planning/REQUIREMENTS.md` — Requirements PERS-01, PERS-02, PERS-03, PERF-01

### Key source files
- `src/main/java/net/axther/serverCore/cosmetic/data/CosmeticStore.java` — Full-file save, pending map, PendingCosmetic record
- `src/main/java/net/axther/serverCore/pet/data/PetStore.java` — Simple map save, no pending state
- `src/main/java/net/axther/serverCore/quest/data/QuestStore.java` — Active + completed quest save via manager accessors
- `src/main/java/net/axther/serverCore/cosmetic/CosmeticManager.java` — Lines 79, 97: per-mutation save calls
- `src/main/java/net/axther/serverCore/quest/QuestManager.java` — Lines 327-363: handleExplore with String.split per tick
- `src/main/java/net/axther/serverCore/quest/QuestObjective.java` — Lines 53-55: EXPLORE fromConfig, target string format
- `src/main/java/net/axther/serverCore/ServerCore.java` — Lines 451-509: onDisable() synchronous save pattern

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **CosmeticStore.save(CosmeticManager):** Full snapshot pattern already exists — iterates `manager.getActiveCosmetics()` and `pending` to build YamlConfiguration. Can be split into snapshot (main thread) + write (async thread).
- **PetStore.save():** Simple ownedPets map iteration. Snapshot is trivial — deep-copy the map.
- **QuestStore.save(QuestManager):** Iterates `manager.getAllActiveQuests()` and `manager.getAllCompletedQuests()`. Snapshot requires copying both maps and their contents.
- **QuestObjective.fromConfig() (line 53):** Already has the explore location string and radius extraction. Pre-parse can be added here.
- **Tick counter pattern (Phase 2):** `++sweepCounter >= 6000` with explicit reset, driven from existing tick tasks. Reuse for save flush.

### Established Patterns
- **YamlConfiguration.save(File):** Used by all three stores. This is the synchronous I/O call to replace with async.
- **config.save(file) in try-catch with SEVERE log:** Consistent across all stores. The async variant should maintain this error handling.
- **BukkitRunnable scheduling:** All tick tasks use `runTaskTimer(plugin, 0L, 1L)`. Async one-shot uses `runTaskAsynchronously(plugin, runnable)`.
- **onDisable() cancel-save-destroy order:** Already correct. Just need to ensure save calls the sync path.

### Integration Points
- **CosmeticManager.applyCosmetic() (line 79):** Replace `store.save(this)` with `store.markDirty()`
- **CosmeticManager.removeCosmetics() (line 97):** Replace `store.save(this)` with `store.markDirty()`
- **PetStore.addPet()/removePet():** Add `markDirty()` call (currently no save trigger)
- **QuestManager accept/complete/abandon:** Add `markDirty()` on quest store (currently no save trigger during play)
- **ServerCore.onDisable() (lines 462-496):** Change store save calls to explicit sync variant
- **QuestObjective constructor/fromConfig():** Add ExploreTarget parsing
- **QuestManager.handleExplore() (line 342):** Replace split/parse with pre-parsed ExploreTarget

### Key Files (from scout)

| File | Lines | What's there | What's needed |
|------|-------|-------------|---------------|
| CosmeticStore.java | 30-77 | Sync save with full serialization | Split into snapshot + async write, add dirty flag |
| CosmeticManager.java | 79, 97 | `store.save(this)` per mutation | Replace with `store.markDirty()` |
| PetStore.java | 23-39 | Sync save, no per-mutation trigger | Add dirty flag, async write |
| QuestStore.java | 66-94 | Sync save via manager accessors | Add dirty flag, async write, snapshot deep-copy |
| QuestObjective.java | 9-55 | EXPLORE target as raw String | Add ExploreTarget record + pre-parse |
| QuestManager.java | 342-360 | Per-tick split + parseDouble | Use pre-parsed ExploreTarget |
| ServerCore.java | 451-509 | Sync saves in onDisable | Ensure sync variant is called |

</code_context>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 03-async-persistence-and-performance*
*Context gathered: 2026-03-21*
