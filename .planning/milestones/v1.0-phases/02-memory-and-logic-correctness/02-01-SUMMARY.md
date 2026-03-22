---
phase: 02-memory-and-logic-correctness
plan: 01
subsystem: hologram
tags: [memory-leak, cooldown-map, tick-task, eviction, MEM-01]

# Dependency graph
requires:
  - phase: 01-correctness-and-stability
    provides: Hologram system listeners and tick infrastructure in place
provides:
  - sweepCooldowns(int tickCount) method on HologramInteractListener that evicts expired entries
  - hologramInteractListener stored as a ServerCore field (not a local variable)
  - HologramTickTask wired to call sweepCooldowns() every 6000 ticks (~5 minutes)
affects: [02-memory-and-logic-correctness, future hologram plans]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Tick-driven eviction: pass tickCount from task to listener; guard with tickCount % N == 0 to amortize cost"
    - "Listener stored as plugin field when the task needs to call back into it — avoids anonymous local variable anti-pattern"

key-files:
  created: []
  modified:
    - src/main/java/net/axther/serverCore/hologram/listener/HologramInteractListener.java
    - src/main/java/net/axther/serverCore/hologram/task/HologramTickTask.java
    - src/main/java/net/axther/serverCore/ServerCore.java

key-decisions:
  - "sweepCooldowns guard uses tickCount % 6000 (not a separate counter in the listener) — caller provides the tick clock, listener stays stateless"
  - "interactListener null-check in HologramTickTask.run() mirrors the existing getVisibilityTracker() != null pattern for defensive consistency"
  - "Field type uses fully-qualified name in ServerCore.java to avoid adding a new import line — consistent with existing hologram visibility tracker pattern in the same file"

patterns-established:
  - "MEM-01 pattern: periodic map eviction via removeIf on entrySet, driven by tickCount modulo, called from the system's existing tick task"

requirements-completed: [MEM-01]

# Metrics
duration: 1min
completed: 2026-03-21
---

# Phase 02 Plan 01: Hologram Cooldown Map Eviction Summary

**Bounded hologram cooldown map via sweepCooldowns() called every 6000 ticks — HashMap entries evicted by removeIf on expiry timestamp, wired through HologramTickTask**

## Performance

- **Duration:** ~1 min
- **Started:** 2026-03-21T23:20:02Z
- **Completed:** 2026-03-21T23:21:09Z
- **Tasks:** 2 of 2
- **Files modified:** 3

## Accomplishments

- Added `sweepCooldowns(int tickCount)` to HologramInteractListener with MEM-01 comment; uses `cooldowns.entrySet().removeIf(e -> now > e.getValue())` — single-line, allocation-free
- Promoted `interactListener` from a local variable to a private field `hologramInteractListener` in ServerCore so it can be passed to HologramTickTask
- Wired HologramTickTask to accept and call `interactListener.sweepCooldowns(tickCount)` every tick — the modulo guard inside the method limits actual work to once per ~5 minutes

## Task Commits

Each task was committed atomically:

1. **Task 1: Add sweepCooldowns() to HologramInteractListener** - `f84053d` (fix)
2. **Task 2: Promote interactListener to field and wire into HologramTickTask** - `c86f8ad` (fix)

**Plan metadata:** `682eaa1` (docs: complete plan)

## Files Created/Modified

- `src/main/java/net/axther/serverCore/hologram/listener/HologramInteractListener.java` — added sweepCooldowns(int tickCount) method with MEM-01 Javadoc comment
- `src/main/java/net/axther/serverCore/hologram/task/HologramTickTask.java` — added HologramInteractListener constructor param and sweep call in run()
- `src/main/java/net/axther/serverCore/ServerCore.java` — added hologramInteractListener field, changed local var to field assignment, passed to HologramTickTask constructor

## Decisions Made

- `sweepCooldowns` takes the tickCount from the caller rather than maintaining its own counter — keeps the listener stateless and the clock authoritative in the task layer
- `interactListener` null guard in HologramTickTask mirrors the `getVisibilityTracker() != null` check directly above it — consistent defensive pattern
- Fully-qualified class name used for the field in ServerCore.java to avoid adding a new import, matching the pre-existing visibility tracker declaration in the same file

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- MEM-01 resolved: hologram cooldown map is now bounded to interactions within the last 5-minute sweep window
- HologramTickTask constructor signature changed — any external code constructing it directly (unlikely given package-private use) would need updating
- Ready for 02-02 (next memory/logic correctness plan)

---
*Phase: 02-memory-and-logic-correctness*
*Completed: 2026-03-21*

## Self-Check: PASSED

- HologramInteractListener.java: FOUND
- HologramTickTask.java: FOUND
- ServerCore.java: FOUND
- 02-01-SUMMARY.md: FOUND
- Commit f84053d: FOUND
- Commit c86f8ad: FOUND
