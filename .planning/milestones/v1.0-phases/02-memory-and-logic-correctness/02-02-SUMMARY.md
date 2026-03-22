---
phase: 02-memory-and-logic-correctness
plan: 02
subsystem: cosmetic, pet
tags: [memory-safety, standIndex, sweepCounter, audit-sweep, tickAll, BukkitEntity]

# Dependency graph
requires:
  - phase: 01-correctness-and-stability
    provides: CosmeticManager and PetManager with correct null guards and tick cleanup paths
provides:
  - CosmeticManager.sweepCounter field + MEM-02 audit sweep in tickAll()
  - PetManager.sweepCounter field + MEM-02 audit sweep in tickAll()
  - Belt-and-suspenders protection against orphaned standIndex entries from plugin conflicts or unexpected chunk events
affects: [02-memory-and-logic-correctness, future-hardening-phases]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "sweepCounter pattern: increment-then-compare (++counter >= N) with explicit reset avoids modulo overflow edge case"
    - "FINE-level audit logging: eviction noise stays out of INFO stream; diagnosable when operators enable debug logging"

key-files:
  created: []
  modified:
    - src/main/java/net/axther/serverCore/cosmetic/CosmeticManager.java
    - src/main/java/net/axther/serverCore/pet/PetManager.java

key-decisions:
  - "Use ++sweepCounter >= 12000 with explicit reset to 0 rather than % 12000 == 0 — avoids integer overflow producing a negative value that never satisfies the modulo check"
  - "Log evictions at Level.FINE not WARNING — stale entries from plugin conflicts are expected edge cases, not operational errors; WARNING level would cause alarm fatigue"
  - "Bukkit.getEntity(uuid) == null is a reliable gone-check for non-persistent armor stands because setPersistent(false) ensures they are removed on chunk unload, not merely unloaded"

patterns-established:
  - "MEM-02 audit sweep pattern: sweepCounter field + increment-compare-reset block appended at end of tickAll()"

requirements-completed: [MEM-02]

# Metrics
duration: 5min
completed: 2026-03-21
---

# Phase 02 Plan 02: Safety-net Audit Sweep Summary

**10-minute standIndex audit sweep added to CosmeticManager and PetManager using ++sweepCounter >= 12000 pattern with FINE-level eviction logging**

## Performance

- **Duration:** 5 min
- **Started:** 2026-03-21T23:25:00Z
- **Completed:** 2026-03-21T23:30:00Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments

- CosmeticManager.tickAll() now runs a safety-net audit sweep every 12000 ticks (10 minutes) that calls Bukkit.getEntity() for each standIndex entry and evicts entries where the entity is null
- PetManager.tickAll() gains the identical sweep, ensuring both managers have belt-and-suspenders protection against orphaned standIndex entries
- Counter resets to 0 on trigger (not modulo) to avoid integer overflow edge case (D-06 pattern)

## Task Commits

Each task was committed atomically:

1. **Task 1: Add safety-net audit sweep to CosmeticManager** - `80ebbd6` (feat)
2. **Task 2: Add safety-net audit sweep to PetManager** - `f929520` (feat)

**Plan metadata:** (committed with docs commit below)

## Files Created/Modified

- `src/main/java/net/axther/serverCore/cosmetic/CosmeticManager.java` - Added `private int sweepCounter = 0;` field and MEM-02 audit block at end of tickAll()
- `src/main/java/net/axther/serverCore/pet/PetManager.java` - Added `private int sweepCounter = 0;` field and MEM-02 audit block at end of tickAll()

## Decisions Made

- Used `++sweepCounter >= 12000` with explicit `sweepCounter = 0` reset rather than `% 12000 == 0` — the increment-then-compare pattern avoids a subtle integer overflow bug where a large accumulated counter could become negative and never satisfy a modulo check
- Logged evictions at `Level.FINE` (not WARNING or INFO) — stale entries resulting from plugin conflicts or unexpected chunk events are not error conditions; they are expected edge cases that the sweep is designed to silently clean up
- `Bukkit.getEntity(uuid) == null` is the correct gone-check here because armor stands are spawned with `setPersistent(false)`, meaning they are removed (not just unloaded) when their chunk unloads — null reliably means truly gone

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None. Both files already imported `org.bukkit.Bukkit` as the plan noted. `./gradlew compileJava` passed with BUILD SUCCESSFUL on first attempt (one pre-existing deprecation warning in PetConfig.java unrelated to this plan).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- MEM-02 requirement fully satisfied in both CosmeticManager and PetManager
- The sweepCounter pattern is ready to be applied to other managers (EmitterManager, HologramManager) if similar standIndex structures exist in them
- Phase 02 plan 02 complete; ready to proceed to next plan

---
*Phase: 02-memory-and-logic-correctness*
*Completed: 2026-03-21*
