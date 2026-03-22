---
phase: 03-async-persistence-and-performance
plan: 01
subsystem: database
tags: [yaml, async, persistence, dirty-flag, atomic-write, thread-safety]

# Dependency graph
requires:
  - phase: 02-memory-and-logic-correctness
    provides: Stable manager collections and lifecycle correctness needed before async I/O refactor
provides:
  - Dirty-flag debounced persistence for CosmeticStore, PetStore, QuestStore
  - Snapshot-then-async write pattern eliminating main-thread YAML saves
  - Atomic file swap (ATOMIC_MOVE with fallback) for crash-safe writes
  - SaveFlushTask running every 6000 ticks to batch store flushes
  - Synchronous shutdown path via saveSync() in onDisable
  - .tmp crash recovery on plugin load for all three stores
affects:
  - phase-04 (any future performance work touching stores or save paths)

# Tech tracking
tech-stack:
  added: [java.nio.file.Files, java.nio.file.StandardCopyOption, java.nio.file.AtomicMoveNotSupportedException]
  patterns:
    - "Dirty-flag pattern: markDirty() on mutation, flushIfDirty() on timer, saveSync() on shutdown"
    - "Snapshot-then-async: buildSnapshot() on main thread, writeSnapshot() dispatched async via BukkitScheduler"
    - "Atomic file swap: write to .tmp then Files.move with ATOMIC_MOVE, fallback to REPLACE_EXISTING"
    - ".tmp crash recovery: check for orphaned .tmp on load, recover or delete"

key-files:
  created:
    - src/main/java/net/axther/serverCore/task/SaveFlushTask.java
  modified:
    - src/main/java/net/axther/serverCore/cosmetic/data/CosmeticStore.java
    - src/main/java/net/axther/serverCore/pet/data/PetStore.java
    - src/main/java/net/axther/serverCore/quest/data/QuestStore.java
    - src/main/java/net/axther/serverCore/cosmetic/CosmeticManager.java
    - src/main/java/net/axther/serverCore/quest/QuestManager.java
    - src/main/java/net/axther/serverCore/ServerCore.java

key-decisions:
  - "buildSnapshot() runs on main thread — Bukkit.getEntity() and live collection access is safe there; writeSnapshot() is the only async portion"
  - "dirty is cleared BEFORE dispatch (not after) — mutations during snapshot window correctly re-set dirty for next flush cycle"
  - "saving flag is volatile — written by async thread in finally block, read on main thread in flushIfDirty(); no lock needed"
  - "PetStore mutation methods (addPet/removePet) call markDirty() internally; QuestManager methods call store.markDirty() externally (store is optional/null-guarded)"
  - "SaveFlushTask accepts null for any store/manager not initialized — null checks inside run() handle partial-init safely"

patterns-established:
  - "Store dirty pattern: boolean dirty + volatile boolean saving + markDirty() + flushIfDirty() + saveSync()"
  - "Async write safety: snapshot must be fully detached before dispatch; YamlConfiguration is read-only after buildSnapshot() returns"

requirements-completed: [PERS-01, PERS-02, PERS-03]

# Metrics
duration: 4min
completed: 2026-03-21
---

# Phase 03 Plan 01: Async Persistence Summary

**Dirty-flag debounced YAML persistence with snapshot-then-async writes and atomic file swap for CosmeticStore, PetStore, and QuestStore — eliminates main-thread disk I/O on every cosmetic apply/remove**

## Performance

- **Duration:** 4 min
- **Started:** 2026-03-21T23:48:57Z
- **Completed:** 2026-03-21T23:52:36Z
- **Tasks:** 2
- **Files modified:** 6 (+ 1 created)

## Accomplishments

- All three stores now use dirty-flag + snapshot-then-async write pattern: mutations call `markDirty()`, a periodic `SaveFlushTask` calls `flushIfDirty()` every 6000 ticks, and `saveSync()` is called in `onDisable()` for guaranteed shutdown write
- Atomic file swap (`Files.move` with `ATOMIC_MOVE`, fallback to `REPLACE_EXISTING`) prevents partial YAML files from crash mid-write
- `.tmp` cleanup on plugin load recovers or discards orphaned temp files from crashed async writes
- `buildSnapshot()` runs on the main thread (safe for Bukkit API), `writeSnapshot()` dispatched async (only touches detached `YamlConfiguration`) — thread safety by construction
- `CosmeticManager.applyCosmetic()` and `removeCosmetics()` no longer trigger synchronous `config.save()` per mutation
- `QuestManager.acceptQuest()`, `completeQuest()`, `abandonQuest()` now mark store dirty so periodic flush captures in-flight quest state

## Task Commits

Each task was committed atomically:

1. **Task 1: Add dirty-flag, snapshot-then-async, and atomic write to all three stores** - `a178dae` (feat)
2. **Task 2: Wire flush task, update mutation call sites, and update onDisable** - `18e6491` (feat)

## Files Created/Modified

- `src/main/java/net/axther/serverCore/task/SaveFlushTask.java` - New BukkitRunnable that flushes all three stores every 6000 ticks (~5 minutes); null-safe for partially-initialized systems
- `src/main/java/net/axther/serverCore/cosmetic/data/CosmeticStore.java` - Added dirty/saving flags, buildSnapshot(), flushIfDirty(), saveSync(), writeSnapshot() with ATOMIC_MOVE, writeSnapshotSync(), .tmp crash recovery in load()
- `src/main/java/net/axther/serverCore/pet/data/PetStore.java` - Same persistence pattern; addPet/removePet call markDirty() internally
- `src/main/java/net/axther/serverCore/quest/data/QuestStore.java` - Same pattern; deep-copies mutable int[] objectiveProgress via Arrays.copyOf in buildSnapshot()
- `src/main/java/net/axther/serverCore/cosmetic/CosmeticManager.java` - Replaced store.save(this) with store.markDirty() in applyCosmetic and removeCosmetics
- `src/main/java/net/axther/serverCore/quest/QuestManager.java` - Added store.markDirty() after acceptQuest, completeQuest, abandonQuest mutations
- `src/main/java/net/axther/serverCore/ServerCore.java` - Added SaveFlushTask field + registration in onEnable; cancel in onDisable; replaced save() with saveSync() for all three stores

## Decisions Made

- `buildSnapshot()` runs on main thread — `Bukkit.getEntity()` and live collection access is safe there; only `writeSnapshot()` is async
- `dirty` is cleared BEFORE dispatch: mutations during the snapshot-build window re-set dirty, which is correct (next flush will pick them up)
- `saving` is `volatile` (not synchronized): written by the async thread in `finally`, read on main thread in `flushIfDirty()`. No lock needed because the pattern is safe: main thread sets `saving=true` before dispatch, async sets `saving=false` in finally
- `PetStore` wires `markDirty()` internally in mutation methods; `QuestManager` wires it at call sites (store is null-guarded) — consistent with how each system owns its mutations
- `SaveFlushTask` constructor accepts null stores/managers; null checks in `run()` handle disabled systems

## Deviations from Plan

None — plan executed exactly as written.

## Issues Encountered

None — both tasks compiled cleanly on first attempt.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- All three data stores are now async-safe with guaranteed shutdown writes
- Plan 03-02 can proceed (any remaining performance work in this phase)
- No blockers

## Self-Check: PASSED

All 8 files confirmed present on disk. Both task commits (a178dae, 18e6491) confirmed in git history.

---
*Phase: 03-async-persistence-and-performance*
*Completed: 2026-03-21*
