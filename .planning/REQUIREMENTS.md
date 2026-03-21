# Requirements: ServerCore Debug & Verification Sweep

**Defined:** 2026-03-21
**Core Value:** Every system must work correctly under real server conditions — no silent failures, no entity leaks, no data corruption, no crashes.

## v1 Requirements

Requirements for this milestone. Each maps to roadmap phases.

### Correctness

- [x] **CORR-01**: Cosmetic armor stand is not spawned until after CosmeticApplyEvent cancellation check passes — no orphan entity leaks on event cancellation
- [x] **CORR-02**: All entity access follows canonical null-check order: `entity != null` → `entity.isDead()` → `entity.getWorld() != null` — no NPE from reversed checks
- [x] **CORR-03**: All tick task `tickAll()` loops use try-catch per instance so one failed instance does not skip cleanup of subsequent instances
- [x] **CORR-04**: QuestManager is initialized exactly once regardless of NPC system enabled/disabled state — no double-initialization overwrite
- [x] **CORR-05**: Quest `getFirstIncompleteIndex()` logic extracted to single method on QuestProgress — single source of truth for objective progression
- [x] **CORR-06**: Quest FETCH objective tracks consistent progress — inventory count at completion check time, not stale cached values from a prior accept

### Lifecycle

- [x] **LIFE-01**: All BukkitRunnable tick tasks store task references and are cancelled in `onDisable()` before manager state is cleared
- [x] **LIFE-02**: `onDisable()` cleanup is idempotent — safe to call in partial-init state, on crash, or after reload
- [x] **LIFE-03**: All lifecycle listeners (death, chunk unload) handle double-fire safely — removing an already-removed entity does not throw or corrupt state
- [x] **LIFE-04**: `onDisable()` performs synchronous final data flush for all stores (cosmetics, pets, quests) — no data loss on shutdown

### Memory

- [x] **MEM-01**: HologramInteractListener cooldown map has periodic eviction — expired entries are removed, map does not grow unbounded
- [x] **MEM-02**: Cosmetic and pet stand UUID index maps purge entries for dead/despawned entities — no unbounded growth over server lifetime
- [x] **MEM-03**: Hologram visibility tracker guards against null world before distance calculations — no NPE on world unload

### Config Validation

- [x] **CONF-01**: NPC view-distance config value is validated to minimum 1 at load time — invalid values logged with WARNING and clamped to default
- [ ] **CONF-02**: NPC skin texture and signature fields are validated as proper Base64 format at load time — malformed values logged and skin skipped
- [x] **CONF-03**: Config values with numeric bounds (distances, intervals, counts) are validated at load time with logged warnings for out-of-range values
- [x] **CONF-04**: Soft dependency detection centralized in single DependencyChecker pass in `onEnable()` — each present/absent dependency logged at INFO level

### Persistence

- [x] **PERS-01**: Data stores (cosmetics, pets, quests) use dirty-flag with periodic batch write (debounced) instead of saving on every mutation
- [x] **PERS-02**: Async saves use snapshot-then-async pattern — main thread snapshots data, async thread serializes to disk — no ConcurrentModificationException
- [x] **PERS-03**: `onDisable()` synchronous flush guarantees all pending dirty data is written before process exits

### Performance

- [x] **PERF-01**: Quest explore objective target coordinates are pre-parsed to Location objects at QuestConfig load time — no per-tick `String.split()` allocation

### Security

- [ ] **SEC-01**: Hologram actions support optional `permission` field in YAML config — HologramInteractListener checks player permission before executing action
- [ ] **SEC-02**: NPC skin texture Base64 validation rejects malformed data before sending to clients via PacketEvents

### Observability

- [ ] **OBS-01**: `/servercore debug` subcommand prints live plugin state: active instance counts per system, pending save flags, registered hooks, tick task status

## v2 Requirements

Deferred to future milestone. Tracked but not in current roadmap.

### Performance (Architectural)

- **PERF-V2-01**: Spatial-indexed hologram visibility using chunk-key bucketing — reduces O(n*m) scan to chunk-local evaluation
- **PERF-V2-02**: Batched NPC tab-list removal — single repeating task drains pending removal queue instead of per-NPC delayed tasks
- **PERF-V2-03**: ReactiveManager condition caching with event-driven invalidation — eliminates redundant re-evaluation every 20 ticks

### Scaling

- **SCALE-V2-01**: NPC entity ID pool with reuse — prevents 32-bit entity ID space exhaustion on long-running servers
- **SCALE-V2-02**: Async batched hologram spawning at startup — prevents onEnable() blocking with 1000+ holograms

### Infrastructure

- **INFRA-V2-01**: Hot-reload support (`/servercore reload`) — safe teardown and reinitialization of all nine systems
- **INFRA-V2-02**: Data migration/versioning for YAML schema changes across plugin versions
- **INFRA-V2-03**: PlaceholderAPI reflection replacement with stable API layer

## Out of Scope

| Feature | Reason |
|---------|--------|
| NMS or reflection-based optimization | Project constraint: Paper API only |
| Database migration (SQLite/MySQL) | Multi-week effort; debounced YAML solves the practical problem |
| Full integration test suite | Mixes concerns; write targeted unit tests for fragile logic only |
| New feature systems | This is a fix/harden pass, not feature addition |
| Async entity spawning at startup | Race conditions with other systems initializing; batch-spawn across ticks if needed in v2 |
| Reactive condition caching | Requires event-driven invalidation design; separate milestone |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| CORR-01 | Phase 1 | Complete |
| CORR-02 | Phase 1 | Complete |
| CORR-03 | Phase 1 | Complete |
| CORR-04 | Phase 1 | Complete |
| CORR-05 | Phase 2 | Complete |
| CORR-06 | Phase 2 | Complete |
| LIFE-01 | Phase 1 | Complete |
| LIFE-02 | Phase 1 | Complete |
| LIFE-03 | Phase 1 | Complete |
| LIFE-04 | Phase 1 | Complete |
| MEM-01 | Phase 2 | Complete |
| MEM-02 | Phase 2 | Complete |
| MEM-03 | Phase 2 | Complete |
| CONF-01 | Phase 1 | Complete |
| CONF-02 | Phase 4 | Pending |
| CONF-03 | Phase 1 | Complete |
| CONF-04 | Phase 1 | Complete |
| PERS-01 | Phase 3 | Complete |
| PERS-02 | Phase 3 | Complete |
| PERS-03 | Phase 3 | Complete |
| PERF-01 | Phase 3 | Complete |
| SEC-01 | Phase 4 | Pending |
| SEC-02 | Phase 4 | Pending |
| OBS-01 | Phase 4 | Pending |

**Coverage:**
- v1 requirements: 24 total
- Mapped to phases: 24
- Unmapped: 0 ✓

---
*Requirements defined: 2026-03-21*
*Last updated: 2026-03-21 after initial definition*
