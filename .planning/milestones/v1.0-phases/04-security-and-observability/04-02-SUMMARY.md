---
phase: 04-security-and-observability
plan: 02
subsystem: observability
tags: [debug-command, permissions, bukkit-runnable, java-record]

# Dependency graph
requires: []
provides:
  - "/servercore debug subcommand with live snapshot of all nine systems"
  - "DebugContext record bundling all manager/store/task/hook references"
  - "PetManager.getActivePetOwnerCount() public getter"
  - "ReactiveManager.getActiveEffectCount() public getter"
  - "servercore.admin.debug permission (default: op)"
affects: [future-observability, ops-tooling]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "DebugContext record pattern: inject all live references at startup for diagnostics"
    - "countOrDisabled<T> generic helper: null-safe count with 'not loaded' fallback"
    - "ServerCoreCommand constructed after all systems init: guarantees DebugContext has live refs"

key-files:
  created:
    - src/main/java/net/axther/serverCore/command/DebugContext.java
  modified:
    - src/main/java/net/axther/serverCore/command/ServerCoreCommand.java
    - src/main/java/net/axther/serverCore/ServerCore.java
    - src/main/java/net/axther/serverCore/pet/PetManager.java
    - src/main/java/net/axther/serverCore/reactive/ReactiveManager.java
    - src/main/resources/plugin.yml

key-decisions:
  - "DebugContext constructed after all systems init — guarantees all manager/store/task references are non-null when the user runs /servercore debug"
  - "ServerCoreCommand construction moved from top of onEnable() to after ServerCoreAPI.init() and saveFlushTask start — this is the only safe position for full DebugContext wiring"
  - "countOrDisabled<T> uses generic Function parameter — avoids cast overhead and keeps printDebug readable without instanceof chains"
  - "storeState uses pattern-matching instanceof — all three stores have isDirty() but share no common interface, so type dispatch is the clean solution without interface changes"

patterns-established:
  - "Null-safe debug output: all nine systems return 'not loaded' when manager is null rather than NPE"
  - "Tab-completion permission-gated: debug subcommand only appears in completions if sender has servercore.admin.debug"

requirements-completed: [OBS-01]

# Metrics
duration: 3min
completed: 2026-03-21
---

# Phase 04 Plan 02: Live Debug Command Summary

**`/servercore debug` operator diagnostic command — prints active instance counts for all nine systems, dirty/clean save state for three stores, present/absent soft-dep hooks, and running/stopped tick task status**

## Performance

- **Duration:** 3 min
- **Started:** 2026-03-21T21:13:10Z
- **Completed:** 2026-03-21T21:16:00Z
- **Tasks:** 2
- **Files modified:** 6

## Accomplishments

- Created `DebugContext` record with 26 fields covering all nine managers, three stores, nine tick tasks, and four soft-dependency boolean flags
- Added `getActivePetOwnerCount()` to `PetManager` and `getActiveEffectCount()` to `ReactiveManager` as the two missing public getters needed by the debug command
- Implemented `printDebug()` in `ServerCoreCommand` with null-safe output for all sections — disabled systems show "not loaded" without NPE
- Permission-gated under `servercore.admin.debug` (default: op), declared in plugin.yml with wildcard inheritance under `servercore.admin.*`
- Tab-completion includes "debug" with permission guard
- Moved `ServerCoreCommand` construction to after all systems are initialized so `DebugContext` captures live references

## Task Commits

Each task was committed atomically:

1. **Task 1: Create DebugContext record and add missing manager getters** - `ae0c0f1` (feat)
2. **Task 2: Implement debug subcommand in ServerCoreCommand, wire DebugContext in ServerCore, add permission to plugin.yml** - `8188615` (feat)

## Files Created/Modified

- `src/main/java/net/axther/serverCore/command/DebugContext.java` - New Java record bundling all live debug references
- `src/main/java/net/axther/serverCore/command/ServerCoreCommand.java` - Expanded constructor, debug subcommand, helper methods, updated tab-complete
- `src/main/java/net/axther/serverCore/ServerCore.java` - DebugContext construction after system init, moved ServerCoreCommand registration
- `src/main/java/net/axther/serverCore/pet/PetManager.java` - Added `getActivePetOwnerCount()`
- `src/main/java/net/axther/serverCore/reactive/ReactiveManager.java` - Added `getActiveEffectCount()`
- `src/main/resources/plugin.yml` - Added `servercore.admin.debug` permission (children + standalone), updated servercore usage line

## Decisions Made

- DebugContext constructed after all system init (after `saveFlushTask.runTaskTimer`) — this is the only position where every manager/store/task field is non-null if the system is enabled
- `ServerCoreCommand` construction moved from top of `onEnable()` to after all systems start — was previously at lines 99-105 before any systems loaded
- Generic `countOrDisabled<T>` helper avoids instanceof chains in `printDebug()` — clean and type-safe
- `storeState()` uses Java 21 pattern-matching instanceof since the three stores share no common `Dirty` interface — extending the store hierarchy would be out of scope for a diagnostics feature

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None - build succeeded first attempt after Task 1 and Task 2. Pre-existing test compilation errors from the parallel 04-01 plan (HologramAction.parse signature update) were already resolved in the working tree.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- OBS-01 complete: operators can now run `/servercore debug` for a live plugin health snapshot
- Phase 04 plan 02 of 2 is now complete
- All debug output is null-safe and handles partial-init scenarios (disabled systems show "not loaded")

---
*Phase: 04-security-and-observability*
*Completed: 2026-03-21*

## Self-Check: PASSED

- DebugContext.java: FOUND
- ServerCoreCommand.java: FOUND
- 04-02-SUMMARY.md: FOUND
- Commit ae0c0f1 (Task 1): FOUND
- Commit 8188615 (Task 2): FOUND
