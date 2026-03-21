---
phase: 01-correctness-and-stability
verified: 2026-03-21T23:59:00Z
status: human_needed
score: 5/5 must-haves verified
re_verification:
  previous_status: gaps_found
  previous_score: 4/5
  gaps_closed:
    - "Server shutdown completes with all data files written and no tick task still running after onDisable() returns — hologram block now cancels before save"
    - "All requirement IDs referenced by phase plans are valid and defined in REQUIREMENTS.md — CONF-05 removed from 01-02-PLAN.md"
  gaps_remaining: []
  regressions: []
human_verification:
  - test: "Set systems.npcs.view-distance: -5 in server config and start the server"
    expected: "Console shows WARNING line containing \"systems.npcs.view-distance value '-5' is out of bounds [1-256], clamping to 48\""
    why_human: "Cannot verify at-runtime log output without running the server"
  - test: "Inject a deliberate exception into CosmeticInstance.tick() (temporarily corrupt a stand UUID) and observe tick behavior"
    expected: "Console shows one WARNING for the failed instance; other cosmetic instances on other mobs continue ticking normally"
    why_human: "Requires live server interaction to trigger and observe concurrent behavior"
---

# Phase 1: Correctness and Stability Verification Report

**Phase Goal:** The plugin does not crash, leak orphan entities, or fail silently under normal server operation
**Verified:** 2026-03-21T23:59:00Z
**Status:** human_needed
**Re-verification:** Yes — after gap closure (previous status: gaps_found, score: 4/5)

## Re-Verification Summary

Two gaps were identified in the initial verification and both have been resolved:

**Gap 1 — Hologram task-cancel ordering (LIFE-01):** `ServerCore.onDisable()` hologram block now reads: `hologramTickTask.cancel()` (line 485-487) BEFORE `hologramConfig.saveAll(hologramManager)` (line 488-490). The correct cancel-then-save-then-destroy ordering is now consistent across all 9 systems. LIFE-01 is fully satisfied.

**Gap 2 — Phantom CONF-05 (data integrity):** `01-02-PLAN.md` frontmatter `requirements` field now reads `[CONF-01, CONF-03, CONF-04]`. CONF-05 has been removed. No CONF-05 string appears anywhere in the plan files (only in the previous VERIFICATION.md which this file replaces). REQUIREMENTS.md does not define CONF-05 and the phase requirement set does not include it — the plans now accurately reflect the work done.

---

## Goal Achievement

### Observable Truths (from ROADMAP.md Success Criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | A cosmetic apply event cancelled by another plugin leaves no armor stand entity in the world | VERIFIED | CosmeticManager.applyCosmetic() line 47: "CORR-01 VERIFIED: Event is fired BEFORE stand spawn" comment. Stand spawn only occurs after `isCancelled()` returns false. |
| 2 | The server console shows no NullPointerException originating from any tick task during normal play | VERIFIED | All 5 tick loops have per-instance try-catch (CORR-03). Entity null-checks follow canonical null -> isDead() -> getWorld() == null order in CosmeticInstance, PetInstance, HologramVisibilityTracker (CORR-02). |
| 3 | Server shutdown completes with all data files written and no tick task still running after onDisable() returns | VERIFIED | All 9 systems follow cancel-then-save-then-destroy order. Hologram block confirmed fixed: hologramTickTask.cancel() at lines 485-487, hologramConfig.saveAll() at lines 488-490. LIFE-01 VERIFIED comment present at onDisable() start. |
| 4 | Config files with out-of-range numeric values or missing soft dependencies produce a WARNING log entry naming the specific key | VERIFIED | NPC view-distance: rawViewDistance validated with "out of bounds [1-256], clamping to 48" warning. Hologram view-distance: rawHoloViewDistance validated with "out of bounds [1.0-512.0], clamping to 48.0". Absent deps logged at INFO: ModelEngine not found, PlaceholderAPI not found, Vault not found (CONF-04). |
| 5 | Reloading or crashing the server in any partial-init state does not throw an exception from onDisable() | VERIFIED | All 9 system blocks in onDisable() guard with null checks before every operation. LIFE-02 comment documents the invariant. No block assumes prior block succeeded. |

**Score:** 5/5 success criteria fully verified

---

## Must-Have Artifact Verification

### Plan 01-01 Artifacts (CORR-02, CORR-03)

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/java/net/axther/serverCore/cosmetic/CosmeticManager.java` | Per-instance try-catch in tickAll() with Logger field | VERIFIED | `private final Logger logger` field at line 23. Constructor accepts Logger at line 25. tickAll() has try-catch inside removeIf lambda at lines 106-118. Catch logs WARNING "CosmeticInstance tick failed for mob UUID". |
| `src/main/java/net/axther/serverCore/pet/PetManager.java` | Per-instance try-catch in tickAll() with Logger field | VERIFIED | `private final Logger logger` field at line 23. Constructor accepts `(boolean, Logger)` at line 25. tickAll() has try-catch inside removeIf at lines 121-133. Catch logs WARNING "PetInstance tick failed for owner UUID". |
| `src/main/java/net/axther/serverCore/cosmetic/CosmeticInstance.java` | World-null guard in tick() | VERIFIED | `mob.getWorld() == null` and `stand.getWorld() == null` in null-check condition. CORR-02 comment present. |
| `src/main/java/net/axther/serverCore/pet/PetInstance.java` | World-null guard in tick() | VERIFIED | `stand.getWorld() == null` in null-check condition. CORR-02 comment present. |
| `src/main/java/net/axther/serverCore/hologram/visibility/HologramVisibilityTracker.java` | World-null guard before distanceSquared | VERIFIED | Line 39: `if (holoLoc == null || holoLoc.getWorld() == null) continue;` before distanceSquared call. CORR-02 comment present. |
| `src/main/java/net/axther/serverCore/particle/EmitterManager.java` | CORR-03 verification comment on tickAll() | VERIFIED | Line 85: "CORR-03 VERIFIED: EmitterInstance.tick() has a world-null guard." Per-instance try-catch with WARNING log on emitter ID. |
| `src/main/java/net/axther/serverCore/timeline/TimelineManager.java` | CORR-03 verification comment on tickAll() | VERIFIED | "CORR-03:" comment present. Per-instance try-catch inside removeIf lambda. Logs WARNING with timeline id. |

### Plan 01-02 Artifacts (CONF-01, CONF-03, CONF-04)

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/java/net/axther/serverCore/ServerCore.java` | View-distance bounds validation + absent-dep INFO logs | VERIFIED | rawHoloViewDistance/rawViewDistance validated with "out of bounds" warning and clamp. ModelEngine/PlaceholderAPI/Vault absent-dep logs at INFO confirmed present. |
| `src/main/java/net/axther/serverCore/particle/config/EmitterConfig.java` | World existence check at loadAll() | VERIFIED | `Bukkit.getWorld(worldName) == null` check with WARNING and continue before EmitterInstance construction. D-05 comment present. |

### Plan 01-03 Artifacts (CORR-01, CORR-04, LIFE-01-04)

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/java/net/axther/serverCore/cosmetic/CosmeticManager.java` | CORR-01 comment on applyCosmetic() | VERIFIED | Line 47: "CORR-01 VERIFIED: Event is fired BEFORE stand spawn" comment present. |
| `src/main/java/net/axther/serverCore/ServerCore.java` | LIFE-01/LIFE-02/LIFE-04 comments + correct task-cancel ordering for all systems | VERIFIED | LIFE-01/LIFE-02/LIFE-04 comments at onDisable() start (lines 451-456). Hologram block fixed: hologramTickTask.cancel() (485-487) before hologramConfig.saveAll() (488-490). Emitter and pet blocks also correct. |
| `src/main/java/net/axther/serverCore/quest/QuestManager.java` | CORR-04 null-guard comment | VERIFIED | ServerCore.java CORR-04 comment and getLogger().info("Initializing QuestManager (standalone") present. |
| `src/main/java/net/axther/serverCore/cosmetic/listener/CosmeticLifecycleListener.java` | LIFE-03 comment | VERIFIED | "LIFE-03 VERIFIED: hasCosmetics() guard" comments present on both event handlers. |
| `src/main/java/net/axther/serverCore/pet/listener/PetLifecycleListener.java` | LIFE-03 comment | VERIFIED | LIFE-03 comments on dismissAll() calls and isPetStand() guard. |
| `src/main/java/net/axther/serverCore/hologram/listener/HologramLifecycleListener.java` | LIFE-03 comment | VERIFIED | "LIFE-03 VERIFIED: isSpawned() guard prevents double-despawn" present. |
| `src/main/java/net/axther/serverCore/particle/listener/EmitterLifecycleListener.java` | LIFE-03 comment | VERIFIED | Class-level "LIFE-03: Emitter system has no entity lifecycle" note present. |

---

## Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| ServerCore.onEnable() cosmetic block | CosmeticManager constructor | new CosmeticManager(getLogger()) | VERIFIED | CosmeticManager line 25: constructor accepts Logger; ServerCore passes getLogger() |
| ServerCore.onEnable() pet block | PetManager constructor | new PetManager(megEnabled, getLogger()) | VERIFIED | PetManager line 25: constructor accepts (boolean, Logger); ServerCore passes both |
| CosmeticManager.tickAll() | instance.tick() catch block | try-catch inside removeIf lambda | VERIFIED | Lines 106-118: try wraps instance.tick(), catch logs "CosmeticInstance tick failed for mob UUID" |
| ServerCore.onEnable() hologram block | HologramVisibilityTracker constructor | validated holoViewDistance double value | VERIFIED | rawHoloViewDistance validated, holoViewDistance passed to HologramVisibilityTracker |
| ServerCore.onEnable() pet block | getLogger().info | absent ModelEngine INFO log | VERIFIED | Else branch logs "ModelEngine not found -- pet models disabled, using head items" |
| ServerCore.onEnable() quest block | getLogger().info | absent Vault INFO log | VERIFIED | Else branch logs "Vault not found -- economy rewards disabled" |
| EmitterConfig.loadAll() per-entry loop | Bukkit.getWorld(worldName) | null check with WARNING and continue | VERIFIED | Null check fires warning and continues before EmitterInstance construction |
| ServerCore.onDisable() hologram block | hologramTickTask.cancel() | cancel before saveAll | VERIFIED | hologramTickTask.cancel() at lines 485-487 precedes hologramConfig.saveAll() at lines 488-490 |

---

## Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|------------|-------------|--------|----------|
| CORR-01 | 01-03-PLAN | Cosmetic armor stand not spawned until after cancel check | SATISFIED | CosmeticManager.applyCosmetic(): CORR-01 VERIFIED comment; stand spawn after isCancelled() check |
| CORR-02 | 01-01-PLAN | Canonical null-check order with getWorld() guard | SATISFIED | CosmeticInstance mob/stand guards, PetInstance stand guard, HologramVisibilityTracker holoLoc guard |
| CORR-03 | 01-01-PLAN | All tickAll() loops use per-instance try-catch | SATISFIED | CosmeticManager, PetManager, EmitterManager, TimelineManager all verified |
| CORR-04 | 01-03-PLAN | QuestManager initialized exactly once | SATISFIED | ServerCore.java null-guard with CORR-04 comment and INFO log |
| LIFE-01 | 01-03-PLAN | All BukkitRunnable tick tasks cancelled before manager state cleared | SATISFIED | All 9 systems verified. Hologram block fixed: cancel (line 485) before save (line 488). |
| LIFE-02 | 01-03-PLAN | onDisable() is safe in partial-init state | SATISFIED | All blocks in onDisable() have null guards. LIFE-02 comment at method start. |
| LIFE-03 | 01-03-PLAN | All lifecycle listeners handle double-fire safely | SATISFIED | All 4 listener files verified idempotent with documented guards. |
| LIFE-04 | 01-03-PLAN | onDisable() performs synchronous final data flush | SATISFIED | cosmeticStore.save(), petStore.save(), questStore.save() all called synchronously. LIFE-04 VERIFIED comment at onDisable() start. |
| CONF-01 | 01-02-PLAN | NPC view-distance validated at load with WARNING | SATISFIED | ServerCore.java: rawViewDistance, "out of bounds [1-256], clamping to 48" warning |
| CONF-03 | 01-02-PLAN | Numeric config bounds validated with logged warnings | SATISFIED | Hologram view-distance: rawHoloViewDistance with bounds check. NPC view-distance: rawViewDistance with bounds check. |
| CONF-04 | 01-02-PLAN | Soft dependency detection with present/absent INFO logs | SATISFIED | ModelEngine, PlaceholderAPI, Vault: both present and absent paths log at INFO level |

All 11 requirements from the phase requirement set are SATISFIED. No orphaned or phantom IDs remain in any plan's requirements frontmatter.

---

## Anti-Patterns Found

No blocker anti-patterns detected. The hologram task-cancel ordering issue that was previously flagged as a blocker has been resolved.

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| — | — | — | — | No blockers or warnings found |

---

## Human Verification Required

### 1. Config Validation WARNING Output

**Test:** Set `systems.npcs.view-distance: -5` in the server config and start the server.
**Expected:** Console shows a WARNING line containing `systems.npcs.view-distance value '-5' is out of bounds [1-256], clamping to 48`.
**Why human:** Cannot verify at-runtime log output without running the server.

### 2. Tick Exception Isolation Behavior

**Test:** Inject a deliberate exception into a CosmeticInstance.tick() call (by temporarily corrupting a stand UUID) and verify only that instance is removed; others continue ticking.
**Expected:** Console shows one WARNING for the failed instance; other cosmetic instances on other mobs continue to tick normally in subsequent server ticks.
**Why human:** Requires live server interaction to trigger and observe concurrent behavior.

---

## Re-Verification Gap Closure Evidence

| Gap (previous) | Fix Applied | Verification |
|----------------|-------------|--------------|
| Hologram tick task cancelled after saveAll() in onDisable() | hologramTickTask.cancel() moved to before hologramConfig.saveAll() | Lines 485-490 of ServerCore.java: cancel (485-487) precedes save (488-490). LIFE-01 is now satisfied for all 9 systems. |
| CONF-05 phantom requirement in 01-02-PLAN.md frontmatter | Removed CONF-05 from requirements field | 01-02-PLAN.md line 11: `requirements: [CONF-01, CONF-03, CONF-04]`. CONF-05 does not appear in any plan file. |

No regressions detected — all previously-passing items remain correct.

---

_Verified: 2026-03-21T23:59:00Z_
_Verifier: Claude (gsd-verifier)_
_Mode: Re-verification after gap closure_
