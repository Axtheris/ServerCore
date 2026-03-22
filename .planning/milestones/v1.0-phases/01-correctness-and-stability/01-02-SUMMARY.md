---
phase: 01-correctness-and-stability
plan: 02
subsystem: config-validation
tags: [config, validation, logging, soft-dependencies, emitter]
dependency_graph:
  requires: []
  provides: [CONF-01, CONF-03, CONF-04, D-05]
  affects: [ServerCore.onEnable, EmitterConfig.loadAll]
tech_stack:
  added: []
  patterns: [bounds-validation, warn-and-clamp, absent-dependency-info-log, world-existence-guard]
key_files:
  created: []
  modified:
    - src/main/java/net/axther/serverCore/ServerCore.java
    - src/main/java/net/axther/serverCore/particle/config/EmitterConfig.java
decisions:
  - "Clamping range for NPC view-distance is [1-256] with default 48 (per D-03/D-04)"
  - "Clamping range for hologram view-distance is [1.0-512.0] with default 48.0 (per D-03)"
  - "Absent soft-dep logs at INFO (not WARNING) per D-08 — only PacketEvents absent is WARNING"
  - "[ServerCore] prefix added to new logs for operator log grep-ability"
  - "EmitterConfig world-existence check uses continue inside try-block matching HologramConfig pattern"
metrics:
  duration: "~12 minutes"
  completed: "2026-03-21"
  tasks: 4
  files_modified: 2
---

# Phase 01 Plan 02: Config Validation and Absent-Dependency Logging Summary

Config validation with warn-and-clamp for view-distances, absent-dependency INFO logs for three soft deps, and world-existence guard in EmitterConfig matching the hologram/NPC pattern.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | NPC view-distance bounds validation (CONF-01) | 69592c7 | ServerCore.java |
| 2 | Hologram view-distance bounds validation (CONF-03) | bf19773 | ServerCore.java |
| 3 | Absent-dependency INFO logs for ModelEngine, PlaceholderAPI, Vault (CONF-04) | d687ad1 | ServerCore.java |
| 4 | World-existence validation in EmitterConfig.loadAll() (D-05) | 83c256c | EmitterConfig.java |

## What Was Built

### Task 1: NPC View-Distance Bounds Validation (CONF-01)

In `ServerCore.onEnable()`, the NPC system block now validates the `systems.npcs.view-distance` value before using it. Values outside [1-256] produce a WARNING log naming the key and the bad value, and the validated value is clamped to 48. The `initNpcPacketSystem(viewDistance)` call is unchanged.

### Task 2: Hologram View-Distance Bounds Validation (CONF-03)

In `ServerCore.onEnable()`, the hologram system block now validates `systems.holograms.view-distance`. Values outside [1.0-512.0] produce a WARNING log with the bad value, clamped to 48.0. The `HologramVisibilityTracker` constructor call is unchanged.

### Task 3: Absent-Dependency INFO Logs (CONF-04)

Three `else` branches added to the soft-dependency detection blocks:
- ModelEngine absent: `[ServerCore] ModelEngine not found -- pet models disabled, using head items`
- PlaceholderAPI absent: `[ServerCore] PlaceholderAPI not found -- placeholders disabled`
- Vault absent: `[ServerCore] Vault not found -- economy rewards disabled`

All three log at INFO level. The PacketEvents absent block retains its WARNING level (unchanged per D-08). Present-dependency logs updated to use `[ServerCore]` prefix for consistency.

### Task 4: EmitterConfig World-Existence Validation (D-05)

Added `import org.bukkit.Bukkit` to `EmitterConfig.java` and inserted a world-existence check immediately after `worldName` is read in `loadAll()`. Unknown worlds produce a WARNING and `continue`, skipping `EmitterInstance` construction and `manager.registerExisting()`. This matches the pattern already used in `HologramConfig.loadAll()` and `NPCConfig.loadNPC()`.

## Verification Results

All checks from the plan's `<verification>` block passed:
- `./gradlew compileJava` exits 0 (clean build confirmed)
- `rawViewDistance` found in ServerCore.java
- `rawHoloViewDistance` found in ServerCore.java
- `out of bounds [1-256]` found in ServerCore.java
- `ModelEngine not found` found in ServerCore.java
- `PlaceholderAPI not found` found in ServerCore.java
- `Vault not found` found in ServerCore.java
- `unknown world` found in EmitterConfig.java
- `Bukkit.getWorld` found in EmitterConfig.java

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocker] Stale VaultHook.class prevented incremental compilation**

- **Found during:** Task 1 verification
- **Issue:** Incremental `./gradlew compileJava` failed with "bad class file: VaultHook.class — NoSuchFileException". The class file existed in the build output but the source had moved.
- **Fix:** Ran `./gradlew clean compileJava` to clear stale artifacts. Clean build succeeded immediately.
- **Files modified:** None (build artifact cleanup only)
- **Commit:** None (no source change)

**2. [Observation] Linter updated CosmeticManager and PetManager constructor calls**

- **Found during:** Execution
- **Issue:** A linter auto-updated `new CosmeticManager()` to `new CosmeticManager(getLogger())` and kept `new PetManager(megEnabled, getLogger())` — matching constructor signatures already changed in a parallel plan (01-01).
- **Fix:** No action needed — changes were already correct per the updated constructors.

## Known Stubs

None — all changes are validation/logging additions. No data flows are stubbed.

## Self-Check: PASSED

- [x] ServerCore.java modified and committed (69592c7, bf19773, d687ad1)
- [x] EmitterConfig.java modified and committed (83c256c)
- [x] All four commits confirmed in `git log`
- [x] Build compiles clean with `./gradlew compileJava`
