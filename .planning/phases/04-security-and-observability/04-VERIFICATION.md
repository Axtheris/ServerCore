---
phase: 04-security-and-observability
verified: 2026-03-21T22:00:00Z
status: passed
score: 10/10 must-haves verified
re_verification: false
---

# Phase 4: Security and Observability Verification Report

**Phase Goal:** Hologram actions enforce permissions, NPC skins are validated before packet send, and operators can inspect live plugin state
**Verified:** 2026-03-21T22:00:00Z
**Status:** passed
**Re-verification:** No — initial verification

---

## Goal Achievement

### Observable Truths (from ROADMAP Success Criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | A player without the required permission who clicks a hologram action receives no effect — the action is silently skipped | VERIFIED | `HologramInteractListener.java` line 60: `if (action.getPermission() != null && !player.hasPermission(action.getPermission())) { continue; }` — guard is before `action.execute(player)`, loop continues to next action |
| 2 | A YAML NPC skin entry with a malformed Base64 texture field is rejected at load time with a WARNING log naming the NPC — no malformed packet is ever sent to clients | VERIFIED | `NPCConfig.java` lines 66-83: try/catch around `Base64.getDecoder().decode(skinTexture)` and `Base64.getDecoder().decode(skinSignature)`, with `plugin.getLogger().warning("NPC \"" + id + "\" has malformed Base64 skin texture — skin skipped")` and null assignment before the `new NPC(...)` call |
| 3 | Running `/servercore debug` prints active instance counts for all nine systems, pending save flags, registered soft-dependency hooks, and tick task status — all in one console output | VERIFIED | `ServerCoreCommand.java` `printDebug()` method lines 64-99 outputs all nine systems, three store states, four soft deps, and nine tick tasks; all wrapped by `countOrDisabled()` null-safe helper |

**Score:** 3/3 success criteria verified

---

### Required Artifacts (Plan 01 — SEC-01, CONF-02, SEC-02)

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/java/net/axther/serverCore/hologram/action/HologramAction.java` | HologramAction with optional permission field | VERIFIED | Contains `private final String permission`, 3-param constructor, `parse(String, String, String)`, and `getPermission()` getter (lines 12-48) |
| `src/main/java/net/axther/serverCore/hologram/config/HologramConfig.java` | Permission field parsed from YAML and round-tripped through save | VERIFIED | `loadAll()` line 113: `String actPerm = map.get("permission") != null ? String.valueOf(map.get("permission")) : null;` and 3-param `HologramAction.parse(actType, actValue, actPerm)`. `saveAll()` line 181: `if (act.getPermission() != null) { m.put("permission", act.getPermission()); }` |
| `src/main/java/net/axther/serverCore/hologram/listener/HologramInteractListener.java` | Per-action permission check in the action loop | VERIFIED | Lines 59-62: permission guard with `continue` is AFTER `HologramClickEvent` (line 54) and BEFORE `action.execute(player)` (line 63) |
| `src/main/java/net/axther/serverCore/npc/config/NPCConfig.java` | Base64 validation of skin texture and signature at load time | VERIFIED | Lines 65-83: dual try/catch blocks using `java.util.Base64.getDecoder().decode()` with named WARNING logs and null assignment on failure — appears after `skinTexture`/`skinSignature` reads (lines 63-64) and before `new NPC(...)` (line 117) |

### Required Artifacts (Plan 02 — OBS-01)

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/java/net/axther/serverCore/command/DebugContext.java` | Record bundling all manager/store/task/hook references | VERIFIED | `public record DebugContext(...)` with 26 fields: 9 managers, 3 stores, 9 BukkitRunnable tasks, 4 boolean hook flags |
| `src/main/java/net/axther/serverCore/command/ServerCoreCommand.java` | Debug subcommand implementation with printDebug method | VERIFIED | Contains `private final DebugContext debugContext`, 2-param constructor, `args[0].equalsIgnoreCase("debug")` branch, `printDebug(sender)` call, and all output sections |
| `src/main/java/net/axther/serverCore/ServerCore.java` | DebugContext construction and injection into ServerCoreCommand | VERIFIED | `new DebugContext(...)` at line 398, constructed AFTER `ServerCoreAPI.init()` (line 383) and `saveFlushTask.runTaskTimer()` (line 389); passed to `new ServerCoreCommand(serverCoreConfig, debugContext)` at line 409 |
| `src/main/resources/plugin.yml` | servercore.admin.debug permission declaration | VERIFIED | Line 44: `servercore.admin.debug: true` under `servercore.admin.*` children; lines 123-125: standalone `servercore.admin.debug:` with `description: View live plugin debug information` and `default: op` |
| `src/main/java/net/axther/serverCore/pet/PetManager.java` | getActivePetOwnerCount() public getter | VERIFIED | Lines 187-189: `public int getActivePetOwnerCount() { return activePets.size(); }` |
| `src/main/java/net/axther/serverCore/reactive/ReactiveManager.java` | getActiveEffectCount() public getter | VERIFIED | Lines 50-52: `public int getActiveEffectCount() { return activeEffects.size(); }` |

---

### Key Link Verification (Plan 01)

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `HologramConfig.loadAll()` | `HologramAction.parse()` | passes permission string from YAML map | VERIFIED | Line 115: `HologramAction.parse(actType, actValue, actPerm)` — 3-param call confirmed |
| `HologramConfig.saveAll()` | `HologramAction.getPermission()` | writes permission back to YAML action map | VERIFIED | Line 181: `if (act.getPermission() != null) { m.put("permission", act.getPermission()); }` |
| `HologramInteractListener.onInteract()` | `HologramAction.getPermission()` | checks permission before execute() | VERIFIED | Line 60: `action.getPermission() != null && !player.hasPermission(action.getPermission())` with `continue` |
| `NPCConfig.loadNPC()` | `java.util.Base64.getDecoder()` | validates skinTexture string before NPC constructor | VERIFIED | Lines 68, 78: two separate `java.util.Base64.getDecoder().decode(...)` calls inside try-catch blocks |

### Key Link Verification (Plan 02)

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `ServerCore.onEnable()` | `DebugContext` constructor | constructs after all systems initialized | VERIFIED | Line 398: `new DebugContext(...)` at end of onEnable, after ServerCoreAPI.init() at line 383 |
| `ServerCore.onEnable()` | `ServerCoreCommand` constructor | passes DebugContext to expanded constructor | VERIFIED | Line 409: `new ServerCoreCommand(serverCoreConfig, debugContext)` |
| `ServerCoreCommand.onCommand()` | `printDebug()` | debug subcommand branch | VERIFIED | Line 51: `args[0].equalsIgnoreCase("debug")` branch calls `printDebug(sender)` at line 56 |
| `plugin.yml` | `servercore.admin.debug` | permission declaration under children | VERIFIED | Two occurrences: line 44 (children) and line 123 (standalone) |

---

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|------------|-------------|--------|----------|
| SEC-01 | 04-01-PLAN.md | Hologram actions support optional `permission` field — HologramInteractListener checks player permission before executing action | SATISFIED | `HologramAction.getPermission()` getter exists; permission check with `continue` in listener loop before `execute()` |
| CONF-02 | 04-01-PLAN.md | NPC skin texture and signature fields validated as proper Base64 at load time — malformed values logged and skin skipped | SATISFIED | Dual Base64 decode try/catch blocks in `NPCConfig.loadNPC()` with WARNING log naming the NPC id; malformed texture nulls both fields |
| SEC-02 | 04-01-PLAN.md | NPC skin texture Base64 validation rejects malformed data before sending to clients via PacketEvents | SATISFIED | Validation and null assignment occur before `new NPC(...)` call at line 117, so PacketEvents never receives malformed data |
| OBS-01 | 04-02-PLAN.md | `/servercore debug` prints live plugin state: active instance counts, pending save flags, registered hooks, tick task status | SATISFIED | `printDebug()` in ServerCoreCommand covers all nine systems (cosmetics, emitters, pets, holograms, npcs, quests, timelines, reactive, menus), three stores (cosmetic/pet/quest), four hooks (PacketEvents/ModelEngine/PlaceholderAPI/Vault), nine tick tasks |

**Orphaned requirements check:** REQUIREMENTS.md traceability table maps CONF-02, SEC-01, SEC-02, OBS-01 all to Phase 4. All four are claimed in plan frontmatter. No orphaned requirements.

---

### Anti-Patterns Found

No blockers or warnings found in phase-modified files.

Specific checks performed:

- `HologramAction.java` — no empty implementations; `execute()` has real switch logic; `getPermission()` returns field directly.
- `HologramConfig.java` — no hardcoded empty collections; `actPerm` null fallback is correct (null means no restriction, not a stub).
- `HologramInteractListener.java` — `continue` is a real permission skip, not a placeholder; `HologramClickEvent` fires unconditionally before the filtered loop (correct invariant preserved).
- `NPCConfig.java` — Base64 validation blocks decode and discard bytes (structural validation only, not storage); WARNING log includes NPC id; malformed texture correctly nulls both fields.
- `DebugContext.java` — pure record, no logic; all 26 fields accounted for.
- `ServerCoreCommand.java` — `countOrDisabled()` generic helper uses `Function<T, Object>` properly; `storeState()` uses Java 21 pattern-matching instanceof; `taskState()` calls `BukkitRunnable.isCancelled()`; tab-completion permission-gated correctly.
- `ServerCore.java` — `DebugContext` construction is after `saveFlushTask.runTaskTimer()` (line 389) confirming all references are live at construction time.
- `plugin.yml` — `servercore.admin.debug` appears both as child of wildcard and as standalone permission with `default: op`.

---

### Human Verification Required

The following behaviors are correct at the code level but require a running server to fully confirm:

#### 1. Permission gate — live player test

**Test:** Give a player the `servercore.admin.*` permission and configure a hologram action with a `permission: some.permission.node` field in YAML. Log in as that player, click the hologram, confirm the action executes. Remove the `some.permission.node` permission node, click again — confirm the action is silently skipped (no message, no effect).
**Expected:** Action runs when permission held; silently skipped when not. Other actions on the same hologram without permission requirements still execute.
**Why human:** Permission evaluation depends on the live Bukkit permission system and player permission attachment — cannot simulate without a running server.

#### 2. NPC skin validation warning at startup

**Test:** Add an NPC YAML file with `skin-texture: NOT_VALID_BASE64!!!` and start the server.
**Expected:** Server logs a WARNING containing the NPC id and "malformed Base64 skin texture — skin skipped". The NPC loads without a skin and does not crash PacketEvents.
**Why human:** Requires a live server startup with the test YAML in place and log observation.

#### 3. `/servercore debug` output completeness

**Test:** Run `/servercore debug` as an operator on a server with all systems active.
**Expected:** Console output shows all nine system instance counts (not all zeros if server has loaded data), three store dirty/clean flags, four soft-dep present/absent flags, and nine tick task running/stopped statuses — all in one command.
**Why human:** Output correctness under real system load (non-zero counts, real dirty flags) requires a running server with data.

---

### Gaps Summary

No gaps. All automated verifications passed:

- Plan 01: HologramAction permission field, YAML round-trip, per-action listener check, and NPC Base64 validation are all substantively implemented and correctly wired.
- Plan 02: DebugContext record exists with all 26 fields; PetManager and ReactiveManager have the required getters; ServerCoreCommand has the debug subcommand, permission check, printDebug output for all nine systems/three stores/four hooks/nine tasks; ServerCore.onEnable() constructs DebugContext after all systems initialize; plugin.yml declares the permission in both required locations.
- Requirements CONF-02, SEC-01, SEC-02, OBS-01 are all satisfied with direct codebase evidence.

---

_Verified: 2026-03-21T22:00:00Z_
_Verifier: Claude (gsd-verifier)_
