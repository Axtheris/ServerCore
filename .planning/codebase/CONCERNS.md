# Codebase Concerns

**Analysis Date:** 2026-03-21

## Tech Debt

**Null-safety pattern inconsistencies:**
- Issue: Heavy reliance on null checks scattered throughout codebase without consistent patterns. 636 occurrences of null/instanceof/get() across 107 files create fragility.
- Files: `src/main/java/net/axther/serverCore/hologram/HologramManager.java:78-85`, `src/main/java/net/axther/serverCore/npc/NPCManager.java:39-44`, `src/main/java/net/axther/serverCore/quest/QuestManager.java:92,105,127`
- Impact: Silent failures when entities/quests/NPCs are not found. Missing null checks in performance-critical paths could cause crashes.
- Fix approach: Introduce Optional<T> pattern or Null Object pattern for common lookups; audit hot paths for missing null guards.

**Reflection-based PlaceholderAPI integration:**
- Issue: HologramVisibilityTracker caches reflection Method object statically with lazy initialization and exception-swallowing fallback.
- Files: `src/main/java/net/axther/serverCore/hologram/visibility/HologramVisibilityTracker.java:20-21,86-91`
- Impact: Silent degradation if PlaceholderAPI version changes; unclear failure mode if method signature differs. Static cache creates class-loader coupling.
- Fix approach: Use dependency injection or service registry instead of reflection; document exact PlaceholderAPI version compatibility.

**Soft dependency initialization order fragility:**
- Issue: PacketEvents, PlaceholderAPI, ModelEngine, Vault detected at runtime but visibility/behavior depends on initialization order in `ServerCore.onEnable()`.
- Files: `src/main/java/net/axther/serverCore/ServerCore.java:179-182,244-275,313-316,354-357`
- Impact: If plugins load in wrong order, some features silently disable. Quest system initializes QuestManager twice if NPCs enabled (lines 252, 280).
- Fix approach: Explicit dependency declaration in plugin.yml; centralize soft-dependency detection to avoid duplication.

**HashMap-based concurrent access without synchronization:**
- Issue: Multiple managers (CosmeticManager, PetManager, HologramManager, QuestManager) use unsynchronized HashMaps/ArrayLists. 203 occurrences of HashMap/ArrayList.
- Files: `src/main/java/net/axther/serverCore/cosmetic/CosmeticManager.java:18-20`, `src/main/java/net/axther/serverCore/pet/PetManager.java:19-20`, `src/main/java/net/axther/serverCore/quest/QuestManager.java:30-32`
- Impact: Race conditions if async tasks modify collections while iteration happens (tick tasks run every 1-20 ticks); removeIf() is safe but only on main thread.
- Fix approach: Use ConcurrentHashMap/CopyOnWriteArrayList for maps accessed from async contexts; document thread safety assumptions.

## Known Bugs

**Hologram null cooldown key map entry growth:**
- Symptoms: HologramInteractListener cooldown map (`cooldowns` HashMap) never removes expired entries, grows unbounded over time.
- Files: `src/main/java/net/axther/serverCore/hologram/listener/HologramInteractListener.java:20,35-41`
- Trigger: Any hologram with click interactions; cooldown keys accumulate as players interact with different hologram/player combinations.
- Workaround: Map entries are checked for expiry so interactions work; just wastes memory. Will eventually hit OutOfMemory if server runs long enough with many players.

**Null pointer if hologram conditions evaluate with null Location:**
- Symptoms: NPE if hologram location has null world when evaluating conditions.
- Files: `src/main/java/net/axther/serverCore/hologram/visibility/HologramVisibilityTracker.java:57`
- Trigger: Config loads hologram with invalid world name; world unloads while visibility tracker evaluates.
- Workaround: Restart server to reload configs.

**Quest objectives with FETCH type check inventory on-demand during completion check:**
- Symptoms: `areObjectivesComplete()` calls `countMaterial()` which recounts items in inventory every time it's called, but incremental progress isn't tracked for FETCH objectives.
- Files: `src/main/java/net/axther/serverCore/quest/QuestManager.java:115-119`
- Trigger: Player has required items, abandons quest, drops items, accepts same quest again — progress shows 0 but items still exist.
- Workaround: Players must retain items between acceptances.

**ModelEngineHook fallback doesn't update stand if model lookup fails:**
- Symptoms: If pet profile has modelId but model not found in ModelEngine, fallback applies head item silently but may conflict with existing model attachment.
- Files: `src/main/java/net/axther/serverCore/pet/PetManager.java:76-83`
- Trigger: Misconfigured pet profile or ModelEngine missing custom model data.
- Workaround: Verify pet profiles match available models.

**NPC view tracker doesn't validate viewDistance bounds:**
- Symptoms: NPCViewTracker distance passed from config is used directly in squared distance comparisons without validation.
- Files: `src/main/java/net/axther/serverCore/npc/render/NPCViewTracker.java` (initialized in ServerCore.java:260)
- Trigger: Negative or zero view-distance in config doesn't fail; viewers set is empty so NPCs never spawn.
- Workaround: Ensure view-distance config value > 0.

## Security Considerations

**Armor stand cosmetics and pets can be manipulated if player knows stand UUIDs:**
- Risk: Cosmetic and pet armor stands are invisible/marker with equipment locks, but if player somehow obtains stand UUIDs they could be targeted for entity-specific attacks or modified by other plugins.
- Files: `src/main/java/net/axther/serverCore/cosmetic/CosmeticManager.java:48-60`, `src/main/java/net/axther/serverCore/pet/PetManager.java:57-71`
- Current mitigation: UUIDs are internal and not exposed to API; armor stands are marked non-persistent.
- Recommendations: Store stand UUIDs in a weak hash map or time-based cache to prevent unbounded UUID index growth; validate armor stand ownership in lifecycle listener.

**NPC skin texture loaded from config without validation:**
- Risk: NPCSkin texture/signature fields accept arbitrary strings which are sent to all clients without validation. Malformed Base64 could cause client issues.
- Files: `src/main/java/net/axther/serverCore/npc/render/NPCRenderer.java:41-45`
- Current mitigation: TextureProperty fields are passed directly to PacketEvents library which likely validates.
- Recommendations: Validate texture string format (Base64) and signature before using; log warnings for invalid skins.

**Hologram action execution not permission-checked:**
- Risk: Any player can click holograms and trigger actions (commands, quests, etc.) unless action itself has permission check.
- Files: `src/main/java/net/axther/serverCore/hologram/listener/HologramInteractListener.java:47-49`
- Current mitigation: Individual HologramAction implementations may check permissions.
- Recommendations: Add optional permission-per-action in config; document security model in action interface.

**Quest completion reward gives items directly without validation:**
- Risk: Quest rewards bypass normal inventory mechanics. Overflow items could drop uncontrolled or be lost silently.
- Files: `src/main/java/net/axther/serverCore/quest/QuestManager.java:146,155`
- Current mitigation: Uses standard addItem() which drops overflow.
- Recommendations: Warn if inventory is full; allow quest reward customization hooks.

## Performance Bottlenecks

**HologramVisibilityTracker evaluates ALL holograms every update cycle:**
- Problem: O(n*m) where n=holograms, m=players. Iterates all holograms then all players for each tick that matches update interval.
- Files: `src/main/java/net/axther/serverCore/hologram/visibility/HologramVisibilityTracker.java:29-56`
- Cause: No spatial indexing; brute-force linear scan of holograms.
- Improvement path: Chunk-based spatial index (leverage hologram chunk keys that already exist); only evaluate holograms in chunks near players.

**ReactiveManager re-evaluates all rules for all online players every 20 ticks:**
- Problem: O(n*m) where n=rules, m=players with effects. On large servers with many rules, this compounds with cosmetic/pet lookups.
- Files: `src/main/java/net/axther/serverCore/reactive/ReactiveManager.java:51-84`
- Cause: No caching of condition results; full re-evaluation on every tick. Line 107 does distance check squared but accumulates for all cosmetics in world.
- Improvement path: Cache condition results per player; invalidate only when player moves or relevant state changes; use bounding-box filters before distance checks.

**CosmeticStore and PetStore save on every apply/summon without debouncing:**
- Problem: YAML file I/O is slow. Every cosmetic apply or pet summon triggers immediate save.
- Files: `src/main/java/net/axther/serverCore/cosmetic/CosmeticManager.java:68-70`, `src/main/java/net/axther/serverCore/cosmetic/data/CosmeticStore.java:30-77`
- Cause: No batching or debounce; synchronous I/O blocks main thread.
- Improvement path: Implement dirty-flag with periodic batch writes (e.g., save once per 5s if dirty); use async I/O.

**Quest.handleExplore() splits and parses strings on every objective check:**
- Problem: `obj.getTarget().split(",")` allocates new array on every tick for every active quest.
- Files: `src/main/java/net/axther/serverCore/quest/QuestManager.java:337-347`
- Cause: No pre-parsing of coordinate strings during quest load.
- Improvement path: Pre-parse explore target coordinates into Location objects during QuestConfig load.

**NPCRenderer delayed despawn creates task overhead:**
- Problem: Every NPC spawn schedules a delayed task (2 ticks later) to hide from tab list. Many NPCs = many pending tasks.
- Files: `src/main/java/net/axther/serverCore/npc/render/NPCRenderer.java:80-88`
- Cause: Packet scheduling overhead; no batching.
- Improvement path: Collect pending removals and process batch once per tick instead of per-NPC task.

## Fragile Areas

**NPCManager.init() must be called before use, but initialization is deferred and optional:**
- Files: `src/main/java/net/axther/serverCore/npc/NPCManager.java:19-22`, `src/main/java/net/axther/serverCore/ServerCore.java:248-260`
- Why fragile: If NPC system disabled but code tries to use NPCManager, renderer/viewTracker will be null. No defensive null checks in tickAll() (line 60 only checks renderer).
- Safe modification: Make NPCManager always initialize with default renderer/tracker; throw IllegalStateException if used before init with explicit error message.
- Test coverage: No unit tests visible for NPCManager tick cycle.

**Hologram chunk-key computation matches listeners but can diverge:**
- Files: `src/main/java/net/axther/serverCore/hologram/Hologram.java` (not read, but logic depends on getChunkKey())
- Why fragile: Same chunk-key formula appears in HologramLifecycleListener lines 27, 39. If one changes, other breaks silently.
- Safe modification: Extract to static utility method; use in both places.
- Test coverage: No visible tests for chunk-key computation.

**QuestManager's sequential objective logic only checks first-incomplete index:**
- Files: `src/main/java/net/axther/serverCore/quest/QuestManager.java:229,278,334`
- Why fragile: Three separate places call getFirstIncompleteIndex(). If logic differs or changes, quest progression breaks.
- Safe modification: Extract to QuestProgress method; ensure single source of truth.
- Test coverage: Quest progression is core functionality; no visible test coverage.

**CosmeticLifecycleListener depends on exact event firing order:**
- Files: `src/main/java/net/axther/serverCore/cosmetic/listener/CosmeticLifecycleListener.java`
- Why fragile: Entity death and chunk unload both trigger cleanup. If events fire in unexpected order, double-removal or leak possible.
- Safe modification: Use Set-based tracking for cleanup; idempotent removal (remove check before destroy).
- Test coverage: Lifecycle is critical; no visible tests.

## Scaling Limits

**Stand UUID index maps can grow unbounded for long-running servers:**
- Current: Cosmetics have standIndex (UUID->CosmeticInstance), Pets have standIndex (UUID->PetInstance)
- Limit: No cleanup of dead entity UUIDs; if server runs weeks, index accumulates thousands of dead UUIDs.
- Scaling path: Implement weak reference map or TTL-based cleanup (e.g., purge stand UUIDs that haven't been accessed in 1 hour).

**Hologram cooldown map unbounded growth (see Known Bugs section):**
- Current: Map grows with every unique player+hologram pair.
- Limit: After weeks of gameplay with hundreds of players, map has millions of entries.
- Scaling path: Implement periodic cleanup (e.g., every 10 minutes, remove expired entries); or use ConcurrentHashMap with custom cleanup.

**NPC entity ID management:**
- Current: Assigned sequentially in NPCManager but no circular reuse; entity IDs only increase.
- Limit: Minecraft entity IDs are 32-bit; after millions of NPC spawns (across server lifetime), may overflow.
- Scaling path: Implement entity ID pool with reuse; track freed IDs when NPCs despawn.

**Hologram spawnAll() creates entities synchronously on enable:**
- Current: Single thread creates all hologram entities on plugin load (ServerCore.java:239).
- Limit: If 1000+ holograms exist, onEnable() blocks for several seconds; players see lag spike on reload.
- Scaling path: Spawn holograms asynchronously in batches per tick; show loading indicator.

## Dependencies at Risk

**PacketEvents dependency (soft) is critical for NPC system:**
- Risk: NPC system doesn't work without PacketEvents; if PacketEvents updates with breaking API changes, entire NPC subsystem fails.
- Impact: NPCs silently disabled if plugin not present; no fallback renderer.
- Migration plan: Implement optional NPC rendering interface; provide HeadRotation packet fallback if PacketEvents unavailable.

**PlaceholderAPI reflection coupling:**
- Risk: Caching Method object via reflection is fragile; version mismatch breaks silently with exception swallowing.
- Impact: Placeholder conditions always evaluate false if PAPI version mismatches.
- Migration plan: Use adventure's adventure-platform-bukkit or similar stable API layer instead of raw reflection.

**ModelEngine integration untested for version mismatch:**
- Risk: ModelEngineHook assumes specific ModelEngine API; no version checks.
- Impact: Pets fail to render with cryptic silent fallback if ModelEngine API changes.
- Migration plan: Version API with interface; handle ModelEngine-not-present case in tests.

## Missing Critical Features

**No reload support (hot-reload):**
- Problem: Config changes (cosmetics, NPCs, quests) require full server restart. No `/servercore reload` command.
- Blocks: Server operators can't update content without downtime.
- Workaround: Manual stop/start cycles.

**No data migration/versioning:**
- Problem: YAML file formats (cosmetic-data.yml, quest-data.yml, etc.) have no version field. Schema changes break old data silently.
- Blocks: Can't add new fields to cosmetics or quests without losing existing player data.
- Workaround: Manual editing of YAML files or accepting data loss.

**No event cancellation cleanup:**
- Problem: If CosmeticApplyEvent is cancelled, stand is spawned but no longer tracked. Orphan entities leak.
- Blocks: Plugins that cancel cosmetic events create invisible entity leaks.
- Workaround: Ensure downstream plugins don't cancel events.

**No built-in admin commands for debugging:**
- Problem: No `/servercore debug` or hologram/cosmetic info commands. Hard to diagnose state issues.
- Blocks: Server operators must use in-game commands to test; no programmatic debugging.
- Workaround: None; manual investigation in code.

## Test Coverage Gaps

**Untested: Core lifecycle (enable/disable) under failure conditions:**
- What's not tested: What happens if CosmeticConfig load fails? What if NPCConfig can't find quest manager? What if data file is corrupted?
- Files: `src/main/java/net/axther/serverCore/ServerCore.java`, `src/main/java/net/axther/serverCore/cosmetic/config/CosmeticConfig.java`
- Risk: Plugin might partially initialize leaving orphan listeners or managers; onDisable doesn't clean up fully.
- Priority: High

**Untested: Quest progression with sequential objectives:**
- What's not tested: Does progress actually enforce sequential order? What if player completes out of order? What if quest resets mid-sequence?
- Files: `src/main/java/net/axther/serverCore/quest/QuestManager.java:229,249-254`
- Risk: Quest system silently accepts invalid progression states; players can softlock quests.
- Priority: High

**Untested: Concurrent modifications to active cosmetics/pets during tick:**
- What's not tested: If entity dies mid-tick while tick task is iterating, does removeIf() safely handle it?
- Files: `src/main/java/net/axther/serverCore/cosmetic/CosmeticManager.java:91-107`, `src/main/java/net/axther/serverCore/pet/PetManager.java:112-128`
- Risk: Occasional ConcurrentModificationException if entity dies at exact tick.
- Priority: Medium

**Untested: Hologram visibility condition logic with null values:**
- What's not tested: What if quest manager returns null? What if placeholder API returns null? Edge cases in min/max/equals checks?
- Files: `src/main/java/net/axther/serverCore/hologram/visibility/HologramVisibilityTracker.java:58-121`
- Risk: NPE or incorrect visibility if conditions misconfigured.
- Priority: Medium

**Untested: NPC packet rendering with disconnecting players:**
- What's not tested: What if player disconnects while spawn packet is queued? What if despawn fails?
- Files: `src/main/java/net/axther/serverCore/npc/render/NPCRenderer.java`
- Risk: Delayed tasks reference offline players; potential memory leak of player references.
- Priority: Medium

---

*Concerns audit: 2026-03-21*
