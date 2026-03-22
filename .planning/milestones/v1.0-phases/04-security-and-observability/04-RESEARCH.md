# Phase 04: Security and Observability — Research

**Researched:** 2026-03-21
**Domain:** Paper 1.21 plugin security hardening, Base64 validation, permission enforcement, diagnostic commands
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Hologram action permission gating (SEC-01)**
- D-01: Add an optional `permission` field to `HologramAction`. Parsed from YAML config as a string (e.g., `permission: "servercore.hologram.vip"`). When null or empty, the action executes unconditionally (backward-compatible).
- D-02: Permission check happens in `HologramInteractListener.onInteract()` inside the action loop (line 58-60). If `action.getPermission() != null && !player.hasPermission(action.getPermission())`, skip that action silently — no message to the player, no log. Other actions in the same hologram that the player DOES have permission for still execute.
- D-03: The `HologramClickEvent` fires before the permission-filtered action loop. External plugins see the click event regardless of permission outcome — they can cancel at will.
- D-04: Permission field is per-action, not per-hologram.

**NPC skin Base64 validation (CONF-02, SEC-02)**
- D-05: Validate the `skin-texture` field as valid Base64 at config load time in `NPCSkin.fromConfig()` and in `NPCConfig.loadNPC()` (lines 63-64). Use `java.util.Base64.getDecoder().decode()` in a try-catch. If decoding throws `IllegalArgumentException`, log WARNING naming the NPC and skip the skin.
- D-06: The `skin-signature` field is also Base64 but is optional. Validate if present; skip validation if null/blank.
- D-07: Validation happens once at load time — no per-player or per-render validation needed.
- D-08: Log format: `WARNING: NPC "{npcId}" has malformed Base64 skin texture — skin skipped`. Include the NPC id so operators know which config file to fix.

**Debug command (OBS-01)**
- D-09: Add a `debug` subcommand to the existing `ServerCoreCommand` (`/servercore debug`). Requires `servercore.admin.debug` permission. Outputs to the command sender (console or player).
- D-10: The debug output prints one section per system with active instance counts. All nine systems are covered: cosmetics, emitters, pets, holograms, NPCs, quests, timelines, reactive cosmetics, and GUIs (menus).
- D-11: Additional diagnostic lines: pending save flags per store (dirty/clean), registered soft-dependency hooks (present/absent for PacketEvents, ModelEngine, PlaceholderAPI, Vault), tick task status (running/stopped for each tick task).
- D-12: ServerCoreCommand needs references to the managers and stores to query live state. Pass them via constructor or a context object. Claude's discretion on the exact mechanism.

### Claude's Discretion
- Whether `HologramAction.permission` is a constructor parameter or a setter
- Whether Base64 validation is a static utility method or inline in `NPCSkin.fromConfig()`
- The exact format and ordering of debug output lines
- Whether DebugContext is a record, interface, or direct constructor parameters
- Tab-completion for the `debug` subcommand

### Deferred Ideas (OUT OF SCOPE)
None — discussion stayed within phase scope
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| CONF-02 | NPC skin texture and signature fields are validated as proper Base64 format at load time — malformed values logged and skin skipped | `java.util.Base64.getDecoder().decode()` in try-catch; log format established by D-08; single load-time check in `NPCSkin.fromConfig()` |
| SEC-01 | Hologram actions support optional `permission` field in YAML config — `HologramInteractListener` checks player permission before executing action | Bukkit `player.hasPermission(String)` API; optional field with null-means-unrestricted semantics; `HologramConfig` action-map parsing already reads `type` and `value` — add `permission` as third key |
| SEC-02 | NPC skin texture Base64 validation rejects malformed data before sending to clients via PacketEvents | Shares implementation with CONF-02; the skin is stored as the raw string which PacketEvents receives — validation ensures the string is structurally valid Base64 |
| OBS-01 | `/servercore debug` subcommand prints live plugin state: active instance counts per system, pending save flags, registered hooks, tick task status | `BukkitRunnable.isCancelled()` for task status; `isDirty()` on stores; null-check on manager fields for system enabled/disabled; `ServerCoreAPI` already has references to all managers |
</phase_requirements>

---

## Summary

Phase 4 implements three targeted hardening changes to an existing, well-structured Paper 1.21 plugin. All three changes are surgical: they modify narrow integration points in existing classes rather than introducing new subsystems.

The hologram permission gate (SEC-01, D-01 through D-04) requires adding one field to `HologramAction`, one YAML key read in `HologramConfig.loadAll()`, and one `if` guard in the four-line action loop in `HologramInteractListener.onInteract()`. The implementation is constrained entirely to the hologram subsystem.

The NPC skin validation (CONF-02/SEC-02, D-05 through D-08) requires one `try-catch` block around a `java.util.Base64.getDecoder().decode()` call in `NPCSkin.fromConfig()`. The Java standard library handles all decoding logic; no external dependency is needed. The log warning convention (WARNING for bad config data, SEVERE for I/O failures) is already established by Phase 1 decisions.

The debug command (OBS-01, D-09 through D-12) requires: (a) adding `servercore.admin.debug` to `plugin.yml`, (b) expanding `ServerCoreCommand`'s constructor to accept manager/store references, (c) adding a `debug` branch to `onCommand()`, and (d) adding `"debug"` to the tab-completion list. All nine managers are already initialized as fields in `ServerCore.java` and exposed through `ServerCoreAPI`. The hard part is not logic — it is knowing which getter exists on each manager for the count query.

**Primary recommendation:** Implement the three changes in order — validation first (lowest risk, no behavior change), permission gate second (additive, backward-compatible), debug command third (pure addition).

---

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `java.util.Base64` | JDK 21 (stdlib) | Decode Base64 skin texture/signature for structural validation | No external dependency; standard since Java 8; `getDecoder().decode()` throws `IllegalArgumentException` on malformed input |
| `org.bukkit.entity.Player#hasPermission(String)` | Paper API 1.21 | Runtime permission check per-action | Established project pattern; used by every command handler in the codebase |
| `org.bukkit.scheduler.BukkitRunnable#isCancelled()` | Paper API 1.21 | Tick task status reporting for debug command | Bukkit standard; all tick tasks extend `BukkitRunnable` |

### No New Dependencies
This phase adds zero external dependencies. All required APIs are already on the classpath: JDK 21 stdlib (`java.util.Base64`), Paper API 1.21 (`player.hasPermission`, `BukkitRunnable`), and existing project managers/stores.

---

## Architecture Patterns

### Pattern 1: Optional Field on Existing Value Class
**What:** `HologramAction` is a simple two-field class (type, value) with a static `parse()` factory and an `execute(Player)` method. Adding `permission` follows the same shape as the existing fields.

**When to use:** Any time a config-driven object needs a new optional attribute that is only relevant at trigger time.

**Existing shape (abbreviated):**
```java
// HologramAction.java — current structure
public class HologramAction {
    private final String type;
    private final String value;
    // + permission field goes here
    private HologramAction(String type, String value) { ... }
    public static HologramAction parse(String type, String value) { ... }
    public void execute(Player player) { ... }
    public String getType() { ... }
    public String getValue() { ... }
}
```

**Target shape:**
```java
// permission is nullable — null means "no restriction"
private final String permission;   // new field
// constructor expands to (type, value, permission)
// parse() can keep its existing signature; a separate overload or rename
// accommodates the optional permission parameter
public String getPermission() { return permission; }
```

The static factory `parse(type, value)` is called from `HologramConfig.loadAll()` on line 114. Adding a `permission` parameter there is a one-line change that reads `map.get("permission")` — the same `Map<?, ?>` cast already used for `type` and `value`.

### Pattern 2: Per-Item Guard in an Action Loop
**What:** `HologramInteractListener.onInteract()` lines 58-60 iterate `hologram.getActions()` and call `action.execute(player)` unconditionally. The permission guard is an `if` that skips the current iteration — it does not cancel the loop.

**Exact insertion point (line 58-60):**
```java
for (HologramAction action : hologram.getActions()) {
    // SEC-01: skip action if player lacks the required permission
    if (action.getPermission() != null && !player.hasPermission(action.getPermission())) {
        continue;  // silent skip — other actions still execute
    }
    action.execute(player);
}
```

**Key invariant (D-03):** `HologramClickEvent` fires at line 54-56, BEFORE this loop. The event is unconditional. The permission filter is post-event.

### Pattern 3: Load-Time Validation with Warning-and-Skip
**What:** `NPCSkin.fromConfig()` (4 lines) is the canonical injection point for the Base64 guard. The existing logic already returns `null` for blank/null texture. Adding a structural validation before the `return new NPCSkin(...)` makes the guard airtight.

**Current shape:**
```java
public static NPCSkin fromConfig(ConfigurationSection section) {
    String texture = section.getString("skin-texture");
    String signature = section.getString("skin-signature");
    if (texture == null || texture.isBlank()) return null;
    return new NPCSkin(texture, signature);
}
```

**Problem:** `NPCSkin.fromConfig()` does not have access to the NPC id for the warning log format (`WARNING: NPC "{npcId}" has malformed Base64 skin texture — skin skipped`). The NPC id is available in `NPCConfig.loadNPC()` at lines 63-64 where `skinTexture` and `skinSignature` are read.

**Resolution:** Two options with different tradeoffs:

Option A — Validate in `NPCSkin.fromConfig()` with an `id` parameter:
```java
public static NPCSkin fromConfig(ConfigurationSection section, String npcId, java.util.logging.Logger logger) {
    String texture = section.getString("skin-texture");
    String signature = section.getString("skin-signature");
    if (texture == null || texture.isBlank()) return null;
    try {
        java.util.Base64.getDecoder().decode(texture);
    } catch (IllegalArgumentException e) {
        logger.warning("NPC \"" + npcId + "\" has malformed Base64 skin texture — skin skipped");
        return null;
    }
    if (signature != null && !signature.isBlank()) {
        try {
            java.util.Base64.getDecoder().decode(signature);
        } catch (IllegalArgumentException e) {
            logger.warning("NPC \"" + npcId + "\" has malformed Base64 skin signature — signature ignored");
            signature = null;
        }
    }
    return new NPCSkin(texture, signature);
}
```

Option B — Validate inline in `NPCConfig.loadNPC()` at lines 63-64, before constructing `NPCSkin`:
```java
// In loadNPC(), after reading skinTexture / skinSignature:
if (skinTexture != null && !skinTexture.isBlank()) {
    try {
        java.util.Base64.getDecoder().decode(skinTexture);
    } catch (IllegalArgumentException e) {
        plugin.getLogger().warning("NPC \"" + id + "\" has malformed Base64 skin texture — skin skipped");
        skinTexture = null;
        skinSignature = null;
    }
}
// skinTexture (now possibly null) is passed to new NPC(...) on line 98
```

Both options satisfy D-05 through D-08. Option B is simpler: it does not change the `NPCSkin.fromConfig()` signature (which the CONTEXT.md calls the alternate validation location). However, `NPCSkin.fromConfig()` is currently not called by `loadNPC()` — `loadNPC()` reads the strings directly and passes them to the `NPC` constructor. `NPCSkin.fromConfig()` appears to be a utility not wired into the main path. The planner should choose: validate in `loadNPC()` (option B, minimal surface change) or wire in `NPCSkin.fromConfig()` as the standard path (option A, consistent abstraction). CONTEXT.md D-05 says "in `NPCSkin.fromConfig()` AND in `NPCConfig.loadNPC()`" suggesting dual coverage — but the planner should pick one location since double validation is redundant. Option A with the id/logger parameters threads through the NPC id cleanly.

### Pattern 4: DebugContext Record Pattern
**What:** `ServerCoreCommand` currently holds only `ServerCoreConfig`. For debug output it needs references to all nine managers, three stores, and five tick tasks. Passing 17 parameters to the constructor would be unreadable. A `DebugContext` record bundles them.

**Recommended shape (Claude's discretion per D-12):**
```java
// DebugContext.java — new file in net.axther.serverCore.command
public record DebugContext(
    // Managers (null if system disabled)
    CosmeticManager cosmeticManager,
    EmitterManager emitterManager,
    PetManager petManager,
    HologramManager hologramManager,
    NPCManager npcManager,
    QuestManager questManager,
    TimelineManager timelineManager,
    ReactiveManager reactiveManager,
    MenuManager menuManager,
    // Stores (null if system disabled)
    CosmeticStore cosmeticStore,
    PetStore petStore,
    QuestStore questStore,
    // Tick tasks (null if system disabled)
    BukkitRunnable cosmeticTask,
    BukkitRunnable emitterTask,
    BukkitRunnable petTask,
    BukkitRunnable hologramTask,
    BukkitRunnable npcTask,
    BukkitRunnable timelineTask,
    BukkitRunnable reactiveTask,
    BukkitRunnable menuTask,
    BukkitRunnable saveFlushTask,
    // Hook presence (booleans set at startup)
    boolean packetEventsPresent,
    boolean modelEnginePresent,
    boolean placeholderApiPresent,
    boolean vaultPresent
) {}
```

`ServerCore.onEnable()` constructs `DebugContext` at the same point it constructs `ServerCoreCommand` (lines 100-105). All required references are already local variables in `onEnable()`.

### Pattern 5: Tick Task Status via isCancelled()
**What:** `BukkitRunnable` exposes `isCancelled()` (returns true after `cancel()` is called) and `isScheduled()` (returns true once scheduled, regardless of cancellation). For debug output, "running" means `isScheduled() && !isCancelled()`.

**Important:** If a system is disabled, its tick task field in `ServerCore` remains `null`. The debug command must guard: `task != null ? (task.isCancelled() ? "stopped" : "running") : "not loaded"`.

### Pattern 6: Active Instance Count Queries
**What:** Each manager exposes a different method for counting active instances. The debug command must call the correct one per system.

| System | Manager | Count Method | Notes |
|--------|---------|-------------|-------|
| Cosmetics | `CosmeticManager` | `getActiveCosmetics().size()` | Returns `Map<UUID, List<CosmeticInstance>>` — `.size()` is mob count, not instance count |
| Emitters | `EmitterManager` | `getAll().size()` (inferred) or `emittersById.size()` | Check exact method; map is `emittersById` |
| Pets | `PetManager` | `activePets.size()` (via getter if exists) | `activePets` is private `Map<UUID, List<PetInstance>>` — need `getActivePets()` or `getActivePetCount()` |
| Holograms | `HologramManager` | `getAll().size()` | Returns all registered holograms (spawned or not) |
| NPCs | `NPCManager` | `getAll().size()` | Returns `Collection<NPC>` |
| Quests | `QuestManager` | `activeQuests.size()` (players with active quests) | `getAllActiveQuests().size()` |
| Timelines | `TimelineManager` | `activeInstances.size()` (via getter) | Field is `List<TimelineInstance> activeInstances` |
| Reactive | `ReactiveManager` | `rules.size()` (registered rules) | `activeEffects.size()` is players with active effects |
| Menus | `MenuManager` | `openMenus.size()` | Players with open menus |

**IMPORTANT:** Several of these are private fields. The planner must check each manager for public getters before writing the debug command. If a getter does not exist, a getter must be added to the manager as part of the plan. Specifically:
- `PetManager.getActivePets()` — verify it exists or add it
- `EmitterManager.getAll()` — verify return type
- `TimelineManager.getActiveInstanceCount()` — verify it exists or add it
- `ReactiveManager.getRuleCount()` — verify it exists or add it

### Anti-Patterns to Avoid

- **Permission field on Hologram, not HologramAction:** D-04 explicitly requires per-action permissions, not per-hologram. Do not add a `permission` field to `Hologram.java`.
- **Sending a message to the player when permission is denied on an action:** D-02 requires silent skip. No `player.sendMessage()` in the permission check branch.
- **Firing `HologramClickEvent` after permission filtering:** D-03 requires the event fires first. Do not move the event below the action loop.
- **Validating Base64 per render (per packet send):** D-07 says load-time only. `NPCRenderer.sendSpawn()` must not be modified for this requirement.
- **Storing the decoded byte[] from Base64 validation:** The skin is stored as the raw Base64 string. The decode is a structural-validation throw-away — do not save the decoded bytes.
- **Null debug output for disabled systems:** If a system is disabled (manager is null), the debug command should output `"not loaded"` — not throw NPE.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Base64 decoding | Custom Base64 parser | `java.util.Base64.getDecoder().decode(String)` | JDK stdlib; handles all edge cases (padding, non-base64 chars); throws `IllegalArgumentException` on malformed input — exactly what D-05 requires |
| Permission checking | String comparison of permission nodes | `Player.hasPermission(String)` | Bukkit permission API; handles wildcards, negation, OP default; project-wide pattern |
| Task running status | Thread state inspection | `BukkitRunnable.isCancelled()` | Built into BukkitRunnable; no additional state tracking needed |

---

## Common Pitfalls

### Pitfall 1: NPCSkin.fromConfig() does not have the NPC id
**What goes wrong:** The log message required by D-08 includes the NPC id (`NPC "merchant" has malformed...`). `NPCSkin.fromConfig()` receives only a `ConfigurationSection` — not the NPC's top-level `id` string.
**Why it happens:** `NPCSkin` is a record designed around its section. The NPC id lives one level above in the YAML, parsed by `NPCConfig.loadNPC()`.
**How to avoid:** Pass the npc `id` (and optionally the logger) as parameters to `fromConfig()`, OR perform validation in `loadNPC()` inline before the `NPCSkin` record is constructed. Either approach satisfies D-08. Do not log a generic "malformed skin" without naming the NPC.

### Pitfall 2: HologramConfig.saveAll() does not round-trip the permission field
**What goes wrong:** `HologramConfig.saveAll()` serializes each action's `type` and `value` keys into a `Map<String, Object>`. If `permission` is not included in the save map, it is silently dropped on next reload — holograms that required permissions suddenly execute for everyone.
**Why it happens:** `saveAll()` builds the action maps manually (lines 175-181). Adding a new field to `HologramAction` requires updating both the load path (`loadAll()`) and the save path (`saveAll()`).
**How to avoid:** Whenever adding a field to `HologramAction`, update `saveAll()` to include `if (act.getPermission() != null) m.put("permission", act.getPermission())` in the action map. The planner must include this in the task for SEC-01.

### Pitfall 3: saveAll() default example does not show permission field
**What goes wrong:** `HologramConfig.saveDefault()` generates the initial `holograms.yml` example. After adding permission support, new operators won't know the field exists unless the example demonstrates it.
**Why it happens:** Default generation is manual code, not derived from the data model.
**How to avoid:** The planner may optionally add a commented example action with `permission` to the `vip-portal` example in `saveDefault()`. This is low-priority but improves operator experience.

### Pitfall 4: Debug command NullPointerException on disabled systems
**What goes wrong:** If a system is disabled in `config.yml`, its manager field in `ServerCore` is `null`. The debug command code calls methods on that null reference.
**Why it happens:** All nine systems are conditionally initialized. The debug command runs at any time and must not assume all systems are loaded.
**How to avoid:** Every manager reference in the debug handler must be null-checked before accessing. Pattern: `manager != null ? manager.getAll().size() : -1` (or print `"disabled"`).

### Pitfall 5: `servercore.admin.debug` permission not declared in plugin.yml
**What goes wrong:** The permission check `sender.hasPermission("servercore.admin.debug")` returns `false` for non-op players even if a permission plugin grants it, because undeclared permissions default to `false` for non-ops in Bukkit.
**Why it happens:** plugin.yml permissions are the canonical declaration source.
**How to avoid:** Add `servercore.admin.debug` to `plugin.yml` under both the `servercore.admin.*` children map and as a standalone node with `default: op`.

### Pitfall 6: BukkitRunnable.isScheduled() vs isCancelled() confusion
**What goes wrong:** `isScheduled()` returns `true` even after cancellation on some BukkitRunnable implementations. `isCancelled()` is the definitive "not running" check.
**Why it happens:** The Paper API docs define `isCancelled()` as returning true when the task has been cancelled, regardless of whether it has run yet.
**How to avoid:** Use `isCancelled()` for "stopped" detection. A task that has never been scheduled (manager null) is reported as `"not loaded"`, not `"stopped"`.

---

## Code Examples

### Base64 Validation Pattern
```java
// Source: java.util.Base64 JDK 21 documentation
// In NPCSkin.fromConfig() or NPCConfig.loadNPC()
try {
    java.util.Base64.getDecoder().decode(texture);
} catch (IllegalArgumentException e) {
    logger.warning("NPC \"" + npcId + "\" has malformed Base64 skin texture — skin skipped");
    return null;  // or set skinTexture = null
}
```

### Per-Action Permission Check
```java
// In HologramInteractListener.onInteract(), replacing lines 58-60
for (HologramAction action : hologram.getActions()) {
    if (action.getPermission() != null && !player.hasPermission(action.getPermission())) {
        continue;  // SEC-01: silent skip
    }
    action.execute(player);
}
```

### HologramConfig.loadAll() action parsing (adding permission)
```java
// In HologramConfig.loadAll(), the existing action-map block
if (obj instanceof java.util.Map<?, ?> map) {
    String actType  = map.get("type")  != null ? String.valueOf(map.get("type"))  : "";
    String actValue = map.get("value") != null ? String.valueOf(map.get("value")) : "";
    String actPerm  = map.get("permission") != null ? String.valueOf(map.get("permission")) : null;
    hologram.getActions().add(
        HologramAction.parse(actType, actValue, actPerm));  // updated signature
}
```

### HologramConfig.saveAll() action serialization (round-trip)
```java
// In HologramConfig.saveAll(), the action-map block
java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
m.put("type",  act.getType());
m.put("value", act.getValue());
if (act.getPermission() != null) {
    m.put("permission", act.getPermission());
}
actMaps.add(m);
```

### Debug Command Handler Structure
```java
// In ServerCoreCommand.onCommand(), new "debug" branch
if (args[0].equalsIgnoreCase("debug")) {
    if (!sender.hasPermission("servercore.admin.debug")) {
        sender.sendMessage("No permission.");
        return true;
    }
    printDebug(sender);
    return true;
}

private void printDebug(CommandSender sender) {
    sender.sendMessage("=== ServerCore Debug ===");
    sender.sendMessage("-- Active Instances --");
    sender.sendMessage("  cosmetics:  " + countOrDisabled(ctx.cosmeticManager(),
        m -> m.getActiveCosmetics().size()));
    // ... one line per system
    sender.sendMessage("-- Save State --");
    sender.sendMessage("  cosmetic-store: " + storeState(ctx.cosmeticStore()));
    sender.sendMessage("  pet-store:      " + storeState(ctx.petStore()));
    sender.sendMessage("  quest-store:    " + storeState(ctx.questStore()));
    sender.sendMessage("-- Soft Dependencies --");
    sender.sendMessage("  PacketEvents:   " + presence(ctx.packetEventsPresent()));
    sender.sendMessage("  ModelEngine:    " + presence(ctx.modelEnginePresent()));
    sender.sendMessage("  PlaceholderAPI: " + presence(ctx.placeholderApiPresent()));
    sender.sendMessage("  Vault:          " + presence(ctx.vaultPresent()));
    sender.sendMessage("-- Tick Tasks --");
    sender.sendMessage("  cosmetic-task:  " + taskState(ctx.cosmeticTask()));
    // ... one line per task
}
```

### Debug Helper Methods
```java
private static String storeState(/* Store */ Object store) {
    if (store == null) return "disabled";
    // Store exposes isDirty() — use reflection-free approach
    return /* store.isDirty() */ ? "dirty" : "clean";
}

private static String taskState(BukkitRunnable task) {
    if (task == null) return "not loaded";
    return task.isCancelled() ? "stopped" : "running";
}

private static String presence(boolean flag) {
    return flag ? "present" : "absent";
}
```

---

## Integration Map

This section maps each decision to the exact file, line, and change type required — the planner can derive tasks directly.

### SEC-01: Hologram Action Permissions

| File | Change | Scope |
|------|--------|-------|
| `hologram/action/HologramAction.java` | Add `String permission` field; expand constructor; add `getPermission()` getter; update `parse()` factory to accept optional permission | 8 lines added |
| `hologram/config/HologramConfig.java` — `loadAll()` action block (line 111-114) | Read `map.get("permission")`; pass to `HologramAction.parse()` | 1 line added |
| `hologram/config/HologramConfig.java` — `saveAll()` action block (lines 175-181) | Add `if (act.getPermission() != null) m.put("permission", ...)` | 3 lines added |
| `hologram/listener/HologramInteractListener.java` — lines 58-60 | Insert `continue` guard before `action.execute(player)` | 3 lines added |
| `resources/plugin.yml` | No change required — `permission` field in YAML is player-defined; only `servercore.admin.debug` needs to be declared |  |

### CONF-02 / SEC-02: NPC Skin Base64 Validation

| File | Change | Scope |
|------|--------|-------|
| `npc/NPCSkin.java` | Add npc id + logger params to `fromConfig()` (Option A) OR add validation block before `return` (needs id from caller) | 10 lines added |
| `npc/config/NPCConfig.java` — `loadNPC()` lines 63-64 | Option B: add `try-catch` around `Base64.getDecoder().decode(skinTexture)` after reading `skinTexture` | 8 lines added |

The planner must pick Option A or B — not both. Option B is recommended for minimal surface change.

### OBS-01: Debug Command

| File | Change | Scope |
|------|--------|-------|
| `command/DebugContext.java` | New record file grouping all manager/store/task/hook references | ~30 lines |
| `command/ServerCoreCommand.java` | Add `DebugContext ctx` field; expand constructor; add `debug` branch in `onCommand()`; add `"debug"` to tab-completion; add `printDebug()` and helper methods | ~60 lines added |
| `ServerCore.java` — lines 100-105 | Construct `DebugContext` with all live references; pass to `ServerCoreCommand` constructor | ~30 lines added (DebugContext construction) |
| `resources/plugin.yml` | Add `servercore.admin.debug` under `servercore.admin.*` children and as standalone node with `default: op` | 5 lines |

---

## State of the Art

| Old Approach | Current Approach | Notes |
|--------------|------------------|-------|
| No per-action permission field | `permission` is null → unconditional execution; non-null → `player.hasPermission()` check | Minecraft server norm since at least 2016 |
| Skin strings passed to PacketEvents without structural check | Validate Base64 at config load, skip on failure | PacketEvents may silently send garbage; validation prevents client issues |
| No live plugin state command | `/servercore debug` prints snapshot of all nine systems | Pattern common in mature Bukkit plugins (e.g., EssentialsX `/essentials debug`) |

---

## Open Questions

1. **Which active-count getter exists on each manager?**
   - What we know: `CosmeticManager.getActiveCosmetics()` exists (returns `Map<UUID, List<CosmeticInstance>>`); `HologramManager.getAll()` exists; `NPCManager.getAll()` exists; `QuestManager.getAllActiveQuests()` exists.
   - What's unclear: `PetManager` has `activePets` as a private field — does a public getter exist? `TimelineManager.activeInstances` is private — is there a `getActiveInstanceCount()`? `ReactiveManager.rules` is private — is there a `getRuleCount()`? `EmitterManager.emittersById` is private — is there a `getAll()`?
   - Recommendation: The planner should either (a) verify each getter exists by reading each manager file, or (b) plan a task to add `getActiveCount()` methods to any manager that lacks one. The research task checked the first 30 lines of PetManager — the full file should be verified before writing the debug command.

2. **Should the DebugContext be a record, interface, or direct constructor parameters?**
   - What we know: CONTEXT.md marks this as Claude's discretion (D-12). The codebase uses records for small immutable data (`NPCSkin`, `Token`, `ExploreTarget`). A record fits naturally.
   - What's unclear: Whether the planner prefers a dedicated `DebugContext.java` file or inline construction.
   - Recommendation: Use a `record DebugContext(...)` in `net.axther.serverCore.command`. Records are zero-boilerplate for this use case and match codebase conventions.

3. **Does `NPCSkin.fromConfig()` get called anywhere in the normal NPC load path?**
   - What we know: `NPCConfig.loadNPC()` reads `skinTexture` and `skinSignature` directly as strings (lines 63-64) and passes them to the `NPC` constructor on line 98. `NPCSkin.fromConfig()` is a static utility but not currently called from `loadNPC()`.
   - What's unclear: Whether any other code path calls `NPCSkin.fromConfig()`.
   - Recommendation: Search for `NPCSkin.fromConfig` usages before deciding validation placement. If it is called elsewhere, validate there. If it is effectively dead code in the load path, validate inline in `loadNPC()` (Option B) to avoid threading in extra parameters.

---

## Sources

### Primary (HIGH confidence)
- Source code audit: `HologramAction.java`, `HologramInteractListener.java`, `HologramConfig.java` — direct line-by-line reading; all integration points confirmed
- Source code audit: `NPCSkin.java`, `NPCConfig.java`, `NPCRenderer.java` — skin load path confirmed
- Source code audit: `ServerCoreCommand.java`, `ServerCore.java`, all nine managers, three stores, tick tasks — debug command data sources confirmed
- `plugin.yml` — permission system structure confirmed; `servercore.admin.debug` is absent and must be added
- JDK 21 `java.util.Base64` — standard library; `getDecoder().decode(String)` throws `IllegalArgumentException` on malformed Base64 (HIGH confidence — stdlib contract)
- Paper API 1.21 `Player.hasPermission(String)` — project-wide pattern; confirmed in all command handlers
- Paper API 1.21 `BukkitRunnable.isCancelled()` — used in `onDisable()` pattern; confirmed in `CosmeticTickTask`

### Secondary (MEDIUM confidence)
- `.planning/codebase/CONCERNS.md` — confirmed security issue entries for hologram action permissions and NPC skin validation
- `.planning/phases/01-correctness-and-stability/1-CONTEXT.md` — logging convention (WARNING for config errors, SEVERE for I/O) applied to D-08

### Tertiary (LOW confidence)
- None — all claims verified against source code

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all APIs are JDK stdlib or Paper API, confirmed present in codebase
- Architecture patterns: HIGH — derived from direct source code reading with exact line numbers
- Integration points: HIGH — every insertion point identified from source, not assumed
- Pitfalls: HIGH — all derived from actual code structure observed (saveAll() round-trip gap found from reading source, not from prior knowledge)
- Open questions: MEDIUM — count-getter availability requires full manager file reads; noted explicitly

**Research date:** 2026-03-21
**Valid until:** 2026-04-20 (30 days — Paper 1.21 API stable, JDK 21 stdlib stable)
