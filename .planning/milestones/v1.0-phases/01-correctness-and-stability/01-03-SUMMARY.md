---
phase: 01-correctness-and-stability
plan: 03
subsystem: lifecycle
tags: [correctness, lifecycle, idempotency, shutdown, comments, documentation]
dependency_graph:
  requires: []
  provides: [CORR-01, CORR-04, LIFE-01, LIFE-02, LIFE-03, LIFE-04]
  affects:
    - src/main/java/net/axther/serverCore/ServerCore.java
    - src/main/java/net/axther/serverCore/cosmetic/CosmeticManager.java
    - src/main/java/net/axther/serverCore/cosmetic/listener/CosmeticLifecycleListener.java
    - src/main/java/net/axther/serverCore/pet/listener/PetLifecycleListener.java
    - src/main/java/net/axther/serverCore/hologram/listener/HologramLifecycleListener.java
    - src/main/java/net/axther/serverCore/particle/listener/EmitterLifecycleListener.java
tech_stack:
  added: []
  patterns:
    - "cancel-before-save-before-destroy ordering in onDisable()"
    - "hasCosmetics/isPetStand/isSpawned idempotency guards"
key_files:
  created: []
  modified:
    - src/main/java/net/axther/serverCore/ServerCore.java
    - src/main/java/net/axther/serverCore/cosmetic/CosmeticManager.java
    - src/main/java/net/axther/serverCore/cosmetic/listener/CosmeticLifecycleListener.java
    - src/main/java/net/axther/serverCore/pet/listener/PetLifecycleListener.java
    - src/main/java/net/axther/serverCore/hologram/listener/HologramLifecycleListener.java
    - src/main/java/net/axther/serverCore/particle/listener/EmitterLifecycleListener.java
decisions:
  - "CORR-01/CORR-04 documentation comments added inline with code to serve as permanent invariant markers — not in external docs"
  - "Hologram block in onDisable() has save-before-cancel order but was out of plan scope — not fixed here, left for future cleanup"
metrics:
  duration: "~8 minutes"
  completed: "2026-03-21T22:49:31Z"
  tasks_completed: 3
  files_modified: 6
---

# Phase 01 Plan 03: Lifecycle Correctness Documentation and Fix Summary

**One-liner:** Fixed emitter and pet tick task cancellation order in onDisable() and added CORR-01/CORR-04/LIFE-01/LIFE-02/LIFE-03/LIFE-04 verification comments across all lifecycle-sensitive files.

## What Was Done

Three tasks addressing lifecycle correctness in the ServerCore plugin:

1. **CORR-01 and CORR-04 verification comments** — Documented the already-correct CosmeticApplyEvent ordering (event fired before stand spawn) and QuestManager null-guard (prevents double-initialization when NPC system pre-creates QuestManager). Added INFO log to the questManager standalone initialization path.

2. **LIFE-02 fix in onDisable()** — Fixed the ordering of two blocks where tick task cancellation happened AFTER data save (wrong order). The correct order is: cancel task, then save, then destroyAll. Two systems had this reversed:
   - Emitter block: moved `emitterTickTask.cancel()` before `emitterConfig.saveAll()`
   - Pet block: moved `petTickTask.cancel()` before `petStore.save()`

   Also added a LIFE-01/LIFE-04/LIFE-02 comment block at the start of `onDisable()` documenting the invariants.

3. **LIFE-03 idempotency audit** — Read all four lifecycle listener files and added LIFE-03 comments confirming idempotency:
   - CosmeticLifecycleListener: `hasCosmetics()` guard verified on both `onEntityDeath` and `onEntitiesUnload`
   - PetLifecycleListener: `dismissAll()` confirmed idempotent (returns early on `activePets.remove()` null); `isPetStand()` guard confirmed on `onEntitiesUnload`
   - HologramLifecycleListener: `isSpawned()` guard confirmed on `onEntitiesUnload` preventing double-despawn
   - EmitterLifecycleListener: Block events are inherently unique per block; no entity lifecycle risk

## Commits

| Task | Commit | Message |
|------|--------|---------|
| Task 1 (CORR-01) | d37aab0 | feat(01-01): inject Logger into CosmeticManager (CORR-01 comment added by parallel agent) |
| Task 1 (CORR-04) | 69592c7 | feat(01-02): NPC view-distance bounds (CORR-04 comment added by parallel agent) |
| Task 2 | e9ae302 | fix(01-03): fix onDisable() tick task ordering and add LIFE-01/LIFE-04/LIFE-02 comments |
| Task 3 | 90bcd23 | docs(01-03): add LIFE-03 idempotency comments to all lifecycle listeners |

## Verification

All success criteria met:

- `./gradlew compileJava` exits 0 (5 pre-existing deprecation warnings, no errors)
- `CosmeticManager.applyCosmetic()` contains "CORR-01 VERIFIED: Event is fired BEFORE stand spawn"
- `ServerCore.onDisable()` contains "LIFE-01 VERIFIED" and "LIFE-04 VERIFIED" comment block
- `ServerCore.onDisable()` emitter block: `emitterTickTask.cancel()` (line 468) appears before `emitterConfig.saveAll()` (line 471)
- `ServerCore.onDisable()` pet block: `petTickTask.cancel()` (line 477) appears before `petStore.save()` (line 480)
- `ServerCore.java` questManager block contains "CORR-04:" comment and `getLogger().info("Initializing QuestManager (standalone")`
- All four lifecycle listener files contain "LIFE-03" comments

## Deviations from Plan

### Task 1 — CORR-01 and CORR-04 Already Committed by Parallel Agents

**Found during:** Task 1 start
**Issue:** When this executor began, CORR-01 (CosmeticManager comment) and CORR-04 (QuestManager null-guard comment + logger) were already in HEAD. Parallel agents executing 01-01 and 01-02 had added these comments as part of their own scope. The CORR-04 commit (69592c7) added the comment but without the INFO log; it is present in HEAD with the logger from the working copy snapshot this executor applied.
**Resolution:** Verified comments are present in HEAD, confirmed build passes. No duplicate work done. Task 1 commits point to the parallel agent commits for traceability.

### Hologram Block Order Not Fixed (Out of Scope)

**Found during:** Task 2 review
**Issue:** The hologram block in `onDisable()` also has `hologramConfig.saveAll()` before `hologramTickTask.cancel()` (wrong order). However, the plan explicitly scoped Task 2 to emitter and pet blocks only.
**Resolution:** Not fixed. Noted here for future cleanup.

## Known Stubs

None. This plan added comments and fixed ordering only — no UI, data, or API stubs introduced.

## Self-Check: PASSED

- `e9ae302` exists in git log — CONFIRMED
- `90bcd23` exists in git log — CONFIRMED
- CORR-01 in CosmeticManager.java line 47 — CONFIRMED
- LIFE-01 VERIFIED in ServerCore.java line 451 — CONFIRMED
- CORR-04 in ServerCore.java line 297 — CONFIRMED
- LIFE-03 in CosmeticLifecycleListener.java line 31 — CONFIRMED
- LIFE-03 in PetLifecycleListener.java lines 30, 36, 43 — CONFIRMED
- LIFE-03 in HologramLifecycleListener.java line 43 — CONFIRMED
- LIFE-03 in EmitterLifecycleListener.java line 15 — CONFIRMED
