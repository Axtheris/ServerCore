# ServerCore Debug & Verification Sweep

## What This Is

A comprehensive debugging and verification sweep of the ServerCore Paper 1.21 plugin — a nine-system server enhancement platform (cosmetics, emitters, pets, holograms, NPCs, quests, timelines, reactive cosmetics, GUIs). The goal is to audit every system, fix known bugs, address tech debt, resolve security issues, and ensure all systems are robust and production-ready.

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

### Active

- [ ] Fix all known bugs identified in codebase audit
- [ ] Resolve null-safety inconsistencies across all managers
- [ ] Fix hologram cooldown map unbounded growth (memory leak)
- [ ] Fix event cancellation cleanup (orphan entity leak)
- [ ] Fix quest FETCH objective progress tracking
- [ ] Fix NPC view tracker bounds validation
- [ ] Address thread-safety concerns in manager collections
- [ ] Improve hologram visibility tracker performance (spatial indexing)
- [x] Add data persistence debouncing (batch writes instead of per-action saves) — Validated in Phase 3
- [x] Pre-parse quest explore target coordinates at load time — Validated in Phase 3
- [x] Validate NPC skin textures before sending to clients — Validated in Phase 4
- [x] Add permission checks to hologram action execution — Validated in Phase 4
- [ ] Ensure all systems handle edge cases (death during tick, chunk unload during operation, null worlds)
- [ ] Verify build compiles clean with no warnings

### Out of Scope

- New features or systems — this is a fix/harden pass only
- Hot-reload support — significant feature, not a bug fix
- Data migration/versioning — deferred to future milestone
- NMS or reflection usage — Paper API only constraint
- Performance rewrite of reactive system — optimization pass only, not architectural change

## Context

- **Tech stack:** Java 21, Gradle 8.8, Paper 1.21, PacketEvents, PlaceholderAPI, ModelEngine, Vault (soft deps)
- **Existing codebase audit:** Complete codebase map at `.planning/codebase/` with ARCHITECTURE, CONCERNS, STRUCTURE, CONVENTIONS, STACK, INTEGRATIONS, TESTING docs
- **Known bug count:** 5 documented bugs, 5 performance bottlenecks, 4 security considerations, 5 fragile areas, 4 scaling limits
- **Test coverage:** Minimal — no visible unit tests for core lifecycle, quest progression, concurrent modifications, visibility conditions, or NPC packet rendering

## Constraints

- **Tech stack**: Paper API only — no NMS or reflection
- **Compatibility**: Must maintain backward compatibility with existing YAML configs and data files
- **Architecture**: Preserve existing Manager/Instance/Tick/Listener patterns
- **Dependencies**: Soft dependencies must remain optional — all systems must work without them

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Fix-only scope (no new features) | Focus ensures thoroughness; features would dilute quality pass | — Pending |
| Address bugs before performance | Correctness before speed; some perf fixes may change after bug fixes | — Pending |

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
*Last updated: 2026-03-22 after Phase 4 completion (all phases complete)*
