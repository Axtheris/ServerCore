# Roadmap: ServerCore Debug & Verification Sweep

## Overview

Four phases attack the plugin's production bugs in dependency order: crash-causing correctness bugs and null-safety first, memory leaks and logic errors second, async persistence and performance third, and additive security and observability last. Each phase leaves the plugin in a more stable state than it found it — no phase introduces new architectural changes, only targeted hardening within existing component boundaries.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [x] **Phase 1: Correctness and Stability** - Eliminate crashes, null-dereferences, lifecycle failures, and silent config errors (completed 2026-03-21)
- [ ] **Phase 2: Memory and Logic Correctness** - Bound all unbounded maps; fix quest objective progression to a single source of truth
- [ ] **Phase 3: Async Persistence and Performance** - Debounce saves with snapshot-then-async; eliminate per-tick allocations
- [ ] **Phase 4: Security and Observability** - Enforce hologram action permissions, validate NPC skin Base64, add live debug command

## Phase Details

### Phase 1: Correctness and Stability
**Goal**: The plugin does not crash, leak orphan entities, or fail silently under normal server operation
**Depends on**: Nothing (first phase)
**Requirements**: CORR-01, CORR-02, CORR-03, CORR-04, LIFE-01, LIFE-02, LIFE-03, LIFE-04, CONF-01, CONF-03, CONF-04
**Success Criteria** (what must be TRUE):
  1. A cosmetic apply event cancelled by another plugin leaves no armor stand entity in the world
  2. The server console shows no NullPointerException originating from any tick task during normal play
  3. Server shutdown completes with all data files written and no tick task still running after onDisable() returns
  4. Config files with out-of-range numeric values or missing soft dependencies produce a WARNING log entry naming the specific key — not a silent default
  5. Reloading or crashing the server in any partial-init state does not throw an exception from onDisable()
**Plans**: 3 plans

Plans:
- [x] 01-01-PLAN.md — Tick loop hardening: per-instance try-catch in CosmeticManager/PetManager + world-null guards in tick() methods (CORR-02, CORR-03)
- [x] 01-02-PLAN.md — Config validation and soft-dep logging: NPC/hologram view-distance bounds + absent-dependency INFO logs (CONF-01, CONF-03, CONF-04)
- [x] 01-03-PLAN.md — Lifecycle audit: fix onDisable() task-cancel ordering for emitter/pet + verify and document already-correct patterns (CORR-01, CORR-04, LIFE-01, LIFE-02, LIFE-03, LIFE-04)

### Phase 2: Memory and Logic Correctness
**Goal**: Memory usage stays bounded over server lifetime and quest objective progression is always consistent
**Depends on**: Phase 1
**Requirements**: CORR-05, CORR-06, MEM-01, MEM-02, MEM-03
**Success Criteria** (what must be TRUE):
  1. After 24 hours of uptime the hologram cooldown map size does not grow beyond the count of unique players who interacted in the last eviction window
  2. A player who accepts a quest, collects items, abandons, and re-accepts sees correct FETCH objective progress — not stale progress from the prior accept
  3. Completing a quest objective via any call path in QuestManager advances the same underlying progression state
  4. A hologram visibility tick during world unload does not throw a NullPointerException
**Plans**: 3 plans

Plans:
- [x] 02-01-PLAN.md — Hologram cooldown eviction: sweepCooldowns() on HologramInteractListener, field promotion in ServerCore, wired into HologramTickTask (MEM-01)
- [x] 02-02-PLAN.md — standIndex safety-net audit sweep: sweepCounter + Bukkit.getEntity() check in CosmeticManager and PetManager tickAll() (MEM-02)
- [x] 02-03-PLAN.md — Verification comments: MEM-03 on HologramVisibilityTracker, CORR-05/CORR-06 on QuestManager, CORR-06 on QuestListener (MEM-03, CORR-05, CORR-06)

### Phase 3: Async Persistence and Performance
**Goal**: Data saves never block the main thread during play and quest explore checks allocate no garbage per tick
**Depends on**: Phase 1
**Requirements**: PERS-01, PERS-02, PERS-03, PERF-01
**Success Criteria** (what must be TRUE):
  1. Saving cosmetics, pets, or quest data while players are online does not produce a visible tick spike (no main-thread file I/O during play)
  2. A server shutdown with pending unsaved changes writes all dirty data to disk before the process exits
  3. Quest explore objectives check player proximity without allocating String arrays — per-tick allocation profile is flat for explore-type quests
  4. An async save interrupted by crash or power loss never produces a partially-written YAML file that fails to parse on next startup
**Plans**: 2 plans

Plans:
- [ ] 03-01-PLAN.md — Async persistence: dirty-flag + snapshot-then-async writes for CosmeticStore/PetStore/QuestStore, SaveFlushTask every 6000 ticks, atomic file swap, onDisable sync path (PERS-01, PERS-02, PERS-03)
- [ ] 03-02-PLAN.md — Explore target pre-parsing: ExploreTarget record in QuestObjective, pre-parsed at config load, zero-allocation handleExplore (PERF-01)

### Phase 4: Security and Observability
**Goal**: Hologram actions enforce permissions, NPC skins are validated before packet send, and operators can inspect live plugin state
**Depends on**: Phase 3
**Requirements**: CONF-02, SEC-01, SEC-02, OBS-01
**Success Criteria** (what must be TRUE):
  1. A player without the required permission who clicks a hologram action receives no effect — the action is silently skipped
  2. A YAML NPC skin entry with a malformed Base64 texture field is rejected at load time with a WARNING log naming the NPC — no malformed packet is ever sent to clients
  3. Running `/servercore debug` prints active instance counts for all nine systems, pending save flags, registered soft-dependency hooks, and tick task status — all in one console output
**Plans**: TBD

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → 4

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Correctness and Stability | 3/3 | Complete   | 2026-03-21 |
| 2. Memory and Logic Correctness | 0/3 | Not started | - |
| 3. Async Persistence and Performance | 0/2 | Not started | - |
| 4. Security and Observability | 0/? | Not started | - |
