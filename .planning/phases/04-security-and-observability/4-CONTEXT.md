# Phase 4: Security and Observability - Context

**Gathered:** 2026-03-21
**Status:** Ready for planning

<domain>
## Phase Boundary

Enforce hologram action permissions, validate NPC skin Base64 before packet send, and add a live debug command for operators. No new features — only security hardening and a diagnostic command within existing component boundaries.

**Requirements:** CONF-02, SEC-01, SEC-02, OBS-01

</domain>

<decisions>
## Implementation Decisions

### Hologram action permission gating (SEC-01)
- **D-01:** Add an optional `permission` field to `HologramAction`. Parsed from YAML config as a string (e.g., `permission: "servercore.hologram.vip"`). When null or empty, the action executes unconditionally (backward-compatible).
- **D-02:** Permission check happens in `HologramInteractListener.onInteract()` inside the action loop (line 58-60). If `action.getPermission() != null && !player.hasPermission(action.getPermission())`, skip that action silently — no message to the player, no log. Other actions in the same hologram that the player DOES have permission for still execute.
- **D-03:** The `HologramClickEvent` fires before the permission-filtered action loop. External plugins see the click event regardless of permission outcome — they can cancel at will.
- **D-04:** Permission field is per-action, not per-hologram. Different actions on the same hologram can have different permission requirements. This matches the existing YAML structure where actions is a list.

### NPC skin Base64 validation (CONF-02, SEC-02)
- **D-05:** Validate the `skin-texture` field as valid Base64 at config load time in `NPCSkin.fromConfig()` and in `NPCConfig.loadNPC()` (line 63-64). Use `java.util.Base64.getDecoder().decode()` in a try-catch. If decoding throws `IllegalArgumentException`, log WARNING naming the NPC and skip the skin (NPC spawns without a skin, which is safe).
- **D-06:** The `skin-signature` field is also Base64 but is optional (some skin providers omit it). Validate if present; skip validation if null/blank.
- **D-07:** Validation happens once at load time — no per-player or per-render validation needed. The validated skin is stored as-is (the Base64 string goes to PacketEvents). The point is to reject garbage BEFORE it enters the system.
- **D-08:** Log format: `WARNING: NPC "{npcId}" has malformed Base64 skin texture — skin skipped`. Include the NPC id so operators know which config file to fix.

### Debug command (OBS-01)
- **D-09:** Add a `debug` subcommand to the existing `ServerCoreCommand` (`/servercore debug`). Requires `servercore.admin.debug` permission. Outputs to the command sender (console or player).
- **D-10:** The debug output prints one section per system with active instance counts. All nine systems are covered: cosmetics, emitters, pets, holograms, NPCs, quests, timelines, reactive cosmetics, and GUIs (menus). Format: `"  cosmetics: {N} active instances"` per line.
- **D-11:** Additional diagnostic lines: pending save flags per store (dirty/clean), registered soft-dependency hooks (present/absent for PacketEvents, ModelEngine, PlaceholderAPI, Vault), tick task status (running/stopped for each tick task).
- **D-12:** ServerCoreCommand needs references to the managers and stores to query live state. Pass them via constructor or a context object. Claude's discretion on the exact mechanism — either expand the constructor or pass a `DebugContext` record.

### Claude's Discretion
- Whether `HologramAction.permission` is a constructor parameter or a setter
- Whether Base64 validation is a static utility method or inline in NPCSkin.fromConfig()
- The exact format and ordering of debug output lines
- Whether DebugContext is a record, interface, or direct constructor parameters
- Tab-completion for the `debug` subcommand

</decisions>

<specifics>
## Specific Ideas

- Prior phases used WARNING level for operational issues and SEVERE for I/O failures (Phase 1 D-01/D-02). Follow the same convention: malformed Base64 is WARNING (bad config data, not a crash), failed file writes are SEVERE.
- The debug command should be useful for server operators diagnosing issues in production — think "at a glance, is everything healthy?" Not verbose debug logging, but a snapshot.
- Permission-gated hologram actions are a common server admin pattern (VIP-only warps, rank-gated shops). The implementation should feel natural to Minecraft server operators.

</specifics>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Codebase analysis
- `.planning/codebase/CONCERNS.md` — Security considerations for hologram actions and NPC skin validation
- `.planning/codebase/ARCHITECTURE.md` — Manager/Listener/Command layer structure

### Prior phases
- `.planning/phases/01-correctness-and-stability/1-CONTEXT.md` — Logging conventions (D-01/D-02), lifecycle patterns
- `.planning/phases/03-async-persistence-and-performance/3-CONTEXT.md` — Dirty-flag store pattern (for debug output of save state)

### Requirements
- `.planning/REQUIREMENTS.md` — Requirements CONF-02, SEC-01, SEC-02, OBS-01

### Key source files
- `src/main/java/net/axther/serverCore/hologram/action/HologramAction.java` — Action class with execute(), needs permission field
- `src/main/java/net/axther/serverCore/hologram/listener/HologramInteractListener.java` — Lines 58-60: action loop, needs permission check
- `src/main/java/net/axther/serverCore/hologram/config/HologramConfig.java` — Where permission YAML field is parsed
- `src/main/java/net/axther/serverCore/npc/NPCSkin.java` — Record with fromConfig(), needs Base64 validation
- `src/main/java/net/axther/serverCore/npc/config/NPCConfig.java` — Lines 63-64: skin loading, needs validation
- `src/main/java/net/axther/serverCore/npc/render/NPCRenderer.java` — Where skin is sent via PacketEvents
- `src/main/java/net/axther/serverCore/command/ServerCoreCommand.java` — Existing /servercore command, add debug subcommand
- `src/main/java/net/axther/serverCore/ServerCore.java` — Manager references for debug context

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **HologramAction (46 lines):** Simple type/value record with execute(Player). Adding a permission field is straightforward — add a third String field.
- **NPCSkin.fromConfig() (lines 7-12):** Already returns null for missing/blank texture. Add Base64 decode validation before the return.
- **ServerCoreCommand (61 lines):** Clean TabExecutor with reload subcommand. Add debug subcommand following same pattern.
- **HologramConfig:** Already parses action type and value from YAML. Permission is one additional `getString("permission")` call.

### Established Patterns
- **Permission checking:** All commands use `sender.hasPermission("servercore.system.action")` pattern. Consistent prefix.
- **Config validation:** Phase 1 established clamp-and-warn pattern (D-03/D-04). Phase 4 follows: validate-and-skip for malformed data.
- **Tab completion:** All commands provide tab completion. ServerCoreCommand has `onTabComplete()` — add "debug" to the list.
- **Manager accessor pattern:** `ServerCoreAPI.getCosmeticManager()`, etc. Debug command can use the API or direct references.

### Integration Points
- **HologramConfig.loadAll():** Where `permission` is parsed from YAML action blocks
- **HologramInteractListener.onInteract() line 58:** Where permission check is inserted
- **NPCSkin.fromConfig():** Where Base64 validation is added
- **NPCConfig.loadNPC() line 63-64:** Alternate validation location
- **ServerCoreCommand constructor:** Where manager/store references are injected
- **ServerCore.onEnable():** Where ServerCoreCommand is constructed — pass debug context here

### Key Files (from scout)

| File | Lines | What's there | What's needed |
|------|-------|-------------|---------------|
| HologramAction.java | 8-46 | type/value fields, execute() | Add permission field + getter |
| HologramInteractListener.java | 58-60 | Unguarded action loop | Add permission check per action |
| HologramConfig.java | — | Parses action type/value | Parse permission field |
| NPCSkin.java | 5-13 | Record with fromConfig | Add Base64 validation |
| NPCConfig.java | 63-64 | Reads skin-texture/signature | Validate before creating NPCSkin |
| ServerCoreCommand.java | 11-61 | reload subcommand only | Add debug subcommand |
| ServerCore.java | — | Constructs ServerCoreCommand | Pass debug context |

</code_context>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 04-security-and-observability*
*Context gathered: 2026-03-21*
