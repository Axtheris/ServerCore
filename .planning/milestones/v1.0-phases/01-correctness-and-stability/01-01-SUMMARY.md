---
phase: 01-correctness-and-stability
plan: 01
subsystem: cosmetic, pet, hologram, particle, timeline
tags: [java, paper-api, tick-loop, null-safety, exception-handling, logging]

# Dependency graph
requires: []
provides:
  - Per-instance try-catch in CosmeticManager.tickAll() and PetManager.tickAll() with Logger injection
  - World-null guards in CosmeticInstance.tick(), PetInstance.tick(), and HologramVisibilityTracker.update()
  - CORR-03 assessment and try-catch protection in EmitterManager.tickAll() and TimelineManager.tickAll()
affects: [02-correctness-and-stability, 03-correctness-and-stability, all plans that build on tick loop reliability]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Canonical null-check order: null -> isDead() -> getWorld() == null (per CORR-02)"
    - "Logger injected via constructor for all managers that need tick-failure logging"
    - "Per-instance try-catch inside removeIf/for loops to isolate tick failures"
    - "CORR-03 comment pattern to document tick loop assessment"

key-files:
  created: []
  modified:
    - src/main/java/net/axther/serverCore/cosmetic/CosmeticManager.java
    - src/main/java/net/axther/serverCore/pet/PetManager.java
    - src/main/java/net/axther/serverCore/ServerCore.java
    - src/main/java/net/axther/serverCore/cosmetic/CosmeticInstance.java
    - src/main/java/net/axther/serverCore/pet/PetInstance.java
    - src/main/java/net/axther/serverCore/hologram/visibility/HologramVisibilityTracker.java
    - src/main/java/net/axther/serverCore/particle/EmitterManager.java
    - src/main/java/net/axther/serverCore/timeline/TimelineManager.java

key-decisions:
  - "Logger injected via constructor (not static Logger.getLogger) for CosmeticManager and PetManager — consistent with Paper plugin conventions"
  - "EmitterManager and TimelineManager use static Logger.getLogger('ServerCore') to avoid constructor refactoring out of plan scope"
  - "WARNING level chosen for tick failures (not SEVERE) — per D-01/D-02 design decisions; SEVERE is reserved for startup/shutdown failures"
  - "Failed instances are removed from active maps and destroyed after catch — no orphan entity leaks from exceptions"

patterns-established:
  - "Canonical null-check order: null -> isDead() -> getWorld() == null for all entity tick() guards"
  - "try { instance.destroy(); } catch (Exception ignored) {} after tick failure catch — prevent destroy() from masking the original failure"

requirements-completed: [CORR-02, CORR-03]

# Metrics
duration: 15min
completed: 2026-03-21
---

# Phase 01 Plan 01: Tick Loop Exception Isolation and World-Null Guards Summary

**Per-instance try-catch added to all five tick loops, Logger injected into CosmeticManager and PetManager, world-null guards in three entity tick() methods preventing NPE on partial world unload**

## Performance

- **Duration:** ~15 min
- **Started:** 2026-03-21T22:45:00Z
- **Completed:** 2026-03-21T22:59:00Z
- **Tasks:** 4
- **Files modified:** 8

## Accomplishments

- CosmeticManager and PetManager now accept `java.util.logging.Logger` via constructor; tick failures log at WARNING naming the mob/owner UUID
- CosmeticInstance.tick(), PetInstance.tick(), and HologramVisibilityTracker.update() all guard against null worlds before entity operations that would NPE
- EmitterManager.tickAll() and TimelineManager.tickAll() assessed for CORR-03 and both given per-instance try-catch with WARNING logging
- All seven targeted files compile clean with zero new errors

## Task Commits

Each task was committed atomically:

1. **Tasks 1+2: Logger injection into CosmeticManager and PetManager, tickAll() try-catch** - `d37aab0` (feat)
2. **Task 3: World-null guards in CosmeticInstance, PetInstance, HologramVisibilityTracker** - `928867b` (fix)
3. **Task 4: CORR-03 try-catch in EmitterManager and TimelineManager** - `eedd648` (fix)

Note: Tasks 1 and 2 were committed together because ServerCore.java is shared between both and the compiler requires both manager constructors to be updated simultaneously.

## Files Created/Modified

- `src/main/java/net/axther/serverCore/cosmetic/CosmeticManager.java` - Added Logger field + constructor, per-instance try-catch in tickAll()
- `src/main/java/net/axther/serverCore/pet/PetManager.java` - Added Logger field, updated constructor signature, per-instance try-catch in tickAll()
- `src/main/java/net/axther/serverCore/ServerCore.java` - Updated CosmeticManager and PetManager construction to pass getLogger()
- `src/main/java/net/axther/serverCore/cosmetic/CosmeticInstance.java` - Extended null-check to include mob.getWorld() == null and stand.getWorld() == null
- `src/main/java/net/axther/serverCore/pet/PetInstance.java` - Extended null-check to include stand.getWorld() == null
- `src/main/java/net/axther/serverCore/hologram/visibility/HologramVisibilityTracker.java` - Added holoLoc == null || holoLoc.getWorld() == null guard before distanceSquared
- `src/main/java/net/axther/serverCore/particle/EmitterManager.java` - Added CORR-03 comment and try-catch around instance.tick() in tickAll()
- `src/main/java/net/axther/serverCore/timeline/TimelineManager.java` - Converted tickAll() to lambda with try-catch; logs WARNING with timeline ID

## Decisions Made

- **Logger injection via constructor**: Consistent with Paper plugin conventions; CosmeticManager and PetManager both receive the plugin logger from `ServerCore.getLogger()` at construction. EmitterManager and TimelineManager use `Logger.getLogger("ServerCore")` to avoid out-of-scope constructor refactoring.
- **WARNING level for tick failures**: Not SEVERE — tick failures are operational events, not startup/shutdown failures. SEVERE is reserved for I/O or initialization failures.
- **Failed instance cleanup after catch**: After catching a tick exception, standIndex is cleaned and `instance.destroy()` is attempted (wrapped in its own try-catch) before returning true (remove). This prevents orphan entities.
- **HologramVisibilityTracker continues to next hologram on null world**: Uses `continue` inside the outer `for (Hologram hologram : manager.getAll())` loop — correct semantics, just skip that hologram.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

Tasks 1 and 2 shared ServerCore.java as a construction site. Since the compiler errors for both had to be resolved together, a single commit covers both managers plus ServerCore.java. This is documented in the Task Commits section.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- All five tick loops are now exception-isolated. A failing instance no longer aborts the entire loop.
- World-null guards prevent NPE cascades during chunk/world unload.
- CORR-02 and CORR-03 requirements are fully satisfied.
- Ready for Phase 01 Plan 02 execution.

---
*Phase: 01-correctness-and-stability*
*Completed: 2026-03-21*

## Self-Check: PASSED

Files verified present:
- FOUND: src/main/java/net/axther/serverCore/cosmetic/CosmeticManager.java
- FOUND: src/main/java/net/axther/serverCore/pet/PetManager.java
- FOUND: src/main/java/net/axther/serverCore/cosmetic/CosmeticInstance.java
- FOUND: src/main/java/net/axther/serverCore/pet/PetInstance.java
- FOUND: src/main/java/net/axther/serverCore/hologram/visibility/HologramVisibilityTracker.java
- FOUND: src/main/java/net/axther/serverCore/particle/EmitterManager.java
- FOUND: src/main/java/net/axther/serverCore/timeline/TimelineManager.java

Commits verified:
- FOUND: d37aab0
- FOUND: 928867b
- FOUND: eedd648
