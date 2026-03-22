---
phase: 03-async-persistence-and-performance
plan: 02
subsystem: quest
tags: [java, performance, allocation-optimization, records, quest-system]

# Dependency graph
requires: []
provides:
  - ExploreTarget record nested in QuestObjective for zero-allocation explore checks
  - Pre-parsed explore target coordinates stored at config load time
  - handleExplore() in QuestManager uses direct field access with no per-tick allocation
affects: [quest, QuestManager, QuestObjective, QuestConfig]

# Tech tracking
tech-stack:
  added: []
  patterns: ["Pre-parse immutable data at config load time into records; use direct field accessors in tick loops"]

key-files:
  created: []
  modified:
    - src/main/java/net/axther/serverCore/quest/QuestObjective.java
    - src/main/java/net/axther/serverCore/quest/QuestManager.java

key-decisions:
  - "ExploreTarget stores world NAME (String) not World reference — worlds may not be loaded at config parse time (D-19)"
  - "IllegalArgumentException catches both NumberFormatException (subclass) and empty worldName from ExploreTarget constructor — single catch is correct Java"
  - "Raw target string preserved in QuestObjective for backward compatibility; exploreTarget used exclusively for tick-path proximity checks"
  - "Malformed explore targets set exploreTarget=null and are silently skipped in handleExplore() — caller (QuestConfig) is the appropriate log site per D-20"

patterns-established:
  - "Pre-parse allocation pattern: parse at fromConfig(), store in immutable record, access via getter in tick loop"
  - "dx*dx style distance-squared calculation preferred over Math.pow() in tick methods to avoid autoboxing"

requirements-completed: [PERF-01]

# Metrics
duration: 3min
completed: 2026-03-21
---

# Phase 3 Plan 2: Quest Explore Target Pre-parse Summary

**Immutable ExploreTarget record in QuestObjective eliminates per-tick String.split() and Double.parseDouble() from handleExplore() by pre-parsing coordinates at config load time**

## Performance

- **Duration:** 3 min
- **Started:** 2026-03-21T23:49:01Z
- **Completed:** 2026-03-21T23:52:28Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments

- Added `public record ExploreTarget(String worldName, double x, double y, double z)` as a nested type in `QuestObjective`
- Pre-parse happens once in `fromConfig()` via `parseExploreTarget()` — malformed locations return null (graceful degradation)
- `handleExplore()` in `QuestManager` now uses `obj.getExploreTarget()` with direct field accessors (`target.x()`, `target.y()`, `target.z()`) — zero allocation per tick
- `Math.pow(delta, 2)` replaced with `dx * dx` multiplication — removes autoboxing overhead in hot path

## Task Commits

Each task was committed atomically:

1. **Task 1: Add ExploreTarget record and pre-parse in QuestObjective.fromConfig()** - `9a604e3` (feat)
2. **Task 2: Update QuestManager.handleExplore() to use pre-parsed ExploreTarget** - `6e9daaa` (perf)

**Plan metadata:** (docs commit follows)

## Files Created/Modified

- `src/main/java/net/axther/serverCore/quest/QuestObjective.java` — Added ExploreTarget record, parseExploreTarget() static method, exploreTarget field, 6-param constructor chain, getExploreTarget() getter
- `src/main/java/net/axther/serverCore/quest/QuestManager.java` — Replaced split/parseDouble/Math.pow block in handleExplore() with pre-parsed ExploreTarget accessor calls

## Decisions Made

- `IllegalArgumentException` used as single catch (not `NumberFormatException | IllegalArgumentException`) because `NumberFormatException` is a subclass of `IllegalArgumentException` — Java forbids related types in multi-catch
- World stored as String name at parse time per D-19; `location.getWorld().getName()` comparison at tick time remains correct
- Raw target string preserved for display/serialization compatibility; `exploreTarget` is the tick-path field
- Malformed explore targets produce `exploreTarget=null` and are skipped without logging at parse time — logging responsibility falls to QuestConfig per D-20

## Deviations from Plan

**1. [Rule 1 - Bug] Fixed multi-catch error in parseExploreTarget()**

- **Found during:** Task 1 (compilation verification)
- **Issue:** Plan template used `catch (NumberFormatException | IllegalArgumentException e)` but `NumberFormatException extends IllegalArgumentException` — Java prohibits multi-catch with related types
- **Fix:** Changed to `catch (IllegalArgumentException e)` which catches both `NumberFormatException` and the `IllegalArgumentException` thrown by `ExploreTarget` compact constructor for empty worldName
- **Files modified:** `src/main/java/net/axther/serverCore/quest/QuestObjective.java`
- **Verification:** `./gradlew compileJava` succeeded without QuestObjective errors
- **Committed in:** `9a604e3` (Task 1 commit)

---

**Total deviations:** 1 auto-fixed (Rule 1 — bug in plan template)
**Impact on plan:** Fix required for compilation; behavior is identical to intent. No scope creep.

## Issues Encountered

- Pre-existing compilation errors in `CosmeticManager.java`, `PetStore.java`, `CosmeticStore.java`, and `ServerCore.java` (from phase 03-01 changes) were present before and after this plan's changes. Confirmed pre-existing via `git stash` isolation test. Not introduced by this plan.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- PERF-01 complete: explore objective proximity checks are now allocation-free per tick
- QuestObjective.ExploreTarget is available as a public API type for any future quest system extensions
- No blockers for remaining phase 03 work

---
*Phase: 03-async-persistence-and-performance*
*Completed: 2026-03-21*

## Self-Check: PASSED

- FOUND: src/main/java/net/axther/serverCore/quest/QuestObjective.java
- FOUND: src/main/java/net/axther/serverCore/quest/QuestManager.java
- FOUND: .planning/phases/03-async-persistence-and-performance/03-02-SUMMARY.md
- FOUND commit: 9a604e3 (Task 1)
- FOUND commit: 6e9daaa (Task 2)
