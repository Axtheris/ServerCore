# Project Research Summary

**Project:** ServerCore — Paper 1.21 Plugin Debug & Harden Pass
**Domain:** Paper 1.21 plugin quality engineering (nine-system plugin: cosmetics, pets, holograms, NPCs, quests, emitters, timelines, reactive, GUIs)
**Researched:** 2026-03-21
**Confidence:** HIGH

## Executive Summary

ServerCore is a mature Paper 1.21 plugin with nine interdependent enhancement systems. The base stack (Java 21, Gradle 8.8, Paper API 1.21) is fixed and sound. This milestone is a correctness and hardening pass, not a feature addition — the research confirms the architecture is well-structured (Manager/Instance/Tick/Listener/Store layers) but has a cluster of real production bugs that will cause server incidents at scale: orphan entity leaks from incorrect event cancellation ordering, NPEs from backwards null-check order, unbounded HashMaps that grow without eviction, data loss during shutdown because saves read from live entity references, and silent config failures with no logging.

The recommended approach is to attack these in dependency order: null-safety and idempotent cleanup first (enables everything else), memory leak fixes second, logic correctness third, then async persistence improvements. Critically, fixes must stay within their architectural layer — the existing Manager/Instance/Store boundaries are correct and should not be restructured during a hardening pass. The testing toolchain should add MockBukkit 4.108.0 for scheduler and event-bus tests, SpotBugs for static bytecode analysis, and enable JDWP remote debugging for the `runServer` task.

The primary risks are: (1) fixing null safety inconsistently — missing one of the 636+ entity-access call sites — which requires a systematic grep-based audit rather than ad-hoc fixes; (2) introducing async persistence incorrectly and causing ConcurrentModificationException or partial writes — the snapshot-then-async pattern is mandatory; and (3) scope creep into architectural refactors (spatial hologram indexing, reactive condition caching) that belong in a later dedicated milestone. The harden pass must stay focused on correctness and stability.

## Key Findings

### Recommended Stack

The production stack is locked. Hardening-pass additions are testing and analysis tooling only. The key gap is MockBukkit — without it, scheduler-driven and event-driven behavior (tick lifecycle, listener cleanup) cannot be tested without a live server. SpotBugs on compiled bytecode will surface null-dereference paths and resource leak patterns that manual review misses. The `runServer` JDWP configuration enables breakpoint-level debugging in tick methods without modifying source code.

**Core technologies:**
- JUnit Jupiter 5.14.3: unit test runner — update from 5.11.4 for parameterized test improvements; stay on 5.x (JUnit 6 has no MockBukkit support yet)
- Mockito 5.23.0: mock Bukkit interfaces and manager dependencies — upgrade from 5.14.2 for Java 21 virtual-thread safety fixes
- MockBukkit 4.108.0: full mock Paper server environment — required for scheduler tick control and event firing; use `org.mockbukkit.mockbukkit` coordinates (old `com.github.seeseemelk` is stale)
- SpotBugs Gradle plugin 6.4.8: bytecode-level static analysis — finds NPE paths, race conditions, and resource leaks that Checkstyle misses; treat medium+ findings as build failures during the harden pass
- spark (Paper-bundled): CPU profiling and heap dumps — no install needed; use `/spark profiler` for tick regression and `/spark heapdump` + Eclipse MAT for memory leak diagnosis
- AssertJ 3.27.7: fluent assertion DSL — improves failure messages for manager-state collection assertions

### Expected Features

This milestone defines "features" as robustness capabilities. Every item below is a property that distinguishes a production-grade plugin from a fragile one.

**Must have (P1 — correctness, prevents real server incidents):**
- Null-safe entity access audit — canonical check order: null first, then isDead(), then getWorld() != null
- Idempotent onDisable() cleanup — cancel tasks before clearing manager state; flush saves synchronously
- Bounded hologram cooldown map — periodic eviction; currently grows without bound toward OOM
- Event-cancellation orphan fix — fire CosmeticApplyEvent before spawning the armor stand, not after
- Quest FETCH objective consistency — single source of truth for progress state; fix countMaterial() vs stored-progress divergence
- NPC view-distance validation — clamp to minimum 1 at config load; log warning
- Thread-confinement audit — document and enforce main-thread-only map access; snapshot before async handoff
- Config validation with logged warnings — world existence, numeric bounds, format checks emit WARNING with key name
- Hologram action permission check — optional per-action `permission:` field enforced in HologramInteractListener

**Should have (P2 — quality, production-grade):**
- Debounced async YAML persistence — dirty flag + periodic snapshot flush; synchronous final flush in onDisable()
- Pre-parsed quest EXPLORE coordinates — eliminate per-tick String.split() allocation; parse at QuestConfig.load()
- NPC skin texture Base64 validation — reject malformed textures at load time with logged warning
- Initialization order hardening — single DependencyChecker in onEnable(); explicit log for each present/absent soft dep
- Admin debug command (/servercore debug) — print active instance counts, pending saves, hook status
- Chunk unload safety for hologram visibility — null world guard in HologramVisibilityTracker before distanceSquared

**Defer (P3 — scale, not current incidents):**
- Spatial-indexed hologram visibility — O(n*m) scan is fine below ~50 holograms / ~20 players; architectural refactor for later
- Batched NPC tab-list removal — optimization, not correctness; address if scheduler task queue becomes measurable
- Reactive condition caching — requires event-driven invalidation design; separate milestone
- NPC entity ID pool — long-tail 32-bit space exhaustion; not a current concern

### Architecture Approach

The existing Manager/Instance/Tick/Listener/Store/Config/Hook/API eight-layer architecture is sound and must be preserved. All hardening changes are additive safety layers within each component's current responsibility — they do not move logic between layers. Every fix maps to a single component boundary: instance-layer entity lifecycle guards, manager-layer event ordering fixes, config-layer validation, store-layer async persistence, and listener-layer idempotency.

**Major components:**
1. Config Layer (ServerCoreConfig, CosmeticConfig, QuestConfig, etc.) — parse YAML into validated objects; validation with fail-fast logging belongs here
2. Manager Layer (CosmeticManager, HologramManager, QuestManager, etc.) — registry + lifecycle state machine; event fire ordering and cleanup idempotency are manager responsibilities
3. Instance Layer (CosmeticInstance, PetInstance, TimelineInstance) — per-tick entity tracking; canonical null/dead/world guards live here
4. Tick Task Layer (CosmeticTickTask, HologramTickTask, etc.) — BukkitRunnable calling manager.tickAll(); must be cancelled in onDisable() before manager state is cleared
5. Listener Layer (lifecycle listeners, HologramInteractListener) — reacts to Bukkit events; all listeners must be idempotent (double-fire safe)
6. Store Layer (CosmeticStore, PetStore, QuestStore) — YAML serialization; must save from definition maps (not live entity refs) and support async snapshot pattern
7. Hook Layer (PlaceholderHook, VaultHook, PacketEvents) — optional soft-dep bridges; init must be centralized in one DependencyChecker pass
8. API Layer (ServerCoreAPI) — external read-only accessor; must not create or destroy entities

### Critical Pitfalls

1. **Entity spawned before cancellation check (orphan leak)** — armor stands created before isCancelled() check are never tracked by standIndex and can never be cleaned up; fix by firing CosmeticApplyEvent before spawning the stand
2. **Wrong null-check order (entity.isDead() before entity != null)** — Paper entities can be null; checking isDead() on a null reference crashes the tick task; enforce `null → isDead() → getWorld() != null → isValid()` everywhere
3. **Unbounded cooldown and UUID index maps** — hologram cooldown map accumulates millions of entries over weeks; standIndex accumulates dead UUIDs; both cause eventual OOM; fix with periodic eviction sweep
4. **Async task reading live manager collections without snapshot** — moving save() async without snapshotting first causes ConcurrentModificationException and partial saves; the snapshot-then-async pattern is mandatory
5. **onDisable() saving from live entity references** — chunk unload happens before plugin disable on some shutdown paths; WeakReferences become null; save from UUID→definition maps, not from CosmeticInstance entity refs
6. **Quest objective multiple sources of truth** — getFirstIncompleteIndex() logic duplicated in three places in QuestManager; fixing one call site and missing the others leaves the bug partially present; extract to a single method on QuestProgress first, then fix it once
7. **QuestManager double-initialization** — lines 252 and 280 both initialize QuestManager under different conditionals; second init overwrites first; add an initialization guard flag

## Implications for Roadmap

Based on combined research, the fix dependencies create a clear ordering. Fixes within each phase are independent of each other but the phases themselves have ordering constraints: correctness fixes must land before memory/logic fixes (which may touch the same files), and persistence improvements must come after correctness is established (no point debouncing a broken save path).

### Phase 1: Correctness and Stability

**Rationale:** These are the highest-severity issues — bugs that cause crashes or silent failures in correct configurations. They have no inter-dependencies and are lowest risk to change. Null safety and idempotent cleanup must land first because every downstream phase touches the same code paths. Config validation and the soft-dep centralization must land here because they affect observability of all subsequent fixes.
**Delivers:** A plugin that does not crash during normal operation, gives server operators meaningful error output, and has a complete onDisable() lifecycle
**Addresses (P1 features):** Null-safe entity access audit, idempotent onDisable() cleanup, event-cancellation orphan fix, NPC view-distance validation, config validation with logged warnings, soft-dep initialization order hardening, QuestManager double-initialization fix
**Avoids:** Pitfalls 1, 2, 3 (entity/null), 7 (double-init), 10 (silent parse failures)

### Phase 2: Memory and Logic Correctness

**Rationale:** Memory leaks and quest logic bugs require the null safety fixes from Phase 1 to be stable first (touching the same manager files cleanly). The quest objective single-source-of-truth extraction must precede the FETCH fix — extract first, fix once.
**Delivers:** Bounded memory usage on long-running servers; correct quest progression across accept/abandon cycles
**Addresses (P1 features):** Bounded hologram cooldown map, stand UUID index purge, quest FETCH objective consistency, quest getFirstIncompleteIndex() deduplication, hologram null world guard
**Avoids:** Pitfalls 4 (unbounded maps), 9 (quest multiple sources of truth)

### Phase 3: Async Persistence and Performance

**Rationale:** Persistence improvements require correctness to be established (Phase 1) before optimizing the save path. The snapshot-then-async pattern must be implemented correctly the first time — no incremental approach. Pre-parsing quest coordinates is a performance improvement with no correctness dependency, but belongs here to keep Phase 1 focused on crash fixes.
**Delivers:** Non-blocking main thread during cosmetic/pet/quest persistence; elimination of per-tick String allocation in quest explore checks
**Addresses (P2 features):** Debounced async YAML persistence, pre-parsed quest EXPLORE coordinates, chunk unload safety for hologram visibility
**Uses (stack):** Snapshot-then-async pattern (no new library), existing YamlConfiguration + BukkitScheduler.runTaskAsynchronously()
**Avoids:** Pitfalls 5 (async collections), 6 (onDisable data loss)

### Phase 4: Security and Observability

**Rationale:** Permission checks and Base64 validation are additive (no existing behavior changes) and have no dependencies on earlier phases — they could go in Phase 1. They are placed here because they are low-risk and lower severity than crash bugs; they should not delay correctness work.
**Delivers:** Hologram action permission enforcement; NPC skin texture safety; in-game live state inspection for server operators
**Addresses (P2 features):** Hologram action permission check, NPC skin texture Base64 validation, admin debug command (/servercore debug)
**Avoids:** Security pitfalls (permission bypass, malformed packet sends to clients)

### Phase Ordering Rationale

- Null safety comes before memory leak fixes because the periodic sweep in Phase 2 calls `Bukkit.getEntity(uuid)` — that call path must be null-safe first
- Event-cancellation orphan fix (Phase 1) comes before persistence debouncing (Phase 3) because a debounced save of a partially-constructed cosmetic state is worse than a blocking save of correct state
- Quest single-source-of-truth extraction (Phase 2) comes before FETCH fix (also Phase 2) — extract then fix, in that order, within the same phase
- Async persistence (Phase 3) is explicitly gated after onDisable() is correct (Phase 1) — a broken synchronous save path must not be replaced with a broken async path
- Spatial hologram indexing and reactive condition caching are not included — both are architectural refactors that change performance-critical paths and deserve their own dedicated milestone with proper requirements

### Research Flags

Phases with standard, well-documented patterns (skip research-phase during planning):
- **Phase 1:** All fixes are localized to known call sites documented in CONCERNS.md and ARCHITECTURE.md; patterns are established (guard-then-act, fail-fast config validation)
- **Phase 2:** Memory eviction patterns (removeIf on cooldown maps, UUID index sweep) are standard; no external dependencies
- **Phase 4:** Permission check is a one-field YAML addition; debug command is a standard Bukkit command implementation

Phases that may benefit from deeper research during planning:
- **Phase 3 (async persistence):** The snapshot-then-async pattern is documented but the specific interaction with Paper's scheduler during shutdown (onDisable() synchronous flush vs. pending async tasks) may need verification against Paper 1.21.x shutdown sequence documentation. Verify that `runTaskAsynchronously()` tasks scheduled during `onDisable()` are guaranteed to complete before process exit — they are not, which is why the synchronous flush in `onDisable()` is mandatory.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Core tooling (MockBukkit, SpotBugs, spark) verified via Maven Central and official docs; version numbers for Mockito and AssertJ are MEDIUM (sourced from search results, not directly verified on Maven Central) |
| Features | HIGH | Derived from direct codebase audit (CONCERNS.md) cross-referenced with PaperMC official documentation and EssentialsX/DecentHolograms open-source reference patterns |
| Architecture | HIGH | Based on the actual ServerCore source structure; all fix locations are identified to specific files and line numbers; Paper API threading model is well-documented |
| Pitfalls | HIGH | Every critical pitfall is grounded in a confirmed bug in CONCERNS.md or a documented Paper API behavior; not inferred from general patterns |

**Overall confidence:** HIGH

### Gaps to Address

- **Mockito 5.23.0 and AssertJ 3.27.7 version confirmation:** Research sourced from search results, not directly queried Maven Central. Verify exact latest versions before updating build.gradle — use `./gradlew dependencyUpdates` or check `central.sonatype.com` directly.
- **PlaceholderAPI reflection replacement:** ARCHITECTURE.md flags this as HIGH risk because it requires verifying the stable PAPI API surface before replacing the static reflection cache. Defer to Phase 3 or a dedicated investigation step within Phase 4; do not fix speculatively.
- **Paper 1.21.x shutdown sequence for async tasks:** Confirm whether async tasks submitted via BukkitScheduler during `onDisable()` are run to completion or cancelled. If cancelled, the synchronous flush pattern in Phase 3 is the only safe option — which is the current recommendation, but the confirmation matters for documentation.
- **hologram.spawnAll() blocking startup:** ARCHITECTURE.md notes this as a scaling concern for 200+ holograms. Current server size is unknown. If hologram count is under ~100, this is not an active incident. Flag for measurement before adding tick-spread spawning complexity.

## Sources

### Primary (HIGH confidence)
- `.planning/codebase/CONCERNS.md` — confirmed bugs, fragile areas, security issues (basis for all pitfall research)
- https://docs.papermc.io/paper/dev/ — Paper API, scheduling, debugging, Folia support
- https://github.com/MockBukkit/MockBukkit — MockBukkit v1.21 branch; 4.108.0 confirmed on Maven Central
- https://plugins.gradle.org/plugin/com.github.spotbugs — SpotBugs Gradle plugin 6.4.8 (released Dec 11, 2025)
- https://junit.org/junit5/docs/current/release-notes/ — JUnit 5.14.3 current stable
- https://spark.lucko.me/ + https://docs.papermc.io/paper/profiling/ — spark bundled in Paper 1.21+
- https://eclipse.dev/mat/ — Eclipse MAT 1.16.0

### Secondary (MEDIUM confidence)
- https://www.spigotmc.org/threads/async-data-saving.372210/ — async persistence patterns (community consensus)
- https://github.com/PaperMC/Paper/issues/13206 — chunk memory leak; confirms unbounded map risk
- https://github.com/EssentialsX/Essentials — open-source production plugin reference for admin command patterns
- https://wiki.decentholograms.eu/ — packet-based hologram implementation patterns
- SpigotMC forum — HashMap vs ConcurrentHashMap threading discussion

### Tertiary (LOW confidence — confirm during planning)
- https://mvnrepository.com/artifact/org.assertj/assertj-core — AssertJ 3.27.7 (not directly verified on Maven Central)
- https://github.com/mockito/mockito/releases — Mockito 5.23.0 (sourced from search result)

---
*Research completed: 2026-03-21*
*Ready for roadmap: yes*
