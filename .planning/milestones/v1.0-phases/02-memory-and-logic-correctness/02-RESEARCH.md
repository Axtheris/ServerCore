# Phase 2: Memory and Logic Correctness - Research

**Researched:** 2026-03-21
**Domain:** Paper plugin memory eviction patterns, Java HashMap periodic cleanup, quest display consistency
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

#### Cooldown map eviction (MEM-01)
- **D-01:** Add a periodic sweep to the existing hologram tick task (or a separate low-frequency task) that removes expired entries from the `cooldowns` HashMap. Sweep every 5 minutes (6000 ticks). An entry is expired when `System.currentTimeMillis() > expiresAt`.
- **D-02:** The sweep uses `cooldowns.entrySet().removeIf(e -> now > e.getValue())` — single-line, no allocation, runs on main thread (safe for HashMap).
- **D-03:** No need to switch to ConcurrentHashMap — cooldowns are only accessed from the main thread (event handlers + tick task).

#### Stand UUID index cleanup (MEM-02)
- **D-04:** Codebase scout confirmed CosmeticManager has 4 cleanup points and PetManager has 7 cleanup points. The standIndex is already well-synchronized with activeCosmetics/activePets. No unbounded growth risk exists under normal operation.
- **D-05:** Add a periodic safety-net audit sweep (every 10 minutes) that checks each standIndex entry against `Bukkit.getEntity(uuid)` — if the entity no longer exists, remove the entry and log at FINE/DEBUG level. This catches edge cases where an entity is removed without triggering any cleanup path.
- **D-06:** The sweep runs in the existing tick task at a low frequency counter (12000 ticks = 10 minutes).

#### Hologram world-null guard (MEM-03)
- **D-07:** Phase 1 already added the world-null guard to HologramVisibilityTracker (lines 37-39). This requirement is to VERIFY it's sufficient and ensure no other code paths in the hologram system can NPE on null world. No new guard expected — verification only.

#### Quest objective centralization (CORR-05)
- **D-08:** Codebase scout confirmed `getFirstIncompleteIndex()` is already a single private helper method (line 249-254) called from 3 locations. No duplication exists. This requirement is ALREADY SATISFIED — verification comment only.

#### Quest FETCH consistency (CORR-06)
- **D-09:** The on-demand inventory check in `areObjectivesComplete()` is the CORRECT design — it ensures the player actually holds the items at completion time. This is not a bug; it's intentional.
- **D-10:** The "inconsistency" from CONCERNS.md (progress shows 0 after abandon/re-accept) is expected because FETCH objectives never write to QuestProgress — they always read live inventory. The fix is to update the quest UI/action bar display to show live inventory count for FETCH objectives instead of cached progress value (which is always 0).
- **D-11:** When `areObjectivesComplete()` checks FETCH objectives, fire a lightweight progress event so the action bar can display accurate FETCH counts. This is a display fix, not a logic fix.

### Claude's Discretion
- Exact tick counter implementation for periodic sweeps (static counter vs. scheduled task)
- Whether to combine cooldown sweep and standIndex sweep into one periodic task or keep separate
- Log level for swept entries (WARNING seems too noisy; INFO or FINE)

### Deferred Ideas (OUT OF SCOPE)
None — discussion stayed within phase scope
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| CORR-05 | Quest `getFirstIncompleteIndex()` logic extracted to single method on QuestProgress — single source of truth for objective progression | Verified in source: already a private helper at QuestManager line 249-254, called from lines 229, 278, 334. Verification comment only needed. |
| CORR-06 | Quest FETCH objective tracks consistent progress — inventory count at completion check time, not stale cached values from a prior accept | Verified design: `areObjectivesComplete()` already uses on-demand `countMaterial()`. Fix is display layer: `sendProgressBar()` in QuestListener line 153-154 already reads live inventory for FETCH — already correct. Verify no FETCH display path uses stale `progress.getProgress(i)` directly. |
| MEM-01 | HologramInteractListener cooldown map has periodic eviction — expired entries are removed, map does not grow unbounded | Confirmed bug: `cooldowns` HashMap at HologramInteractListener line 20, never cleaned. `interactListener` is a local variable in onEnable() — sweep must go inside the listener itself using an internal tick counter, or listener must expose a `sweepCooldowns()` method called from HologramTickTask. |
| MEM-02 | Cosmetic and pet stand UUID index maps purge entries for dead/despawned entities — no unbounded growth over server lifetime | Already well-guarded (CosmeticManager 4 cleanup points, PetManager 7 cleanup points). Safety-net sweep: add frequency counter in CosmeticManager.tickAll() and PetManager.tickAll() calling Bukkit.getEntity(uuid) on standIndex entries. |
| MEM-03 | Hologram visibility tracker guards against null world before distance calculations — no NPE on world unload | Already implemented in Phase 1 at HologramVisibilityTracker line 39: `if (holoLoc == null || holoLoc.getWorld() == null) continue;`. Verification comment only. |
</phase_requirements>

## Summary

Phase 2 is a targeted hardening pass with five requirements, three of which are verification-only. The two real implementation tasks are: (1) adding periodic eviction to the hologram cooldown map, and (2) adding a safety-net audit sweep to CosmeticManager and PetManager standIndex maps.

The FETCH display inconsistency (CORR-06) turns out to be already partially fixed — `sendProgressBar()` in QuestListener already reads live inventory for FETCH objectives at lines 153-154. The fix is to verify this path is correct and add a comment confirming it. The `areObjectivesComplete()` design is intentionally on-demand and correct.

The most architecturally interesting constraint: `HologramInteractListener` is registered as a local variable (`var interactListener`) in `onEnable()` and is never stored in a ServerCore field. This means the HologramTickTask cannot call `interactListener.sweepCooldowns()` without either (a) passing the listener to the task, (b) storing the listener in a ServerCore field, or (c) moving the sweep counter into the listener itself. The simplest approach is to add an internal tick counter directly inside HologramInteractListener and do the sweep on `PlayerInteractAtEntityEvent` or inside a method wired to the tick task.

**Primary recommendation:** Add the cooldown sweep counter inside `HologramInteractListener` directly. On each `onInteract` call, increment an internal counter — when it reaches 6000 ticks' worth of calls, sweep. Alternatively, expose `sweepCooldowns()` on the listener and store the listener reference in the `ServerCore` hologram block so it can be called from `HologramTickTask`. The tick-counter-in-task approach is cleaner and is how CosmeticManager/PetManager sweeps are implemented.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Paper API | 1.21 | Plugin API, BukkitRunnable, Bukkit.getEntity() | Project constraint: Paper API only |
| Java 21 | 21 | Language runtime | Project constraint |

No new library dependencies for this phase. All changes use existing Paper API primitives.

### Patterns Already in Codebase
| Pattern | Where Used | Extending To |
|---------|-----------|--------------|
| `removeIf` on entrySet | CosmeticManager.tickAll(), PetManager.tickAll() | HologramInteractListener cooldowns eviction |
| Frequency counter (tickCount % N == 0) | HologramTickTask.tickCount | CosmeticManager/PetManager standIndex audit sweep |
| `Bukkit.getEntity(uuid)` | Not yet used for sweeps | standIndex audit sweep for both managers |
| Verification comment pattern | Phase 1 — CORR-01, LIFE-01, etc. | CORR-05, MEM-03 verification comments |

## Architecture Patterns

### Pattern 1: Cooldown Map Eviction (MEM-01)

**What:** Remove expired entries from a HashMap periodically using `removeIf` on the entry set.

**When to use:** Any HashMap that accumulates entries bounded only by user interaction (player+entity combos) over server lifetime.

**Key constraint:** `HologramInteractListener` is constructed as a local variable in `ServerCore.onEnable()` at line 234 and passed to `registerEvents`. It is NOT stored as a ServerCore field. To add a tick-driven sweep, one of these must change:

Option A (Recommended — store listener as a field): Store the listener as a `private HologramInteractListener hologramInteractListener` field in ServerCore, then call `hologramInteractListener.sweepCooldowns(tickCount)` from `HologramTickTask` (after passing the listener to the task constructor). This follows the same pattern as HologramVisibilityTracker being passed to HologramTickTask.

Option B (Self-contained): Add a private int `interactCallCount` inside `HologramInteractListener` and sweep at the start of `onInteract` when `++interactCallCount % 6000 == 0`. This requires zero wiring changes but sweeps on interaction count, not tick count.

Option C (Separate BukkitRunnable): Schedule a new `runTaskTimer(plugin, 6000L, 6000L)` task that calls the listener. Requires storing the listener and the new task reference.

**Recommendation:** Option A — store `hologramInteractListener` as a ServerCore field (matching the pattern of `hologramTickTask`, `hologramManager`, etc.), pass it to `HologramTickTask`, and add a `sweepCooldowns(int tickCount)` method to the listener called when `tickCount % 6000 == 0`.

**Example — eviction method:**
```java
// In HologramInteractListener
public void sweepCooldowns(int tickCount) {
    if (tickCount % 6000 != 0) return;
    long now = System.currentTimeMillis();
    // MEM-01: Periodic eviction — removes expired cooldown entries so map does not grow unbounded.
    cooldowns.entrySet().removeIf(e -> now > e.getValue());
}
```

**Example — tick task call:**
```java
// In HologramTickTask.run()
if (interactListener != null) {
    interactListener.sweepCooldowns(tickCount);
}
```

### Pattern 2: standIndex Safety-Net Audit Sweep (MEM-02)

**What:** Periodically scan a standIndex map and evict entries where the armor stand entity no longer exists in the world.

**When to use:** After Phase 1 established all normal cleanup paths are correct. This is a belt-and-suspenders guard against edge cases (plugin conflicts, unexpected chunk events).

**Frequency:** Every 12000 ticks (10 minutes). Rationale: standIndex is well-maintained under normal operation (Phase 1 verification confirmed this). The sweep is a safety net, not the primary cleanup mechanism. At 12000 ticks, cost is negligible.

**Thread safety:** `Bukkit.getEntity(uuid)` is safe on the main thread. CosmeticManager and PetManager are main-thread-only (D-03 confirmed). No synchronization needed.

**Example — CosmeticManager:**
```java
// In CosmeticManager.tickAll() — add at top after existing removeIf block
private int sweepCounter = 0;

public void tickAll() {
    // ... existing removeIf logic ...

    // MEM-02: Safety-net audit sweep every 10 minutes for stale standIndex entries.
    if (++sweepCounter % 12000 == 0) {
        standIndex.entrySet().removeIf(e -> {
            if (Bukkit.getEntity(e.getKey()) == null) {
                logger.fine("[ServerCore] CosmeticManager audit: removed stale standIndex entry " + e.getKey());
                return true;
            }
            return false;
        });
    }
}
```

**Example — PetManager:**
```java
// Same pattern in PetManager.tickAll()
private int sweepCounter = 0;

// MEM-02: Safety-net audit sweep every 10 minutes.
if (++sweepCounter % 12000 == 0) {
    standIndex.entrySet().removeIf(e -> {
        if (Bukkit.getEntity(e.getKey()) == null) {
            logger.fine("[ServerCore] PetManager audit: removed stale standIndex entry " + e.getKey());
            return true;
        }
        return false;
    });
}
```

### Pattern 3: FETCH Display Consistency (CORR-06)

**What:** Verify that `sendProgressBar()` in QuestListener correctly shows live inventory count for FETCH objectives, not the always-zero stored progress value.

**Already implemented:** QuestListener.sendProgressBar() lines 153-154 already override `current` with `countMaterial(player, obj.getTarget())` for FETCH objectives. This is the correct fix.

**What to verify:** Confirm no other display path (e.g., command output, GUI display) reads `progress.getProgress(i)` directly for FETCH without the live override. The fix is a verification comment on lines 153-154 plus a CORR-06 comment on `areObjectivesComplete()` confirming the on-demand design is intentional.

**Example — comment to add:**
```java
// In QuestListener.sendProgressBar(), existing block at lines 153-154:
if (obj.getType() == QuestObjective.Type.FETCH) {
    // CORR-06: FETCH objectives never write to QuestProgress (always 0).
    // Read live inventory count to avoid showing stale display after abandon/re-accept.
    current = manager.countMaterial(player, obj.getTarget());
}
```

### Pattern 4: Verification-Only Comments (CORR-05, MEM-03)

**CORR-05 — getFirstIncompleteIndex() centralization:**
Already a single private method at QuestManager line 249-254. Three call sites: line 229 (incrementObjective), line 278 (handleTalk), line 334 (handleExplore). No duplication. Add comment:

```java
// CORR-05 VERIFIED: getFirstIncompleteIndex() is the single source of truth for
// sequential objective progression. Called from 3 sites; logic is not duplicated.
private int getFirstIncompleteIndex(QuestProgress progress, List<QuestObjective> objectives) {
```

**MEM-03 — Hologram world-null guard:**
Already implemented at HologramVisibilityTracker line 39:
```java
if (holoLoc == null || holoLoc.getWorld() == null) continue;
```
Phase 1 already added this with a CORR-02 comment. Add MEM-03 reference or confirm the existing comment covers it.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Cooldown expiry check | Custom TTL cache / WeakHashMap | `HashMap.entrySet().removeIf()` on main thread | removeIf is allocation-free, iterator-safe, single line |
| Stale entity detection | WeakReference tracking | `Bukkit.getEntity(uuid) == null` check | Paper keeps a global entity registry; getEntity returns null for dead/removed entities |
| Periodic sweep scheduling | New BukkitRunnable task | Frequency counter inside existing tick method | Existing tick tasks already run every tick; adding a counter avoids new task registration boilerplate |

**Key insight:** The existing tick infrastructure already provides a reliable periodic execution context. Adding a counter (`if (++sweepCounter % N == 0)`) to an existing `tickAll()` method is always preferable to scheduling a new task for low-frequency housekeeping.

## Common Pitfalls

### Pitfall 1: Sweeping standIndex Removes Active Instances
**What goes wrong:** The safety-net sweep removes a standIndex entry for an armor stand that is active but temporarily in an unloaded chunk. `Bukkit.getEntity(uuid)` returns null for entities in unloaded chunks.
**Why it happens:** Paper's `Bukkit.getEntity(uuid)` only finds entities in loaded chunks.
**How to avoid:** The sweep is intentionally a safety net for truly orphaned entries, not a primary cleanup. However, if armor stands are set non-persistent (`setPersistent(false)`) they do not survive chunk unloads — they are removed. This is already the case (confirmed in CosmeticManager.applyCosmetic() line 62: `s.setPersistent(false)`). Non-persistent entities are removed on chunk unload, so `getEntity(uuid) == null` for a standIndex entry means the entity is truly gone.
**Warning signs:** If sweep logs show many FINE entries during active gameplay (not just server-running-for-hours), the non-persistent flag may not be effective.

### Pitfall 2: Cooldown Sweep on Wrong Thread
**What goes wrong:** If `sweepCooldowns()` is ever called from an async context, HashMap corruption occurs.
**Why it happens:** HashMap is not thread-safe. removeIf during concurrent modification from another thread causes ConcurrentModificationException or silent corruption.
**How to avoid:** All calls to `sweepCooldowns()` must originate from the main server thread. HologramTickTask runs on the main thread (it's a BukkitRunnable without `runTaskAsynchronously`). Event handlers always run on the main thread in Paper. D-03 already confirms main-thread-only access.
**Warning signs:** ConcurrentModificationException stack traces mentioning HashMap.

### Pitfall 3: interactListener Not Stored → Can't Wire to Tick Task
**What goes wrong:** Developer adds `sweepCooldowns()` to `HologramInteractListener` but cannot call it from `HologramTickTask` because `interactListener` is only a local variable in `onEnable()`.
**Why it happens:** ServerCore.onEnable() line 234: `var interactListener = new HologramInteractListener(hologramManager)` — local variable, no field.
**How to avoid:** Add `private HologramInteractListener hologramInteractListener;` as a ServerCore field, assign it at line 234, pass it to `HologramTickTask` constructor. Update `onDisable()` null guard if needed (though `HologramInteractListener` has no teardown).
**Warning signs:** Compiler error "cannot find symbol: interactListener" when trying to pass it to the task.

### Pitfall 4: FETCH Display Fix Breaks Non-FETCH Objectives
**What goes wrong:** Modifying `sendProgressBar()` accidentally affects non-FETCH objective display by moving or restructuring the type-check block.
**Why it happens:** The FETCH override at lines 153-154 is already correct — it only triggers for FETCH type. Risk comes from refactoring the surrounding loop.
**How to avoid:** The fix is comment-only (or at most adding the CORR-06 comment inline). Do not restructure `sendProgressBar()`.
**Warning signs:** Non-FETCH quest objectives show incorrect counts in action bar.

### Pitfall 5: Sweep Counter Integer Overflow
**What goes wrong:** `sweepCounter` is an `int`. After 2^31 - 1 ticks (~3.4 years of continuous server runtime), it overflows to negative and `sweepCounter % 12000` may never equal 0.
**Why it happens:** Java int wraps silently.
**How to avoid:** Use `sweepCounter % 12000 == 0` which remains safe even after overflow (negative values still satisfy `n % 12000 == 0` when `n` is a multiple of 12000). Alternatively, reset counter: `if (++sweepCounter >= 12000) { sweepCounter = 0; /* sweep */ }`. The reset approach is more readable and avoids the modulo edge case.
**Warning signs:** Sweeps stop appearing in logs after extended server uptime.

## Code Examples

### Verified: Existing removeIf pattern (from CosmeticManager.java)
```java
// Source: CosmeticManager.java lines 105-119
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

### Verified: HologramInteractListener cooldown map (from source)
```java
// Source: HologramInteractListener.java lines 20, 37-41
private final Map<String, Long> cooldowns = new HashMap<>();
// ...
String cooldownKey = player.getUniqueId() + ":" + hologram.getId();
long now = System.currentTimeMillis();
Long expiresAt = cooldowns.get(cooldownKey);
if (expiresAt != null && now < expiresAt) return;
cooldowns.put(cooldownKey, now + (hologram.getClickCooldown() * 50L));
```

### Verified: Frequency counter pattern (from HologramTickTask.java)
```java
// Source: HologramTickTask.java lines 9, 18-19
private int tickCount;
// ...
tickCount++;
manager.tickAll(tickCount);
```

### Verified: HologramVisibilityTracker world-null guard (from source)
```java
// Source: HologramVisibilityTracker.java lines 37-39 (Phase 1 addition)
Location holoLoc = hologram.getLocation();
// Guard against null world — hologram may reference an invalid/unloaded world (per CORR-02)
if (holoLoc == null || holoLoc.getWorld() == null) continue;
```

### Verified: FETCH display already correct in sendProgressBar (from QuestListener.java)
```java
// Source: QuestListener.java lines 150-164
for (int i = 0; i < objectives.size(); i++) {
    QuestObjective obj = objectives.get(i);
    int current = progress.getProgress(i);
    if (obj.getType() == QuestObjective.Type.FETCH) {
        current = manager.countMaterial(player, obj.getTarget());  // live read, not stale progress
    }
    if (current < obj.getAmount()) {
        // ... action bar display
        return;
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| WeakHashMap for entity tracking | Direct UUID key + explicit cleanup points | Pre-existing design | WeakHashMap tied to GC timing, unpredictable; explicit cleanup is deterministic |
| TTL cache library (Guava CacheBuilder) | removeIf on HashMap entries | Phase 2 decision | No new dependency; same correctness; main-thread safe |
| ConcurrentHashMap for all plugin maps | HashMap with main-thread discipline (D-03) | Phase 2 decision | HashMap is faster; Paper plugins run primarily on main thread |

**Not deprecated for this project:**
- `BukkitRunnable` tick tasks remain the standard; no migration to Folia scheduler needed (Folia not in scope)

## Open Questions

1. **Log level for sweep entries**
   - What we know: D-05 says "FINE/DEBUG level". Java's `java.util.logging` uses `Level.FINE` (not `Level.DEBUG`). Paper's `getLogger()` is a `java.util.logging.Logger`.
   - What's unclear: Whether FINE-level logs are visible by default in Paper (they are not — default level is INFO). Server operators won't see sweep activity unless they configure lower log levels.
   - Recommendation: Use `logger.fine(...)` as specified. If operators need to diagnose sweep behavior, they can lower the log level. This is the correct choice — INFO-level per-sweep logging would be noisy after weeks of runtime.

2. **HologramInteractListener field promotion**
   - What we know: Local variable currently prevents wiring to tick task.
   - What's unclear: Whether the planner prefers field-promotion (Option A) or self-contained counter (Option B).
   - Recommendation: Field promotion (Option A) is more consistent with how `hologramManager`, `hologramConfig`, `hologramTickTask` are all stored as ServerCore fields. Self-contained counter (Option B) is also correct. Planner should choose based on which produces fewer changed files.

## Sources

### Primary (HIGH confidence)
- Direct source code audit — `HologramInteractListener.java`, `CosmeticManager.java`, `PetManager.java`, `QuestManager.java`, `QuestListener.java`, `HologramVisibilityTracker.java`, `HologramTickTask.java`, `QuestProgress.java`
- `.planning/phases/02-memory-and-logic-correctness/2-CONTEXT.md` — locked decisions D-01 through D-11
- `.planning/phases/01-correctness-and-stability/01-VERIFICATION.md` — Phase 1 completion evidence, confirmed world-null guard at HologramVisibilityTracker line 39
- `.planning/codebase/CONCERNS.md` — Known bugs: hologram cooldown map growth (line 33-37), stand UUID growth (line 149-152)

### Secondary (MEDIUM confidence)
- `.planning/research/SUMMARY.md` — Project-level research confirming memory eviction patterns and Phase 2 scope
- `.planning/codebase/ARCHITECTURE.md` — Manager/Instance/Tick/Listener layering confirming main-thread discipline

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — no new libraries; all patterns verified in existing codebase
- Architecture: HIGH — all key files read directly; integration points confirmed
- Pitfalls: HIGH — each pitfall is grounded in a specific code observation (local variable, non-persistent flag, thread model)

**Research date:** 2026-03-21
**Valid until:** 2026-04-21 (stable Paper 1.21 API; no expiry risk for this scope)
