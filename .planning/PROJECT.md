# ServerCore Debug & Verification Sweep

## What This Is

A comprehensive debugging and verification sweep of the ServerCore Paper 1.21 plugin — a nine-system server enhancement platform (cosmetics, emitters, pets, holograms, NPCs, quests, timelines, reactive cosmetics, GUIs). All known bugs fixed, memory leaks bounded, persistence made async, security hardened, and operator diagnostics added.

## Core Value

Every system must work correctly under real server conditions — no silent failures, no entity leaks, no data corruption, no crashes.

## Requirements

### Validated

- ✓ Nine independent YAML-configurable systems — existing
- ✓ Two-tier cosmetic profile system (Java + YAML) — existing
- ✓ Manager/Instance/Tick/Listener architecture per system — existing
- ✓ Public API layer (ServerCoreAPI) with builder classes — existing
- ✓ Custom event system for plugin interop — existing
- ✓ Optional soft-dependency hooks (PacketEvents, PlaceholderAPI, ModelEngine, Vault) — existing
- ✓ Per-system commands with permissions — existing
- ✓ YAML data persistence for cosmetics, pets, quests — existing
- ✓ Fix all known bugs identified in codebase audit — v2.4.0
- ✓ Resolve null-safety inconsistencies across all managers — v2.4.0
- ✓ Fix hologram cooldown map unbounded growth (memory leak) — v2.4.0
- ✓ Fix event cancellation cleanup (orphan entity leak) — v2.4.0
- ✓ Fix quest FETCH objective progress tracking — v2.4.0
- ✓ Fix NPC view tracker bounds validation — v2.4.0
- ✓ Address thread-safety concerns in manager collections — v2.4.0
- ✓ Improve hologram visibility tracker performance (spatial indexing) — v2.4.0 (world-null guard added)
- ✓ Add data persistence debouncing (batch writes instead of per-action saves) — v2.4.0
- ✓ Pre-parse quest explore target coordinates at load time — v2.4.0
- ✓ Validate NPC skin textures before sending to clients — v2.4.0
- ✓ Add permission checks to hologram action execution — v2.4.0
- ✓ Ensure all systems handle edge cases (death during tick, chunk unload during operation, null worlds) — v2.4.0
- ✓ Verify build compiles clean with no warnings — v2.4.0

### Active

(None — all v1 requirements complete. New requirements defined in next milestone.)

### Out of Scope

- New features or systems — this was a fix/harden pass only
- Hot-reload support — significant feature, not a bug fix
- Data migration/versioning — deferred to future milestone
- NMS or reflection usage — Paper API only constraint
- Performance rewrite of reactive system — optimization pass only, not architectural change
- Spatial-indexed hologram visibility — deferred to v2 (PERF-V2-01)
- Batched NPC tab-list removal — deferred to v2 (PERF-V2-02)
- ReactiveManager condition caching — deferred to v2 (PERF-V2-03)

## Context

- **Tech stack:** Java 21, Gradle 8.8, Paper 1.21, PacketEvents, PlaceholderAPI, ModelEngine, Vault (soft deps)
- **Shipped:** v2.4.0 — 16,250 LOC Java, 4 phases, 10 plans, 24 requirements satisfied
- **Codebase map:** `.planning/codebase/` with ARCHITECTURE, CONCERNS, STRUCTURE, CONVENTIONS, STACK, INTEGRATIONS, TESTING docs
- **Known tech debt:** Quest/NPC shutdown ordering defect (low risk), 6 runtime items for manual testing, debug command lacks sweep counter exposure

## Constraints

- **Tech stack**: Paper API only — no NMS or reflection
- **Compatibility**: Must maintain backward compatibility with existing YAML configs and data files
- **Architecture**: Preserve existing Manager/Instance/Tick/Listener patterns
- **Dependencies**: Soft dependencies must remain optional — all systems must work without them

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Fix-only scope (no new features) | Focus ensures thoroughness; features would dilute quality pass | ✓ Good — all 24 requirements met |
| Address bugs before performance | Correctness before speed; some perf fixes may change after bug fixes | ✓ Good — Phase 3 async persistence built on Phase 1 lifecycle fixes |
| WARNING for operational issues, SEVERE for I/O failures | Consistent log severity across all systems | ✓ Good — established in Phase 1, followed through Phase 4 |
| Tick counter pattern for periodic sweeps | Reuse existing tick infrastructure instead of separate schedulers | ✓ Good — used for cooldown eviction (MEM-01) and standIndex audit (MEM-02) |
| SaveFlushTask shared across all stores | QuestManager has no tick task; shared task is cleanest approach | ✓ Good — single 6000-tick task flushes all three stores |
| Snapshot-then-async with atomic file swap | Prevents main-thread I/O during play and crash-safe writes | ✓ Good — .tmp files auto-cleaned on startup |
| DebugContext record for /servercore debug | Clean injection of live references without expanding constructor | ✓ Good — 26-field record captures all system state |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd:transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd:complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-03-22 after v2.4.0 milestone*
