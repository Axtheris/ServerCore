# Pitfalls Research

**Domain:** Paper 1.21 plugin debugging and hardening (multi-system: cosmetics, pets, holograms, NPCs, quests, emitters, timelines, reactive, GUIs)
**Researched:** 2026-03-21
**Confidence:** HIGH — all critical pitfalls grounded in actual bugs documented in CONCERNS.md and verified against Paper community patterns

---

## Critical Pitfalls

### Pitfall 1: Entity Spawned Before Cancellation Check — Orphan Entity Leak

**What goes wrong:**
The pattern "fire cancellable event → check isCancelled() → proceed" fails when the entity is spawned or state is mutated *before* the cancellation check. If `CosmeticApplyEvent` fires after the armor stand is already spawned, a cancelled event leaves an invisible, untracked armor stand in the world with no cleanup path. The stand UUID is never added to `standIndex`, so `CosmeticLifecycleListener` can never find and remove it.

**Why it happens:**
Developers implement "fire event, then do work" but put the entity spawn before the event check in the same method, or fire the event after partially setting up state. The entity exists but is unreachable by any manager.

**How to avoid:**
Fire the cancellable event first, check `isCancelled()` immediately, and only then spawn the entity. No state mutation or entity creation should occur before the cancellation check returns false. Apply this to all nine systems that use cancellable API events.

**Warning signs:**
- Invisible armor stands accumulating in worlds (use `/summon` debug or entity counters)
- `CosmeticManager.standIndex` size diverges from `activeCosmetics` size
- ArmorStand-Limiter plugins flagging unexpected entity counts in chunks where no live cosmetics exist

**Phase to address:** Bug fix phase — fix `CosmeticApplyEvent` and `PetSummonEvent` flow ordering before any hardening work, since every other cleanup mechanism depends on the entity being in the index.

---

### Pitfall 2: Double-Removal and Non-Idempotent Cleanup Causing NPE

**What goes wrong:**
`CosmeticLifecycleListener` responds to both `EntityDeathEvent` and `ChunkUnloadEvent`. Both can fire in rapid succession for the same entity (e.g., a mob dies in a chunk that then unloads). If the removal path calls `entity.remove()` or dereferences the armor stand without checking whether cleanup already happened, the second handler hits a null entity reference or an already-removed entity, causing an NPE or `IllegalStateException`.

**Why it happens:**
Event ordering in Paper is not guaranteed to be exclusive. Entity death triggers `EntityDeathEvent`, but the entity may still be present in the chunk during `ChunkUnloadEvent`. Without idempotency guards, a Set/Map `remove()` that returned false (meaning already removed) silently allows the code to continue dereferencing a cleaned-up instance.

**How to avoid:**
Make all cleanup paths idempotent. Check the return value of `Map.remove()` — if it returns null (already removed), return immediately without proceeding. Use a dedicated cleanup method (e.g., `CosmeticManager.destroyIfTracked(UUID)`) that checks presence before acting. Never call `entity.remove()` without verifying the entity is still in the manager's index.

**Warning signs:**
- Sporadic NPE stack traces in console during high-traffic events (boss fights, world resets)
- Stack trace points to `CosmeticLifecycleListener` or `PetLifecycleListener` during `ChunkUnloadEvent`
- `ConcurrentModificationException` during `tickAll()` if a listener modifies the active map during iteration

**Phase to address:** Bug fix phase — apply idempotent guard to all six lifecycle listener pairs (cosmetics, pets, holograms, NPCs, emitters, timelines) before running stress tests.

---

### Pitfall 3: Null World Before Null Entity — NPE in Wrong Order

**What goes wrong:**
The code in `CosmeticInstance.tick()` and `HologramVisibilityTracker` checks `if (entity.isDead() || entity == null)` — but `entity` is dereferenced *before* the null check. Similarly, checking a Location's world (`location.getWorld()`) without first verifying the location itself is non-null, or that the world hasn't been unloaded, causes NPE on world operations. The hologram null-world bug (`HologramVisibilityTracker:57`) is a confirmed instance of this.

**Why it happens:**
Developers write defensive checks in an intuitive but incorrect order: "check if dead, then check if null" reads naturally but crashes when null. World unloads are not guarded against because developers assume the world reference persists for the entity's lifetime.

**How to avoid:**
Enforce the canonical check order everywhere:
1. `if (entity == null) → cleanup`
2. `if (entity.isDead()) → cleanup`
3. `if (entity.getWorld() == null) → cleanup`
4. `if (!entity.isValid()) → cleanup`

For Locations, always check `location.getWorld() != null` before any world-dependent operations. In `HologramVisibilityTracker`, guard against null world before evaluating distance or conditions.

**Warning signs:**
- NPE stack traces referencing `.getWorld()`, `.getLocation()`, or `.isDead()` inside tick tasks
- NPE only occurs when worlds are being loaded or unloaded, or during server shutdown
- HologramVisibilityTracker crashes only when using custom world names in configs

**Phase to address:** Bug fix phase — this is the highest-frequency crash class; fix all tick methods and the visibility tracker before any performance work.

---

### Pitfall 4: Unbounded Maps Cause Eventual OutOfMemoryError

**What goes wrong:**
Two confirmed unbounded maps exist: (1) `HologramInteractListener.cooldowns` accumulates one entry per unique `player-UUID + hologram-ID` pair and never evicts expired entries. (2) `CosmeticManager.standIndex` and `PetManager.standIndex` accumulate dead entity UUIDs as entities die without triggering cleanup (e.g., entity removed by another plugin rather than dying normally). After weeks of operation with hundreds of players, these maps grow to millions of entries.

**Why it happens:**
Developers add entries on event and forget to add corresponding removal. Expiry-based cleanup is skipped with the rationalization that "the entry only wastes a few bytes." On live servers, hundreds of unique players each triggering hundreds of hologram interactions compound into tens of millions of dead map entries.

**How to avoid:**
For cooldown maps: use a scheduled cleanup task (e.g., every 10 minutes, `cooldowns.entrySet().removeIf(e -> e.getValue() < System.currentTimeMillis())`) or switch to Guava's `Cache` with TTL expiry. For stand UUID indexes: add a `cleanupDeadStands()` pass in the existing tick cycle — any UUID whose `Bukkit.getEntity(uuid)` returns null should be evicted. Do not rely on lifecycle events alone for cleanup; pair with periodic audits.

**Warning signs:**
- JVM heap usage climbing steadily over days/weeks with no plateau
- `cooldowns.size()` exceeds player count multiplied by hologram count
- Heap dumps show large `HashMap$Entry[]` arrays in hologram or cosmetic classes
- GC pauses increasing in length over server uptime

**Phase to address:** Memory and scaling phase — fix cooldown map first (highest growth rate), then stand index cleanup.

---

### Pitfall 5: Async Task Accessing Bukkit API or Shared Collections

**What goes wrong:**
If `CosmeticStore.save()` or `QuestStore.save()` is moved to an async task to avoid blocking the main thread, but the async task reads from `activeCosmetics` (a `HashMap`) while the main thread's tick task is writing to it, the result is a `ConcurrentModificationException` or silent data corruption. Paper enforces that Bukkit API calls (entity spawning, location retrieval, packet sending) must happen on the main thread; async violations can cause server crashes or desynced world state.

**Why it happens:**
Developers correctly identify YAML I/O as slow and move saves async, but forget to snapshot the data before handing it off to the async thread. The async task reads live manager state, which is modified by the main thread concurrently.

**How to avoid:**
The correct pattern for async persistence: (1) On the main thread, create an immutable snapshot of the data to save (a `Map.copyOf()` or serialized string). (2) Pass the snapshot to `Bukkit.getScheduler().runTaskAsynchronously()`. (3) The async task only writes to disk, never reads from or writes to live manager state. Never call `Bukkit.getEntity()`, `player.getLocation()`, or any entity method from an async context.

**Warning signs:**
- `ConcurrentModificationException` stack traces during save operations
- YAML files written with partial data (missing entries that existed in memory)
- Intermittent save failures that only occur under load

**Phase to address:** Performance and persistence phase — implement dirty-flag + async snapshot pattern when adding debounced saves.

---

### Pitfall 6: onDisable() Saves Missing Entities in Unloaded Chunks

**What goes wrong:**
During server shutdown, `onDisable()` calls `CosmeticStore.save()` and `QuestStore.save()`. By the time `onDisable()` runs, chunks may already be unloaded, meaning entities held by `WeakReference` in `CosmeticInstance` have been garbage collected or invalidated. The save then writes empty or partial state, losing all cosmetics for mobs that were in unloaded chunks. This is silent — no exception is thrown, data just disappears.

**Why it happens:**
Plugin lifecycle assumes entities are available during `onDisable()`, but Paper unloads chunks before calling plugin disable on some shutdown paths. WeakReferences are designed to be collected under memory pressure, which shutdown triggers.

**How to avoid:**
In `onDisable()`, save from the manager's UUID→definition maps (which don't use weak references) rather than from live entity references. For cosmetics, persist the mob UUID + cosmetic item mapping, not the armor stand position — positions are always recomputed on next load. Validate that each piece of saved data doesn't depend on a live entity reference.

**Warning signs:**
- Players complain that cosmetics or pets disappear after server restarts
- Save files have fewer entries after a graceful restart than before
- Data loss reproducible by applying a cosmetic then doing `/stop` immediately

**Phase to address:** Data persistence phase — audit all Store classes to ensure save() reads from definition maps, not live entity state.

---

### Pitfall 7: Soft Dependency Initialization Order Causing Silent Subsystem Disable

**What goes wrong:**
`ServerCore.onEnable()` checks for PacketEvents, PlaceholderAPI, ModelEngine, and Vault at runtime. If a soft dependency loads after ServerCore (despite `softdepend` in plugin.yml), the check returns false and the subsystem is silently disabled for the entire server session. `QuestManager` is initialized twice (lines 252 and 280) if NPCs are enabled — the second initialization overwrites the first, potentially losing registered quests.

**Why it happens:**
`softdepend` in plugin.yml declares preference, not guarantee. Paper processes load order best-effort for soft dependencies. Developers assume that if a plugin is installed, it will always be available at `onEnable()` time. Duplicate initialization is introduced when subsystems have conditional paths that don't share a central initialization flag.

**How to avoid:**
Use `plugin.yml` `depend` for required dependencies and `softdepend` for optional ones, but always add a fallback log message when a soft dependency is absent. For duplicate initialization: introduce a boolean flag (e.g., `questManagerInitialized`) that prevents second initialization. Centralize soft-dependency detection into a single `HookManager` that runs once, logging the result of each hook.

**Warning signs:**
- NPCs don't spawn despite PacketEvents being installed
- Quest system behavior changes depending on whether NPCs are enabled in config
- Console shows no error but subsystem features are absent
- `QuestManager` state is inconsistent after enable

**Phase to address:** Stability/hardening phase — centralize hook detection and add duplicate-initialization guard early, as this affects every downstream system.

---

### Pitfall 8: Sending Packets to Disconnecting or Disconnected Players

**What goes wrong:**
`NPCRenderer` schedules a `runTaskLater(plugin, 2)` delayed task to hide NPCs from the tab list after spawning. If the player disconnects between the spawn packet and the delayed hide task, the task runs against an offline player reference, causing NPE in PacketEvents or Paper's packet queue. Similar issues arise if `HologramVisibilityTracker` evaluates conditions for a player who disconnects mid-evaluation cycle.

**Why it happens:**
Delayed tasks capture a player reference at scheduling time. A 2-tick window is normally safe but player disconnects happen at any point. Developers test with stable connections and never observe the race.

**How to avoid:**
In all delayed tasks that reference a player, add a `player.isOnline()` guard as the first check inside the task body. For NPC rendering: batch all tab-list removals into a single task that runs once per tick and checks player online status before each packet send. Catch `IOException` or `IllegalStateException` around packet sends (PacketEvents can throw if the channel is closed).

**Warning signs:**
- NPE or `ChannelClosedException` stack traces in console correlating with player disconnect timestamps
- NPCRenderer errors appear exactly 2 ticks after a player disconnect
- Hologram visibility errors fire only when players disconnect during a server-tick-heavy moment

**Phase to address:** NPC stability phase — fix delayed task guard before any NPC rendering improvements.

---

### Pitfall 9: Quest Objective Progress State Machine Has Multiple Sources of Truth

**What goes wrong:**
`QuestManager` calls `getFirstIncompleteIndex()` in three separate places (lines 229, 278, 334). If any one of these diverges in its definition of "incomplete" — perhaps after a bug fix changes the logic in one location — quest progression silently breaks. Players can get stuck on objectives that are already complete (according to two of the three call sites) but not recognized by the third.

**Why it happens:**
Copy-paste of inline logic rather than extracting to a shared method. When fixing one bug in the progress check, developers only update the visible call site and miss the others.

**How to avoid:**
Extract `getFirstIncompleteIndex()` as a single method on `QuestProgress`, making it the only source of truth for objective sequencing. Replace all three call sites with this method. Apply the same principle to FETCH objective completion: move inventory counting into `QuestProgress.isFetchComplete()` rather than scattering across `QuestManager`.

**Warning signs:**
- Quest bug reports that are inconsistent — "it works sometimes" — suggesting different paths hit different check logic
- Players softlocked on quests despite having the required items
- Fixing a quest bug at one call site but leaving two others unfixed

**Phase to address:** Quest system bug fix phase — extract to single method before fixing the FETCH objective tracking bug, as the fix must only exist in one place.

---

### Pitfall 10: Config Parse Errors That Swallow Exceptions Instead of Logging

**What goes wrong:**
When `CosmeticConfig` or `QuestConfig` encounters a malformed YAML entry (bad EntityType name, missing required field, unparseable coordinate string), the current pattern catches the exception and silently skips that entry. The server operator sees no indication that a cosmetic profile or quest definition was ignored. On the quest side, explore target coordinates are re-parsed from strings every tick (`split(",")` on every objective check), meaning a malformed coordinate silently fails every tick with no log output.

**Why it happens:**
The Bukkit pattern of "log warning and continue" is the right approach for config parsing, but developers omit the log, leaving silent failures. Pre-parsing is not done because "it works well enough for small configs."

**How to avoid:**
Every config parse failure must emit a `getLogger().warning()` with the specific key, file location, and expected format. Pre-parse all coordinate strings for EXPLORE objectives during `QuestConfig.load()` and store as `Location` objects — fail loudly at load time, not silently every tick. Apply the same to NPC skin texture validation: validate Base64 format at load time, log a warning and skip invalid entries.

**Warning signs:**
- Server operator reports "quest X doesn't work" but no error appears in console
- EXPLORE objective never triggers even when player stands in the correct location
- Cosmetic profiles missing from in-game but present in YAML with no error logged

**Phase to address:** Config validation phase — add warnings before fixing downstream bugs, otherwise bug fixes will be attributed to the wrong cause.

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Synchronous YAML save on every action | Simple code, no batching logic | Blocking main thread I/O; visible lag spikes on high-activity servers | Never on a live server; acceptable only in tests |
| `null` return for "not found" | No Optional boilerplate | Callers forget to check; NPE propagates far from origin | Only for internal private methods with documented contract |
| Checking `entity == null` after `entity.isDead()` | Reads naturally | Crashes when entity is actually null; the most common NPE pattern in Bukkit code | Never — the null check must always be first |
| Parsing coordinate strings every tick | No pre-processing step | Allocates arrays on every tick for every active quest; measurable GC pressure | Never for per-tick paths; pre-parse at load time |
| Duplicate manager initialization with conditionals | Easy to add new subsystem branches | State corruption when second init overwrites first | Never — use an initialization guard flag |
| Static `WeakReference` cache for reflection methods (PlaceholderAPI hook) | One-time lookup | Silent degradation if API changes; exception swallowing hides the failure | Never — use the stable PAPI API or inject the hook via interface |

---

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| PacketEvents (NPC rendering) | Sending packets to players without checking `player.isOnline()` or catching closed-channel exceptions | Guard every packet send with `player.isOnline()` and wrap in try-catch for `IOException`; verify PacketEvents is non-null before the NPC tick runs |
| PlaceholderAPI | Caching `Method` object via reflection; silently returning false when PAPI version mismatches | Call `PlaceholderAPI.setPlaceholders(player, text)` directly via the stable API; never cache reflected methods statically |
| ModelEngine | Assuming model lookup succeeds; no fallback when model ID is missing | Check `ModelEngineAPI.getModeledEntity()` for null before applying; log a specific warning with the missing model ID when fallback triggers |
| Vault | Registering Vault hooks without checking Economy provider is non-null after plugin detection | Two-step check: plugin present AND `economy != null` after `getRegistration()` |
| Paper entity API | Calling `entity.remove()` on an entity in an unloaded chunk | Verify `entity.isValid()` and `entity.getChunk().isLoaded()` before removing; batch removals via a main-thread scheduler if called from a chunk event |

---

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| O(n*m) hologram visibility scan every tick | TPS drops proportional to player count * hologram count | Chunk-based spatial index; only evaluate holograms in chunks within player render distance | ~50+ holograms with 20+ players simultaneously |
| O(n*m) reactive rule evaluation every 20 ticks | Reactive cosmetics cause TPS spikes on high-population servers | Cache condition results per player; invalidate only on relevant state change events | ~20+ reactive rules with 40+ players |
| YAML `save()` on every cosmetic apply or pet summon | TPS spike on popular cosmetics; visible lag on first-join pet restore | Dirty-flag + scheduled async flush (snapshot data first, write async) | Any server with >5 simultaneous cosmetic operations per second |
| `String.split(",")` in EXPLORE objective per tick | GC pressure from array allocation on every active quest, every tick | Pre-parse coordinates to `Location` at `QuestConfig.load()` time | Noticeable at 50+ players each with active EXPLORE quests |
| `runTaskLater()` per NPC spawn for tab-list hide | Task queue bloat on servers with many players entering NPC view range simultaneously | Collect pending tab-list removals; process batch once per tick | 20+ NPCs in a populated area with 30+ players passing through |
| Synchronous `hologram.spawnAll()` on `onEnable()` | Server startup lag spike; plugins with many holograms block the main thread for seconds | Spread hologram spawning across ticks with a counter-based scheduler | 200+ holograms on plugin reload |

---

## Security Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| Hologram click actions execute without permission check | Any player can trigger command-dispatch or quest-start actions regardless of operator intent | Add optional `permission: <node>` field to hologram action config; check in `HologramInteractListener` before dispatching |
| NPC skin textures loaded from config without Base64 validation | Malformed texture strings sent to all clients in view range; potential client kick | Validate Base64 format and length at `NPCConfig.load()` time; log warning and skip invalid skins |
| Quest rewards bypass inventory full check | Items silently dropped on ground uncontrolled; server lag from item entity spawning; exploitable by griefing | Check `player.getInventory().firstEmpty() == -1` before giving reward; offer alternative (mail system, chest) or warn player |
| Armor stand stand UUIDs not isolated from external plugin targeting | External plugins iterating world entities can accidentally interact with cosmetic stands | The existing marker + equipment-lock approach is correct; ensure `standIndex` is not exposed via public API; document internal-only status |

---

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| Silent config parse failures | Server operators spend hours debugging why a quest or cosmetic "just doesn't work" with no console output | Every skip due to parse error emits a `WARNING` level log with the exact key and reason |
| No admin debug command | Operators cannot inspect manager state in-game; must add print statements and recompile to diagnose | Add `/servercore debug` showing active instance counts, pending saves, and hook status per system |
| NPC view distance of 0 silently disables NPCs | Config typo causes all NPCs to be invisible; no error is logged | Validate view-distance at load time; clamp to 1 minimum; log `WARNING: npc.view-distance set to 0, clamped to 1` |
| Quest abandonment resets FETCH progress but items remain in inventory | Players re-accept quest, items already in inventory but progress shows 0; confusing UX | Track FETCH objectives as incremental progress in `QuestProgress`; detect existing items on quest accept |
| Hologram interactions silently fail when cooldown is active | Player clicks hologram, nothing happens, no feedback | Add optional `cooldown-message` config field; send feedback when cooldown is active |

---

## "Looks Done But Isn't" Checklist

- [ ] **Cancellable event cleanup:** Verify that cancelling `CosmeticApplyEvent` or `PetSummonEvent` does NOT leave a spawned entity behind — check that entity spawn happens strictly after the cancel check.
- [ ] **Lifecycle listener idempotency:** Verify that calling `removeCosmetics(uuid)` twice in a row (simulating death + chunk unload) does not throw or produce any error — the second call must be a safe no-op.
- [ ] **Null check order:** Grep for `entity.isDead()` and verify every call site also has a preceding `entity != null` check (or uses the entity from a map that guarantees non-null).
- [ ] **onDisable data integrity:** Restart the server immediately after applying a cosmetic and confirm the cosmetic persists — tests that saves read from definition maps, not live entity state.
- [ ] **Soft dependency isolation:** Disable PlaceholderAPI and confirm hologram conditions evaluate to a safe default (not NPE); disable PacketEvents and confirm NPCs are cleanly absent with a logged warning.
- [ ] **Cooldown map bounded:** After 1000 simulated hologram interactions across 100 unique player UUIDs, confirm `cooldowns.size()` does not equal 1000 — entries must have been evicted.
- [ ] **Quest objective single source of truth:** Verify that `getFirstIncompleteIndex()` is called from one place only; grep confirms no inline copies of the same logic.
- [ ] **Config parse failures logged:** Introduce a deliberately malformed EntityType in `cosmetics.yml` and confirm the console shows a WARNING identifying the bad key, not silence.
- [ ] **NPC delayed task guard:** Disconnect a player exactly as an NPC would spawn for them; confirm no NPE or PacketEvents error appears in console.
- [ ] **Async save snapshot pattern:** Confirm that the async persistence task holds a copy of data, not a reference to the live manager map — verify by modifying the live map after scheduling and checking the saved file reflects the snapshot, not the modification.

---

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| Orphan armor stands from cancelled events | LOW | Run entity audit command; remove all invisible armor stands not in `standIndex`; add idempotent cancel guard to prevent recurrence |
| Cooldown map OutOfMemoryError | HIGH | Requires server restart; add periodic cleanup task; monitor heap before restart is necessary |
| Quest data corruption from async save race | HIGH | Restore from backup YAML; implement snapshot pattern; add file-write lock to prevent partial writes |
| NPE crash from null-world hologram | LOW | Fix null guard in `HologramVisibilityTracker`; no data loss; server restart not required if plugin is hot-reloadable |
| Soft dependency silently disabled | LOW | Verify plugin load order in console startup logs; add explicit `depend` entry if required; restart with correct load order |
| onDisable data loss for entities in unloaded chunks | MEDIUM | Restore from last clean backup; fix save to use definition maps; add shutdown hook that saves before chunk unload begins |
| QuestManager double-initialization | MEDIUM | Add initialization guard; verify quest registry is populated correctly after enable; restart required |

---

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| Orphan entity from cancelled event | Phase 1 — Bug fixes (cancellation ordering) | Cancel a cosmetic apply via API and confirm no armor stand remains in world |
| Double-removal / non-idempotent cleanup | Phase 1 — Bug fixes (lifecycle listeners) | Trigger death + chunk unload for same entity; confirm no console error |
| Null check order (entity/world) | Phase 1 — Bug fixes (null safety audit) | Grep confirms `entity == null` always precedes `entity.isDead()`; HologramVisibilityTracker world guard present |
| Unbounded cooldown map | Phase 2 — Memory hardening | After 24-hour simulated load, `cooldowns.size()` stays bounded by eviction count |
| Async task accessing live collections | Phase 3 — Persistence improvements | Confirm async save task holds snapshot; no CME under load testing |
| onDisable save missing entities | Phase 3 — Persistence improvements | Restart test: apply cosmetic, `/stop`, restart, confirm cosmetic present |
| Soft dependency initialization order | Phase 1 — Stability (duplicate init fix) | QuestManager initialized exactly once; PAPI-absent server boots cleanly |
| Packet to disconnecting player | Phase 4 — NPC stability | Disconnect player during NPC render; confirm no NPE in console |
| Quest objective multiple sources of truth | Phase 2 — Quest bug fixes | `getFirstIncompleteIndex()` exists in exactly one location; grep confirms |
| Config parse errors swallowed | Phase 1 — Stability (logging hardening) | Malformed YAML key produces WARNING with key name and file line |

---

## Sources

- `.planning/codebase/CONCERNS.md` — Confirmed bugs, fragile areas, scaling limits, security considerations (primary source)
- Paper GitHub issue #8448 — "Can't remove entities during ChunkUnloadEvent" (entity removal during unload)
- Paper GitHub issue #11496 — "cancelling PlayerInteractAtEntityEvent causes impossible entities" (orphan entity from cancel)
- SpigotMC/Bukkit community — Async task safety requirements; `BukkitScheduler` documentation confirms main-thread-only API constraint
- MultiPaper/Folia documentation — Thread safety rules for concurrent collections; ConcurrentHashMap pitfalls when used carelessly
- GitHub Gist (blablubbabc) — Async task cleanup in `onDisable()`; pattern for waiting on async tasks
- PaperMC documentation (plugin-configurations) — Async file I/O best practices
- PaperMC GitHub issue #11353 — NPE fix for disconnected player in respawn; confirms disconnected player packet sends are unsafe
- retrooper/packetevents issue #969 — Packet sending errors requiring online check
- Bukkit Wiki — Scheduler Programming; event ordering; safe entity removal patterns
- CONCERNS.md known bugs: hologram cooldown map, FETCH objective, NPC view distance, ModelEngine fallback, HologramVisibilityTracker null world

---

*Pitfalls research for: Paper 1.21 plugin debugging and hardening — ServerCore nine-system sweep*
*Researched: 2026-03-21*
