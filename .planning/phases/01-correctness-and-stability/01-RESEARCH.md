# Phase 1: Correctness and Stability - Research

**Researched:** 2026-03-21
**Domain:** Paper 1.21 plugin hardening — null safety, lifecycle idempotency, config validation, soft-dep logging
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Error handling in tick loops**
- D-01: Each instance tick in `tickAll()` loops gets its own try-catch. A failed instance logs a WARNING with the instance UUID and exception, then returns false (triggering cleanup). One bad instance must never kill the tick loop for all remaining instances.
- D-02: The catch should log at WARNING level (not SEVERE) since individual instance failures are expected during edge cases like entity death mid-tick.

**Config validation behavior**
- D-03: Invalid config values are clamped to sensible defaults with a WARNING log naming the specific config key, the invalid value, and the default being used. Systems are NOT disabled for bad config — they use the clamped value.
- D-04: NPC view-distance is clamped to minimum 1 (and maximum 256 for sanity). Zero or negative values are clamped and logged.
- D-05: World existence is checked at config load time. Invalid world names log a WARNING and skip that entry (hologram, NPC, emitter) rather than creating an entity with a null world.

**Lifecycle idempotency pattern**
- D-06: `onDisable()` uses null guards on each field (`if (tickTask != null) tickTask.cancel()`) before cleanup. No new initialized-flag tracking needed — null check is sufficient since fields are initialized to null.
- D-07: Lifecycle listeners (death, chunk unload) use `remove()` return value or containment check before `destroy()`. Double-fire of the same entity UUID is a no-op, not an exception.

**Soft dependency centralization**
- D-08: Soft dependency detection stays in `onEnable()` (not extracted to a separate class — the current pattern is safe). The improvement is adding INFO-level log lines for each present/absent dependency: `"[ServerCore] PacketEvents detected — NPC system enabled"` and `"[ServerCore] PacketEvents not found — NPC system disabled"`.

**QuestManager initialization**
- D-09: The existing null-guard at line 279 (`if (questManager == null)`) prevents true double-initialization. The fix is documentation (a comment explaining the initialization order) and adding a log line when QuestManager is created early for NPC inline quests.

**Scope adjustment from codebase scout**
- D-10: CORR-01 (event cancellation orphan fix) — codebase scout confirmed CosmeticApplyEvent is already fired BEFORE armor stand spawn. This requirement needs VERIFICATION only, not a fix. Verify the same pattern in PetSummonEvent and any other cancellable events.
- D-11: LIFE-01 (task cancellation in onDisable) — already implemented correctly for all 8 tick tasks. Needs verification only.
- D-12: LIFE-04 (synchronous data flush) — already implemented for all 5 stores. Needs verification only.

### Claude's Discretion
- Exact try-catch structure in tickAll() (wrap individual `instance.tick()` vs. wrap the removeIf predicate)
- Which specific config keys need bounds validation beyond view-distance (discover during implementation)
- Whether to add @Nullable annotations to entity reference getters

### Deferred Ideas (OUT OF SCOPE)
None — discussion stayed within phase scope
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| CORR-01 | Cosmetic armor stand not spawned until CosmeticApplyEvent cancellation check passes | **VERIFIED CORRECT** — event fired at line 41-45 of CosmeticManager.java before stand spawn at line 48. PetSummonEvent also verified correct at PetManager:48-53. No code change needed; verification task only. |
| CORR-02 | All entity access follows canonical null-check order: `entity != null` → `entity.isDead()` → `entity.getWorld() != null` | CosmeticInstance.tick() already uses correct order (line 45). PetInstance.tick() checks `owner == null \|\| !owner.isOnline() \|\| stand == null \|\| stand.isDead()` — correct. Need world-null guard added to tick() paths for mob/stand. Systematic grep audit across all 9 systems. |
| CORR-03 | All tick task `tickAll()` loops use try-catch per instance | No try-catch currently exists in CosmeticManager.tickAll() (lines 96-102) or any other manager. Wrap `instance.tick()` call in try-catch inside the removeIf predicate. Apply to all 9 systems with tick tasks. |
| CORR-04 | QuestManager initialized exactly once | **EXISTING GUARD** — line 279 `if (questManager == null)` already prevents overwrite. Fix is documentation comment + INFO log. No behavior change. |
| LIFE-01 | All BukkitRunnable tick tasks cancelled in `onDisable()` | **VERIFIED CORRECT** — onDisable() lines 425-488 cancel all 8 tick tasks with null guards. Verification task only. |
| LIFE-02 | `onDisable()` cleanup is idempotent — safe for partial-init | Null guards exist on most blocks but are inconsistently placed. Some blocks check the manager but not the task, or vice versa. Need to verify every cleanup block follows the D-06 pattern. |
| LIFE-03 | All lifecycle listeners handle double-fire safely | CosmeticLifecycleListener uses `hasCosmetics()` guard before `removeCosmetics()` (already idempotent). PetLifecycleListener calls `dismissAll()` directly without guard — need to verify `dismissAll()` is safe for unknown UUID. Need to check all other lifecycle listeners. |
| LIFE-04 | `onDisable()` performs synchronous final data flush for all stores | **VERIFIED CORRECT** — cosmeticStore.save(), petStore.save(), questStore.save() all called synchronously in onDisable(). Verification task only. |
| CONF-01 | NPC view-distance config value validated to minimum 1 at load time | NPCViewTracker constructor takes raw int with no validation. Fix: add `Math.max(1, Math.min(256, viewDistance))` in `ServerCore.java:259` where `getNpcViewDistance()` is called, with WARNING log if value was outside bounds. |
| CONF-03 | Config values with numeric bounds validated at load time | Requires discovery during implementation — identify all numeric config keys with valid ranges. The `getConfig().getInt()` calls with defaults are the right pattern but lack bounds checking. Primary targets: NPC view-distance, hologram view-distance, pet/cosmetic counts. |
| CONF-04 | Soft dependency detection logs each present/absent dependency at INFO | PacketEvents absence already logs WARNING (line 246). ModelEngine presence logs INFO (line 181). PlaceholderAPI presence logs INFO (line 356). Vault presence logs INFO (lines 418-419). Gap: no log when ModelEngine is absent, Vault is absent, or PlaceholderAPI is absent. Add absent-dependency INFO logs to match present-dependency pattern. |
</phase_requirements>

---

## Summary

Phase 1 is a targeted correctness and stability hardening pass, not a feature addition. The codebase has a well-structured Manager/Instance/Tick/Listener/Store architecture that is sound and must not be restructured. Direct source inspection reveals that several requirements from the planning discussion are already correctly implemented (CORR-01, LIFE-01, LIFE-04) and need verification only, not code changes. The real work is: (1) adding per-instance try-catch to all tickAll() loops across 9 systems, (2) auditing and enforcing the canonical null/dead/world-null check order across all instance tick() methods, (3) adding world-existence validation at config load time, (4) clamping NPC view-distance, and (5) adding absent-dependency INFO logs.

The QuestManager double-initialization concern (CORR-04) is already prevented by the null-guard at line 279 — the fix is documentation and a log line explaining WHY the guard exists. This is the most important "don't fix what isn't broken" finding.

**Primary recommendation:** Work system by system. Verify the three already-correct requirements first (document findings in code comments), then add try-catch to all tickAll() loops, then do the null-check order audit, then config validation, then logging gaps.

## Standard Stack

### Core (locked, no changes)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Paper API | 1.21 | Server plugin API | Project constraint — Paper API only, no NMS |
| Java | 21 | Language runtime | Project constraint |
| Gradle | 8.8 | Build system | Project constraint |

### No New Libraries Required
This phase introduces no new dependencies. All fixes use:
- `java.util.logging.Logger` (via `getLogger()` from JavaPlugin) — already available
- Standard Java try-catch
- Existing Paper API null-check patterns already in use

**Version verification:** Not applicable — no new libraries.

## Architecture Patterns

### Existing Pattern 1: Event-Before-Spawn (already correct in cosmetics and pets)

CosmeticManager.java lines 41-48 — event fired, cancel checked, THEN stand spawned:
```java
// Source: CosmeticManager.java:41-48 (verified 2026-03-21)
CosmeticApplyEvent event = new CosmeticApplyEvent(mob, item);
Bukkit.getPluginManager().callEvent(event);
if (event.isCancelled()) {
    return false;
}
// Stand only spawned here — after cancel check
Location standLoc = profile.computeStandLocation(mob);
ArmorStand stand = mob.getWorld().spawn(standLoc, ArmorStand.class, s -> { ... });
```

PetManager.java lines 48-53 — same correct pattern for PetSummonEvent.

**Action:** Verify only — no code change needed. Leave a comment: `// Event fired before spawn to prevent orphan entities — do not reorder`.

### Existing Pattern 2: Canonical Null-Check Order (already correct in CosmeticInstance)

CosmeticInstance.java line 45 is the canonical pattern to extend to all systems:
```java
// Source: CosmeticInstance.java:45 (verified 2026-03-21)
if (mob == null || mob.isDead() || stand == null || stand.isDead()) {
    destroy();
    return false;
}
```

This is the canonical form. Missing from this pattern in the codebase: `getWorld() != null` check. The extended canonical pattern for all instance tick() methods:
```java
if (mob == null || mob.isDead() || mob.getWorld() == null) {
    destroy();
    return false;
}
if (stand == null || stand.isDead() || stand.getWorld() == null) {
    destroy();
    return false;
}
```

### Pattern 3: Per-Instance Try-Catch in tickAll() (to be added)

The decision (D-01) wraps `instance.tick()` inside the removeIf predicate. The cleanest structure:
```java
// Apply this pattern to all 9 systems' tickAll() methods
instances.removeIf(instance -> {
    try {
        if (!instance.tick()) {
            standIndex.remove(instance.getStandUuid());
            return true;
        }
        return false;
    } catch (Exception e) {
        getLogger().warning("[ServerCore] Exception ticking instance "
            + instance.getMobUuid() + ": " + e.getMessage());
        standIndex.remove(instance.getStandUuid());
        instance.destroy();
        return true;
    }
});
```

Note: managers do not have access to `getLogger()` directly. The logger must be passed to the manager constructor or the try-catch must be in the TickTask. The cleanest approach without breaking existing manager constructor signatures: pass `java.util.logging.Logger` to each manager at construction in `onEnable()`.

Alternative (lower-risk, no constructor changes): wrap in the TickTask's `run()` method per-call. But this is less precise — it catches the whole tickAll() not individual instances. The decision (D-01) requires per-instance try-catch, so logger injection into managers is required.

### Pattern 4: Guard-Before-Cleanup in Lifecycle Listeners (already correct in cosmetics)

CosmeticLifecycleListener uses containment check before calling remove:
```java
// Source: CosmeticLifecycleListener.java:31-33 (verified 2026-03-21)
if (manager.hasCosmetics(entity.getUniqueId())) {
    manager.removeCosmetics(entity.getUniqueId());
}
```

This is the correct pattern (D-07). Verify all other lifecycle listeners follow the same pattern.

### Pattern 5: onDisable() Null Guards (already largely correct)

ServerCore.java onDisable() (lines 422-488) already uses null guards consistently. Key observation: the pattern is correct. The verification task is to confirm NO gap exists in partial-init safety (e.g., if cosmetics system fails to load, are any subsequent cleanup blocks that reference cosmeticManager still safe?).

### Pattern 6: Config Validation with Clamping (to be added)

```java
// Apply this pattern for all bounded numeric config values
// Source: research/ARCHITECTURE.md Pattern 2
int viewDistance = serverCoreConfig.getNpcViewDistance();
if (viewDistance <= 0 || viewDistance > 256) {
    getLogger().warning("[ServerCore] systems.npcs.view-distance value '"
        + viewDistance + "' is out of bounds [1-256], clamping to 48");
    viewDistance = 48;
}
```

### Pattern 7: Absent-Dependency INFO Logging (gap to fill)

Current behavior for each soft dep:
- ModelEngine: present → INFO logged; absent → NOTHING logged
- PlaceholderAPI: present → INFO logged; absent → NOTHING logged
- Vault: present → INFO logged; absent → NOTHING logged
- PacketEvents: present → INFO logged; absent → WARNING logged (already correct)

Add absent-dependency INFO log for the first three. PacketEvents absent already logs WARNING — do not downgrade to INFO since it disables a configured system.

```java
// Pattern (apply to ModelEngine, PlaceholderAPI, Vault absent cases)
if (!megEnabled) {
    getLogger().info("[ServerCore] ModelEngine not found — pet models disabled, using head items");
}
```

### Anti-Patterns to Avoid

- **Do NOT replace HashMap with ConcurrentHashMap** — all Bukkit handlers and tick tasks run on main thread. ConcurrentHashMap does not prevent CME within a single thread. The removeIf() pattern is already the correct single-threaded solution.
- **Do NOT add initialized-flag booleans** — D-06 specifies null-check is sufficient. Adding initialized flags creates a second source of truth.
- **Do NOT restructure Manager/Instance boundaries** — hardening fixes must be additive within existing layers.
- **Do NOT wrap the whole tickAll() in try-catch** — D-01 requires per-instance catch so one failure doesn't abort the loop.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Null-safe entity lookup | Custom wrapper class | Direct null/dead/world checks in tick() | Existing pattern already works; no wrapper needed |
| Config bounds validation | External validation library | Inline `Math.max`/`Math.min` with getLogger().warning() | Already the pattern used in similar Paper plugins; no dependency needed |
| Idempotent cleanup | Cleanup registry with tracked states | `Map.remove()` return-value check | Standard Java; already used in removeCosmetics() |

**Key insight:** This phase is purely additive defensive code. No new abstractions are needed.

## Runtime State Inventory

> Not applicable — this is a hardening pass on existing code, not a rename/refactor/migration. No runtime state is renamed or migrated.

## Common Pitfalls

### Pitfall 1: Assuming CORR-01 Needs a Fix
**What goes wrong:** Implementer greps for "armor stand spawn" before looking at the actual event ordering, assumes the pattern is wrong, and reorders code that is already correct — introducing a regression.
**Why it happens:** The CONCERNS.md entry describes it as a bug. Direct source inspection revealed it was already fixed before this pass.
**How to avoid:** READ CosmeticManager.applyCosmetic() lines 41-48. The event fires at line 41, isCancelled() is checked at line 43, stand spawns at line 48. This is correct. Leave a comment documenting correctness, do not change the logic.
**Warning signs:** If implementation plan says "move CosmeticApplyEvent to before the spawn" — that is already the case.

### Pitfall 2: Logger Not Available in Manager Classes
**What goes wrong:** Adding try-catch with `getLogger().warning(...)` inside CosmeticManager fails to compile because managers do not extend JavaPlugin.
**Why it happens:** Managers are plain Java classes, not plugin classes. `getLogger()` is only on JavaPlugin.
**How to avoid:** Pass `java.util.logging.Logger` to each manager that needs it. In ServerCore.onEnable(): `cosmeticManager = new CosmeticManager(getLogger())`. Store as `private final Logger logger` in the manager. All existing managers currently have no logger field — add it in the constructor during the CORR-03 task.
**Warning signs:** Compilation error: "cannot find symbol: method getLogger()".

### Pitfall 3: onDisable() Order — Tasks Must Cancel Before Clear
**What goes wrong:** Clearing manager state (destroyAll(), clear()) before cancelling the tick task means the task may fire once more after the clear, hitting empty maps or null references.
**Why it happens:** Instinct to "clean up then cancel". The correct order is cancel first, then clean.
**How to avoid:** The current onDisable() already cancels tick tasks BEFORE calling destroyAll() — verify this remains true for all 9 systems. Example: `tickTask.cancel()` at line 425 before `cosmeticManager.destroyAll()` at line 432.
**Warning signs:** Console shows tick task exceptions on server shutdown after a manager is already cleared.

### Pitfall 4: Double-Fire Idempotency Is Already Implemented for Cosmetics — Don't Assume Others Are
**What goes wrong:** Implementer checks CosmeticLifecycleListener (which uses hasCosmetics guard), concludes all lifecycle listeners are idempotent, and skips auditing the others.
**Why it happens:** Correct pattern in the first system creates false confidence about the others.
**How to avoid:** Verify PetLifecycleListener, EmitterLifecycleListener, HologramLifecycleListener, and NPCListener independently. PetLifecycleListener.onEntitiesUnload() calls `removeStandFromIndex()` without checking if the UUID is tracked — verify that method is safe for unknown UUIDs.
**Warning signs:** NPE or IllegalStateException in PetLifecycleListener during high-traffic chunk unloads.

### Pitfall 5: World-Null Guard in HologramVisibilityTracker Update Path
**What goes wrong:** HologramVisibilityTracker.update() at line 42 calls `player.getLocation().distanceSquared(holoLoc)`. If `holoLoc.getWorld()` is null (hologram configured with invalid world name, or world unloaded), Paper's distanceSquared() throws IllegalArgumentException about world mismatch.
**Why it happens:** Line 41 checks `player.getWorld().getName().equals(hologram.getWorldName())` but this uses `getWorldName()` (a String) not `getWorld()` on the Location object. The Location's world reference may be null even if the name matches.
**How to avoid:** Add `if (holoLoc == null || holoLoc.getWorld() == null) continue;` before line 42 in the update() loop.
**Warning signs:** Console shows IllegalArgumentException from HologramVisibilityTracker referencing distanceSquared when custom worlds are configured.

### Pitfall 6: QuestManager getFirstIncompleteIndex() Has Only ONE Call Site Pattern
**What goes wrong:** CONCERNS.md says three places call getFirstIncompleteIndex(). Reading the actual source: the private method `getFirstIncompleteIndex(QuestProgress, List)` exists at line 249 and is called at lines 229, 278, 334. These are call sites of the SAME method — not duplicate inline logic. The extraction (CORR-05) is a Phase 2 concern about moving this method to QuestProgress. CORR-04 (Phase 1) is only about the double-initialization guard.
**Why it happens:** CONCERNS.md describes the fragility; the actual code has already centralized the logic in one private method. The remaining issue is that the method is on QuestManager, not QuestProgress (Phase 2).
**How to avoid:** CORR-04 work = documentation + log line at the null-guard on line 279. Do not move getFirstIncompleteIndex() to QuestProgress in Phase 1 — that is Phase 2 scope.

## Code Examples

### CORR-03: try-catch pattern for tickAll() (apply to all 9 systems)

The CosmeticManager.tickAll() currently at lines 91-107:
```java
// Current (no try-catch):
instances.removeIf(instance -> {
    if (!instance.tick()) {
        standIndex.remove(instance.getStandUuid());
        return true;
    }
    return false;
});

// After fix (with try-catch, logger injected in constructor):
instances.removeIf(instance -> {
    try {
        if (!instance.tick()) {
            standIndex.remove(instance.getStandUuid());
            return true;
        }
        return false;
    } catch (Exception e) {
        logger.warning("[ServerCore] CosmeticInstance tick failed for mob UUID "
            + instance.getMobUuid() + ": " + e.getMessage());
        standIndex.remove(instance.getStandUuid());
        try { instance.destroy(); } catch (Exception ignored) {}
        return true;
    }
});
```

### CONF-01: NPC view-distance clamping in ServerCore.onEnable()

```java
// ServerCore.java around line 259 — current:
int viewDistance = serverCoreConfig.getNpcViewDistance();

// After fix:
int rawViewDistance = serverCoreConfig.getNpcViewDistance();
int viewDistance;
if (rawViewDistance < 1 || rawViewDistance > 256) {
    getLogger().warning("[ServerCore] systems.npcs.view-distance value '"
        + rawViewDistance + "' is out of bounds [1-256], clamping to 48");
    viewDistance = 48;
} else {
    viewDistance = rawViewDistance;
}
```

### CORR-02: World-null guard addition to CosmeticInstance.tick()

```java
// Current (CosmeticInstance.java:45):
if (mob == null || mob.isDead() || stand == null || stand.isDead()) {
    destroy();
    return false;
}

// After fix (add world checks):
if (mob == null || mob.isDead() || mob.getWorld() == null
        || stand == null || stand.isDead() || stand.getWorld() == null) {
    destroy();
    return false;
}
```

### CONF-04: Absent-dependency INFO logs in ServerCore.onEnable()

```java
// ModelEngine absent (add after line 183 where megEnabled is evaluated):
if (!megEnabled) {
    getLogger().info("[ServerCore] ModelEngine not found — pet models disabled");
}

// PlaceholderAPI absent (add else branch after line 354):
if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
    registerPlaceholderHook();
    getLogger().info("[ServerCore] PlaceholderAPI detected — placeholders registered");
} else {
    getLogger().info("[ServerCore] PlaceholderAPI not found — placeholders disabled");
}

// Vault absent (add in quest system block, after setupVault call):
if (vaultPresent) {
    setupVault();
} else {
    getLogger().info("[ServerCore] Vault not found — economy rewards disabled");
}
```

### CORR-04: QuestManager initialization guard documentation

```java
// ServerCore.java line 279 — add comment explaining initialization order:
// QuestManager may have been created early in the NPC block (line 252) to support
// inline NPC quests. The null guard here prevents double-initialization, which would
// overwrite any quests already registered by npcConfig.loadAll().
if (questManager == null) {
    getLogger().info("[ServerCore] Initializing QuestManager (standalone quest system)");
    questManager = new QuestManager();
}
// If questManager was already created by the NPC block:
// (no log needed here — the NPC block already logged initialization)
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Checking isDead() before null | Canonical: null first, then isDead() | Best practice since Bukkit 1.16 | Prevents NPE on null entity refs |
| Firing cancellable event after entity spawn | Fire event BEFORE spawn | Paper best practice | Prevents orphan entity leaks on cancel |
| Silent config fallback | Log WARNING with key name and bad value | Standard for production plugins | Operator can diagnose config errors |

**Deprecated/outdated:**
- `entity.isDead() || entity == null` — reversed order is a known NPE source; every instance must use `entity == null || entity.isDead()`.

## Open Questions

1. **Which additional numeric config keys need bounds validation beyond NPC view-distance?**
   - What we know: Hologram view-distance (`systems.holograms.view-distance`, default 48.0 — double, not int), quest max-active (`systems.quests.max-active-quests`, default 0, 0 = unlimited). Pet/cosmetic system have no numeric bounds in config.yml.
   - What's unclear: Whether hologram view-distance needs the same treatment as NPC (per D-03, yes — any numeric bounds key needs validation).
   - Recommendation: During implementation, grep for `config.getInt` and `config.getDouble` across all system init blocks in ServerCore.onEnable(). Apply bounds validation wherever a documented minimum or maximum exists.

2. **Does PetManager.dismissAll() safely handle unknown UUIDs?**
   - What we know: PetLifecycleListener.onPlayerQuit() calls `manager.dismissAll(event.getPlayer().getUniqueId())` without a hasActivePets() guard.
   - What's unclear: Whether dismissAll() is idempotent for a UUID with no active pets. Must read PetManager.dismissAll() implementation.
   - Recommendation: Read the method during implementation; if it does `activePets.remove(uuid)` and handles null return, it is safe. If it iterates without null check, add the guard.

3. **Do EmitterManager and TimelineManager have tickAll() patterns that need try-catch?**
   - What we know: Both have tick tasks. EmitterTickTask and TimelineTickTask exist per STRUCTURE.md.
   - What's unclear: Whether emitter/timeline instances use the removeIf pattern or a simple for-loop. Must read both managers.
   - Recommendation: Read EmitterManager and TimelineManager source during implementation. If they use instance-based ticking with removeIf, apply the same try-catch pattern.

## Sources

### Primary (HIGH confidence)
- Direct source inspection of CosmeticManager.java, CosmeticInstance.java, PetInstance.java, PetManager.java, NPCManager.java, NPCViewTracker.java, ServerCore.java, QuestManager.java, CosmeticLifecycleListener.java, HologramVisibilityTracker.java, HologramInteractListener.java, PetLifecycleListener.java, ServerCoreConfig.java — all files read verbatim 2026-03-21
- `.planning/codebase/CONCERNS.md` — confirmed bugs with file/line references
- `.planning/research/SUMMARY.md` — project-level research (HIGH confidence overall)
- `.planning/research/PITFALLS.md` — pitfall catalog (HIGH confidence, grounded in confirmed bugs)
- `.planning/research/ARCHITECTURE.md` — fix ordering and patterns (HIGH confidence)
- `.planning/phases/01-correctness-and-stability/1-CONTEXT.md` — locked decisions and scope

### Secondary (MEDIUM confidence)
- Paper API documentation (threading model, entity lifecycle) — cited in ARCHITECTURE.md
- PaperMC GitHub issues on entity removal and chunk unload ordering — cited in PITFALLS.md

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — no new libraries; existing stack verified via source
- Architecture: HIGH — all fix locations identified to specific files and line numbers from direct source reading
- Pitfalls: HIGH — grounded in confirmed bugs from CONCERNS.md and direct source inspection
- Requirements mapping: HIGH — each requirement mapped to specific source location after reading files

**Research date:** 2026-03-21
**Valid until:** 2026-04-21 (stable domain; Paper 1.21 API is not moving fast)

---

## Key Verification Findings

The following requirements were verified as ALREADY CORRECT in source — they need verification tasks (read, comment, document) not fix tasks:

| Requirement | Status | Source Evidence |
|-------------|--------|-----------------|
| CORR-01 (event before spawn) | Already correct | CosmeticManager:41-48 — event fired, cancel checked, THEN stand spawned. PetManager:48-53 — same pattern. |
| LIFE-01 (task cancellation) | Already correct | ServerCore:425,437,447,455,464,473,479,485 — all 8 tick tasks cancelled with null guards before manager cleanup. |
| LIFE-04 (synchronous flush) | Already correct | ServerCore:429,444,461 — cosmeticStore, petStore, questStore all saved synchronously before destroy. |
| CORR-04 (QuestManager double-init) | Already guarded | ServerCore:279 — `if (questManager == null)` guard prevents second init. Needs comment + log only. |

The real implementation work is:
1. **CORR-03** — add try-catch to all tickAll() loops (requires Logger injection into all managers)
2. **CORR-02** — add world-null guards to instance tick() methods + systematic null-check order audit
3. **LIFE-02/LIFE-03** — verify onDisable partial-init safety and lifecycle listener idempotency for all 9 systems
4. **CONF-01** — NPC view-distance bounds clamp with log
5. **CONF-03** — discover and validate all bounded numeric config keys
6. **CONF-04** — add absent-dependency INFO logs for ModelEngine, PlaceholderAPI, Vault
