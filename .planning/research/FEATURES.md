# Feature Research

**Domain:** Paper 1.21 plugin hardening / debug sweep
**Researched:** 2026-03-21
**Confidence:** HIGH (primary sources: PaperMC docs, SpigotMC community consensus, direct codebase audit)

---

## Context: What "Features" Means Here

This is a hardening milestone, not a feature-addition milestone. "Features" in this context means **robustness capabilities** — the properties that distinguish a production-grade plugin from a fragile one. Every item below is answering: "What quality attribute does a production Paper 1.21 plugin have that a naive one lacks?"

The downstream consumer (requirements definition) should treat Table Stakes as mandatory scope and Differentiators as stretch goals for this milestone.

---

## Feature Landscape

### Table Stakes (Plugin Is Unreliable Without These)

These are capabilities that production plugins always have. Missing any of them causes real server incidents: crashes, entity leaks, data loss, silent failures.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Idempotent cleanup on `onDisable()` | Server crashes, restarts, and reloads all call `onDisable`. If cleanup is not safe to call multiple times or in partial-init states, the server leaks entities and listeners | MEDIUM | Must cancel BukkitRunnable tasks, remove spawned entities (armor stands, pet entities, hologram stands), unregister listeners. Entity removal must handle null worlds and unloaded chunks gracefully |
| Null-safe entity access at every call site | Entities can become null mid-tick due to death, chunk unload, or world unload. Any unchecked `.getLocation()`, `.isDead()`, or inventory access on a null entity causes an uncaught exception that crashes the tick task | MEDIUM | The existing `isDead() || entity == null` pattern must be applied consistently — 636 current occurrences shows inconsistency. Order matters: null check before `isDead()` |
| Bounded in-memory data structures | Maps that grow unbounded over server uptime eventually cause OutOfMemoryErrors on long-running servers. The hologram cooldown map and stand UUID index are current violations | LOW-MEDIUM | Fix: periodic expiry sweeps, TTL-based cleanup, or `ExpiringMap`/`LoadingCache` from Guava. The hologram cooldown map specifically can accumulate millions of entries over weeks |
| Idempotent entity lifecycle cleanup | Entity death and chunk unload events may both fire for the same entity. Double-removal must be safe (no exception, no state corruption) | LOW | Fix with `remove()` return-value check before destroy, or Set-based deduplication. Current `CosmeticLifecycleListener` relies on firing order |
| Task cancellation on tick tasks | If a tick task is not cancelled on disable, it continues running after the plugin is gone, calling methods on null managers | LOW | Store `BukkitTask` references at scheduling time; call `task.cancel()` in `onDisable()` before clearing manager state |
| Config validation with fail-fast or sensible defaults | A misconfigured value (negative view-distance, invalid world name, malformed Base64 skin) that is accepted silently either crashes later with an opaque error or silently disables a system | LOW-MEDIUM | Apply bounds validation (view distance > 0), world existence check, format validation (Base64 skin texture). Log a WARNING with the offending key when using a default fallback |
| Event-cancellation cleanup | When a custom cancellable event (e.g., `CosmeticApplyEvent`) is cancelled after partial execution, any entities already spawned must be cleaned up. Otherwise orphan invisible armor stands accumulate permanently | MEDIUM | Check event cancellation status before committing side effects, or implement compensating cleanup if cancellation occurs after spawn |
| Permission checks on all player-triggered actions | Hologram click actions, command subcommands, and API entry points that execute server-side effects (run commands, grant quests) must verify the triggering player's permission before executing | LOW | Add `permission` field to hologram action config; enforce in `HologramInteractListener` before action dispatch |
| Crash-safe data persistence on disable | Player data (cosmetics, quests, pets) must be flushed to disk in `onDisable()` synchronously before the process exits. Async-only saves that have not completed will be lost | LOW-MEDIUM | `onDisable()` must call a synchronous final flush regardless of whether async dirty-write tasks are pending |
| Thread confinement for main-thread collections | `HashMap` and `ArrayList` accessed from tick tasks (which run on the main thread) are safe only if nothing else touches them from async contexts. If any async I/O completion callback or scheduler touches the same collection, a `ConcurrentModificationException` or silent corruption occurs | MEDIUM | Audit all manager maps for async access; use `ConcurrentHashMap` wherever an async task (save callbacks, async scheduler) might touch the same map as a tick |
| Dead entity / invalid state guards in tick loops | `tickAll()` calls `instance.tick()` every server tick. If the mob or armor stand dies between the null check and `teleport()`, an exception propagates up, potentially skipping cleanup for all subsequent instances in the same tick | LOW-MEDIUM | Use `removeIf()` with a guarded tick that returns false on any exception, not just on `isDead()`. Wrap each instance tick in a try-catch to isolate failures |

### Differentiators (Professional Quality)

These raise the plugin from "works in testing" to "trusted in production." They are not required for correctness but distinguish high-quality plugins from adequate ones.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Debounced async data persistence | Saving YAML on every cosmetic apply or pet summon blocks the main thread and produces unnecessary I/O. A dirty-flag with periodic batch write (e.g., every 5 seconds if dirty) eliminates the I/O overhead and reduces disk wear | MEDIUM | Implement a `ScheduledSave` helper: mark dirty on mutation, flush on a repeating task and on `onDisable()`. The flush in `onDisable()` must be synchronous to guarantee write before server exits |
| Pre-parsed config values | `obj.getTarget().split(",")` inside `QuestManager.handleExplore()` runs on every tick for every active quest, allocating new arrays constantly. Pre-parsing coordinate strings to `Location` objects during config load eliminates this entirely | LOW | Parse explore targets in `QuestConfig.load()` and store as `Location` fields on the objective. Zero runtime allocation per tick |
| Spatial-indexed hologram visibility | The current O(n*m) scan of all holograms against all players every update cycle does not scale. Chunk-key bucketing (which the hologram data model already partially supports) reduces the check to holograms in chunks near each player | HIGH | Use the existing chunk-key concept to build a `Map<Long, List<Hologram>>` index. On each update, for each player, compute nearby chunk keys and only evaluate holograms in those buckets. This is a genuine architectural change — scope carefully |
| Batched NPC tab-list removal | Every NPC spawn schedules its own 2-tick delayed task to remove from the tab list. On servers with many NPCs, this creates a flood of pending scheduler tasks. Collecting pending removals and processing them in a single task per tick eliminates scheduler overhead | LOW-MEDIUM | Replace per-NPC `runTaskLater()` with a single repeating task that drains a `Queue<UUID>` of pending removals each tick |
| Config-level permission on hologram actions | Individual hologram actions expose no permission model. Server operators cannot restrict which players trigger which actions without modifying Java code | LOW | Add optional `permission: node.name` to hologram action YAML entries; check in `HologramInteractListener` before executing |
| Validation of NPC skin texture format | Arbitrary Base64 strings are passed directly to PacketEvents without format validation. A malformed texture could cause client-side rendering errors or crash susceptible clients | LOW | Validate that texture and signature fields are valid Base64 before applying; log a WARNING and skip the skin rather than sending malformed data |
| Admin debug command (`/servercore debug`) | Server operators have no way to inspect live plugin state without reading source code. A debug subcommand that prints active cosmetic count, active pet count, hologram count, active quest count, and registered listener state dramatically reduces diagnostic time | MEDIUM | Implement as a subcommand of the existing `/servercore` command, behind the `servercore.admin` permission. Dump: manager sizes, pending task count, known-dirty store flags |
| Initialization order hardening for soft dependencies | If a soft dependency registers later than expected, systems silently degrade. Centralizing soft-dep detection with explicit log output ("PacketEvents not found — NPC system disabled") makes the state observable | LOW | Extract all soft-dep checks to a single `DependencyChecker` class called once in `onEnable()` before any system initializes. Log INFO for each found/missing dependency |
| Chunk unload safety for hologram visibility | The hologram visibility tracker evaluates `hologram.getLocation().getWorld()` without checking whether the world is still loaded. A world unload mid-evaluation causes NPE | LOW | Add `world != null && world.isChunkLoaded(...)` guard in `HologramVisibilityTracker.evaluate()` before any spatial comparison |
| NPC entity ID pool with reuse | NPC entity IDs are assigned sequentially with no reuse. After millions of spawns across server lifetime, 32-bit entity ID space exhaustion becomes a real concern on high-traffic servers | HIGH | Implement a free-list of entity IDs reclaimed when NPCs despawn. This requires careful bookkeeping but prevents the long-tail scaling limit. Flag as future-milestone complexity |
| ReactiveManager condition caching | The reactive system re-evaluates all conditions for all players every 20 ticks without caching. For conditions that only change on specific events (player level-up, quest completion), full re-evaluation every second wastes CPU | HIGH | Cache condition results per player per rule; invalidate caches when relevant events fire (QuestCompleteEvent, level change, etc.). This is a larger refactor — scope as a separate phase or milestone |

### Anti-Features (Deliberately Out of Scope for This Milestone)

Features that are commonly requested but would introduce more risk than they resolve in a hardening-only pass.

| Feature | Why Requested | Why Problematic for This Milestone | Alternative |
|---------|---------------|-------------------------------------|-------------|
| Hot-reload (`/servercore reload`) | Server operators want to change YAML configs without restarting | Reload requires safely tearing down and reinitializing nine interdependent systems, cancelling tick tasks, despawning entities, re-registering listeners, and restoring state — this is a major feature, not a bug fix. Doing it incorrectly introduces more instability than it solves | Ensure `onDisable()` is complete and correct so restarts are fast and clean. Document which config changes require restart |
| Data migration / versioning | YAML schema changes across plugin versions risk losing player data | Schema migration requires a migration framework, version tracking fields, and tested upgrade paths. Doing it hastily in a hardening pass could corrupt existing data | Add a `config-version` comment field to YAML files so the path is open, but do not implement migration logic this milestone |
| Async entity spawning at startup | Spawning 1000+ holograms blocks `onEnable()` | Async entity spawning on Paper requires careful world/chunk load checks and introduces race conditions with other systems initializing in parallel | Batch-spawn in small groups across the first N ticks using a `BukkitRunnable` counter — achieves the same unblocking without full async complexity |
| NMS / reflection-based optimization | Some optimizations (e.g., direct packet manipulation) require NMS access and could avoid API overhead | The project constraint is Paper API only. NMS breaks on minor version updates and creates maintenance burden that defeats the purpose of a hardening pass | Achieve performance goals within the Paper API surface. PacketEvents already abstracts packet-level operations for NPC rendering |
| Full test suite with integration tests | Unit testing Paper plugins requires mock frameworks (MockBukkit) and integration testing requires a live server | Adding full test coverage in a hardening pass mixes two distinct concerns and risks delaying actual bug fixes. The test infrastructure setup cost is non-trivial | Write targeted unit tests for the specific logic paths that are hard to verify by observation: quest progression, objective index, chunk-key computation. Skip lifecycle integration tests for now |
| Replacing YAML with SQLite/database | YAML files are slow for frequent writes; databases are more robust | Database migration changes the data layer for all nine systems, requires schema design, connection pooling, and migration of existing player data — a multi-week effort | Fix YAML write performance with debouncing and async saves. This solves the practical problem without the migration risk |

---

## Feature Dependencies

```
Null-safe entity access
    └──enables──> Dead entity guards in tick loops
    └──enables──> Chunk unload safety for hologram visibility

Idempotent cleanup (onDisable)
    └──requires──> Task cancellation on tick tasks
    └──requires──> Bounded in-memory structures (or leaks survive restart cycle)

Crash-safe data persistence
    └──requires──> Debounced async persistence (or sync writes block disable)

Event-cancellation cleanup
    └──requires──> Null-safe entity access (cleanup touches same entity refs)

Admin debug command
    └──enhances──> Initialization order hardening (debug output is more useful with explicit dep logging)
    └──enhances──> Bounded in-memory structures (debug shows map sizes)

Config validation
    └──enables──> NPC view-distance bounds fix (validation catches the misconfiguration)
    └──enables──> NPC skin texture validation

Permission checks on hologram actions
    └──requires──> Config-level permission on hologram actions (to be configurable)

Spatial-indexed hologram visibility ──conflicts with──> Reactive condition caching
    (both are high-complexity; doing both in one milestone is too much scope)
```

### Dependency Notes

- **Null-safe entity access enables dead entity guards:** The tick-loop guard only works if the null check is trustworthy. Inconsistent null-checking in other code paths undermines the tick guard.
- **Idempotent cleanup requires task cancellation:** If tick tasks run after manager state is cleared, they access null references. Task cancellation must precede manager teardown in `onDisable()`.
- **Crash-safe persistence requires debouncing:** Without debouncing, making persistence crash-safe requires a synchronous flush per action, which defeats performance goals. Debounce first, then the final flush in `onDisable()` is cheap.
- **Spatial hologram indexing conflicts with reactive caching:** Both are medium-to-high complexity refactors touching performance-critical paths. Attempting both in one phase creates unstable intermediate states. Pick one.

---

## MVP Definition (For This Hardening Milestone)

This is not a feature launch — it is a correctness sweep. "MVP" here means: what is the minimum set of hardening work that makes the plugin trustworthy on a real server?

### Fix First (Correctness — P1)

These are bugs or safety gaps that will cause real server incidents.

- [ ] Null-safe entity access audit — eliminate silent NPEs in tick paths
- [ ] Idempotent `onDisable()` cleanup — cancel tasks, remove entities, flush data
- [ ] Bounded hologram cooldown map — add periodic expiry sweep
- [ ] Event-cancellation orphan fix — clean up spawned stands if event is cancelled
- [ ] Quest FETCH objective progress tracking — fix `areObjectivesComplete()` for abandoned/re-accepted quests
- [ ] NPC view-distance bounds validation — reject zero/negative config values at load time
- [ ] Thread-confinement audit — document and enforce which maps are main-thread-only
- [ ] Config validation with logged warnings — world existence, numeric bounds, format checks
- [ ] Permission check on hologram action execution

### Harden After Correctness (Quality — P2)

These improve reliability but do not cause immediate incidents.

- [ ] Debounced async YAML persistence (dirty flag + periodic batch write)
- [ ] Pre-parsed quest explore coordinates (eliminate per-tick string split allocation)
- [ ] NPC skin texture Base64 validation
- [ ] Initialization order hardening with explicit dependency logging
- [ ] Admin debug command (`/servercore debug`)
- [ ] Chunk unload safety for hologram visibility tracker

### Defer to Later Milestone (P3)

These require architectural changes larger than a hardening pass.

- [ ] Spatial-indexed hologram visibility — genuine architectural refactor
- [ ] Batched NPC tab-list removal — optimization, not correctness
- [ ] Reactive condition caching — requires event-driven invalidation design
- [ ] NPC entity ID pool — long-tail scaling concern, not a current incident

---

## Feature Prioritization Matrix

| Feature | Server Safety Value | Implementation Cost | Priority |
|---------|---------------------|---------------------|----------|
| Null-safe entity access audit | HIGH | MEDIUM | P1 |
| Idempotent `onDisable()` cleanup | HIGH | MEDIUM | P1 |
| Bounded hologram cooldown map | HIGH | LOW | P1 |
| Event-cancellation orphan fix | HIGH | MEDIUM | P1 |
| Quest FETCH objective fix | MEDIUM | LOW | P1 |
| NPC view-distance bounds validation | MEDIUM | LOW | P1 |
| Thread-confinement audit | HIGH | MEDIUM | P1 |
| Config validation | MEDIUM | LOW | P1 |
| Hologram action permission check | MEDIUM | LOW | P1 |
| Debounced async persistence | MEDIUM | MEDIUM | P2 |
| Pre-parsed explore coordinates | MEDIUM | LOW | P2 |
| NPC skin texture validation | LOW | LOW | P2 |
| Dependency logging on enable | MEDIUM | LOW | P2 |
| Admin debug command | MEDIUM | MEDIUM | P2 |
| Chunk unload safety (holograms) | MEDIUM | LOW | P2 |
| Spatial hologram visibility index | MEDIUM | HIGH | P3 |
| Batched NPC tab-list removal | LOW | MEDIUM | P3 |
| Reactive condition caching | MEDIUM | HIGH | P3 |
| NPC entity ID pool | LOW | HIGH | P3 |

**Priority key:**
- P1: Correctness — must fix for plugin to be trustworthy on a live server
- P2: Quality — should fix to be considered production-grade
- P3: Scale — defer until the server actually hits the scaling limit

---

## Reference: Industry Standards for Production Paper Plugins

Based on research into PaperMC documentation, SpigotMC community patterns, and popular open-source plugins (EssentialsX, DecentHolograms, GHolo):

**Thread model (HIGH confidence, PaperMC docs):** All world/entity modifications must happen on the main server thread. Async tasks may only touch thread-safe data structures (`ConcurrentHashMap`, immutable snapshots). Folia-ready plugins use `EntityScheduler` and `RegionScheduler` instead of global `BukkitScheduler` for entity-bound tasks.

**Entity lifecycle (HIGH confidence, Bukkit API):** Production plugins listen to `EntityDeathEvent`, `ChunkUnloadEvent`, and `WorldUnloadEvent` for cleanup. Cleanup handlers must be idempotent — both events can fire for the same entity.

**Data persistence (MEDIUM confidence, SpigotMC forum consensus):** The established pattern is: snapshot data synchronously on the main thread, write asynchronously via `runTaskAsynchronously()`, and flush synchronously in `onDisable()`. Using `ConcurrentHashMap` as the shared data store eliminates the need for manual cloning before async writes.

**Memory management (HIGH confidence, PaperMC issues):** Maps keyed by entity UUID must have cleanup paths. Paper itself has had bugs with `ChunkHolder` memory leaks from maps that grow without bound. Production plugins use periodic cleanup tasks or TTL-based eviction.

**Admin visibility (MEDIUM confidence, PaperMC docs + Hangar quality standards):** Hangar's quality gate for plugins requires that plugins are well-documented and have clear error messages. A `/debug` or `/info` subcommand that exposes live state is a common pattern in complex plugins (EssentialsX exposes version info, player count, module state).

---

## Sources

- [PaperMC Docs — Plugin Development](https://docs.papermc.io/paper/dev/)
- [PaperMC Docs — Scheduling](https://docs.papermc.io/paper/dev/scheduler/)
- [PaperMC Docs — Persistent Data Container](https://docs.papermc.io/paper/dev/pdc/)
- [PaperMC Docs — Folia Support](https://docs.papermc.io/paper/dev/folia-support/)
- [PaperMC Docs — Debugging](https://docs.papermc.io/paper/dev/debugging/)
- [PaperLib — Async chunk loading patterns](https://github.com/PaperMC/PaperLib)
- [SpigotMC Forum — Async Data Saving patterns](https://www.spigotmc.org/threads/async-data-saving.372210/)
- [Paper Issue #13206 — Chunk memory leak](https://github.com/PaperMC/Paper/issues/13206)
- [DecentHolograms — packet-based hologram implementation reference](https://wiki.decentholograms.eu/)
- [PaperMC GitHub — Plugins disabled on startup exception](https://github.com/PaperMC/Paper/issues/507)
- [EssentialsX — open-source production plugin reference](https://github.com/EssentialsX/Essentials)
- ServerCore codebase audit — `.planning/codebase/CONCERNS.md` (2026-03-21)

---

*Feature research for: Paper 1.21 plugin hardening / debug sweep*
*Researched: 2026-03-21*
