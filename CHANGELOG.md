# Changelog

All notable changes to ServerCore are documented in this file.

## [2.4.0] - 2026-03-22

### Debug & Verification Sweep

Comprehensive audit and hardening of all nine systems. 24 requirements addressed across 4 phases.

### Added

- `/servercore debug` command — prints live instance counts for all 9 systems, save-flag state (dirty/clean), soft-dependency hook status, and tick task status
- `servercore.admin.debug` permission node for the debug command
- `DebugContext` record for injecting live system state into the command layer
- `SaveFlushTask` — shared periodic flush task (every 5 minutes) for all three data stores
- `ExploreTarget` record on `QuestObjective` — pre-parsed explore coordinates at config load time
- Optional `permission` field on `HologramAction` — per-action YAML-driven permission gating
- `PetManager.getActivePetOwnerCount()` and `ReactiveManager.getActiveEffectCount()` getters
- `isDirty()` method on all three stores for diagnostic inspection
- Absent soft-dependency INFO logs at startup (ModelEngine, PlaceholderAPI, Vault)
- World-existence validation in `EmitterConfig.loadAll()` — invalid worlds logged and skipped

### Changed

- `CosmeticStore`, `PetStore`, `QuestStore` — dirty-flag debounced saves with snapshot-then-async writes replacing synchronous per-mutation I/O
- Atomic file swap (`Files.move` with `ATOMIC_MOVE` fallback) for all YAML writes — prevents partial file corruption on crash
- `.tmp` file recovery on startup — orphaned temp files from crashed writes are automatically cleaned
- `CosmeticManager.applyCosmetic()` and `removeCosmetics()` now call `store.markDirty()` instead of `store.save()` — no main-thread file I/O during play
- `QuestManager.acceptQuest()`, `completeQuest()`, `abandonQuest()` now mark the quest store dirty for periodic flush
- `PetStore.addPet()` and `removePet()` mark the store dirty internally
- `QuestManager.handleExplore()` uses pre-parsed `ExploreTarget` fields instead of `String.split()` + `Double.parseDouble()` per tick — zero allocation on the tick path
- `HologramAction.parse()` expanded to 3 parameters (type, value, permission)
- `HologramConfig.saveAll()` now persists the permission field — no silent drop on reload
- `ServerCore.onDisable()` cancels `SaveFlushTask` then calls `saveSync()` on all three stores before entity destruction
- `ServerCoreCommand` constructor expanded to accept `DebugContext` — constructed after all systems initialize

### Fixed

- **CORR-02**: All entity tick methods now use canonical null-check order: `null` → `isDead()` → `getWorld() == null` — prevents NPE on partial world unload
- **CORR-03**: Per-instance try-catch in `CosmeticManager`, `PetManager`, `EmitterManager`, and `TimelineManager` `tickAll()` — one bad instance never kills the loop
- **CORR-01**: Verified `CosmeticApplyEvent` fires before armor stand spawn — no orphan entities on cancellation
- **CORR-04**: Verified `QuestManager` null-guard prevents double initialization
- **CORR-05**: Verified `getFirstIncompleteIndex()` is the single source of truth for quest progression
- **CORR-06**: Verified FETCH objectives use on-demand inventory check — consistent across accept/abandon/re-accept
- **LIFE-01**: Fixed `onDisable()` task-cancel ordering for emitter and pet tick tasks — tasks cancelled before data saves
- **LIFE-02**: All `onDisable()` blocks are null-guarded — safe in partial-init state
- **LIFE-03**: Verified all lifecycle listeners handle double-fire safely
- **LIFE-04**: Verified synchronous data flush in `onDisable()` — no data loss on shutdown
- **MEM-01**: Hologram cooldown map eviction every 6000 ticks via `sweepCooldowns()` — expired entries removed, map no longer grows unbounded
- **MEM-02**: StandIndex safety-net audit sweep every 12000 ticks in `CosmeticManager` and `PetManager` — catches edge-case orphan entries
- **MEM-03**: Verified world-null guard in `HologramVisibilityTracker` — no NPE during world unload
- **CONF-01**: NPC view-distance validated to [1-256] range, clamped with WARNING log
- **CONF-02**: NPC skin texture and signature validated as proper Base64 at load time — malformed data rejected with WARNING naming the NPC
- **CONF-03**: Hologram view-distance validated to [1.0-512.0] range, clamped with WARNING log
- **CONF-04**: Centralized soft-dependency detection with INFO-level logging for each present/absent dependency

### Security

- **SEC-01**: Hologram actions support optional `permission` field — players without permission are silently skipped, other actions still execute
- **SEC-02**: NPC skin Base64 validation rejects malformed texture/signature before PacketEvents sends packets to clients

### Performance

- **PERS-01**: Data stores use dirty-flag with periodic batch write (every 5 minutes) instead of saving on every mutation
- **PERS-02**: Async saves use snapshot-then-async pattern — main thread only does in-memory work
- **PERS-03**: `onDisable()` synchronous flush guarantees all pending data is written before shutdown
- **PERF-01**: Quest explore target coordinates pre-parsed at config load time — eliminates per-tick `String.split()` and `Double.parseDouble()` allocations

### Known Issues

- Quest/NPC shutdown ordering in `onDisable()`: `questStore.saveSync()` executes before `npcTickTask.cancel()` — low risk, no quest tick task exists
- Debug command does not expose MEM-02 sweep counter values or last-sweep timestamps

## [2.3.0] - 2026-03-05

### API & GUI Framework Expansion
- YAML-driven custom menus with click actions and per-player visibility
- Dynamic/animated items with material cycling
- Built-in Quest Journal, Hologram Manager, and NPC Browser GUIs
- Fluent builder API for holograms, emitters, and menus
- 7 new API events including cancellable HologramClickEvent

## [2.2.0] - 2026-03-05

### Hologram System Expansion
- Billboard, text-shadow, background, line-width, see-through, alignment, view-range styling
- PlaceholderAPI dynamic content with configurable update intervals
- Conditional per-player visibility (permission, world, quest, placeholder conditions)

## [2.1.0] - 2026-03-05

### Quest System Expansion
- 8 new objective types: CRAFT, MINE, PLACE, FISH, BREED, SMELT, EXPLORE, INTERACT
- 3 new reward types: MONEY (Vault), PERMISSION, PET
- Quest prerequisites and permission-gated acceptance

## [2.0.0] - 2026-03-03

### Initial Release
- Nine-system server enhancement platform
- Cosmetics, emitters, pets, holograms, NPCs, quests, timelines, reactive cosmetics, GUIs
- Two-tier cosmetic profile system (Java + YAML)
- Public API with ServerCoreAPI and custom events
- Optional soft-dependency hooks (PacketEvents, PlaceholderAPI, ModelEngine, Vault)
