# Phase 1: Correctness and Stability - Context

**Gathered:** 2026-03-21
**Status:** Ready for planning

<domain>
## Phase Boundary

Eliminate crashes, null-dereferences, lifecycle failures, and silent config errors across all nine systems. The plugin must not crash during normal operation, must give server operators meaningful error output, and must have a complete onDisable() lifecycle. No new features — only fixes within existing component boundaries.

**Requirements:** CORR-01, CORR-02, CORR-03, CORR-04, LIFE-01, LIFE-02, LIFE-03, LIFE-04, CONF-01, CONF-03, CONF-04

</domain>

<decisions>
## Implementation Decisions

### Error handling in tick loops
- **D-01:** Each instance tick in `tickAll()` loops gets its own try-catch. A failed instance logs a WARNING with the instance UUID and exception, then returns false (triggering cleanup). One bad instance must never kill the tick loop for all remaining instances.
- **D-02:** The catch should log at WARNING level (not SEVERE) since individual instance failures are expected during edge cases like entity death mid-tick.

### Config validation behavior
- **D-03:** Invalid config values are clamped to sensible defaults with a WARNING log naming the specific config key, the invalid value, and the default being used. Systems are NOT disabled for bad config — they use the clamped value.
- **D-04:** NPC view-distance is clamped to minimum 1 (and maximum 256 for sanity). Zero or negative values are clamped and logged.
- **D-05:** World existence is checked at config load time. Invalid world names log a WARNING and skip that entry (hologram, NPC, emitter) rather than creating an entity with a null world.

### Lifecycle idempotency pattern
- **D-06:** `onDisable()` uses null guards on each field (`if (tickTask != null) tickTask.cancel()`) before cleanup. No new initialized-flag tracking needed — null check is sufficient since fields are initialized to null.
- **D-07:** Lifecycle listeners (death, chunk unload) use `remove()` return value or containment check before `destroy()`. Double-fire of the same entity UUID is a no-op, not an exception.

### Soft dependency centralization
- **D-08:** Soft dependency detection stays in `onEnable()` (not extracted to a separate class — the current pattern is safe). The improvement is adding INFO-level log lines for each present/absent dependency: `"[ServerCore] PacketEvents detected — NPC system enabled"` and `"[ServerCore] PacketEvents not found — NPC system disabled"`.

### QuestManager initialization
- **D-09:** The existing null-guard at line 279 (`if (questManager == null)`) prevents true double-initialization. The fix is documentation (a comment explaining the initialization order) and adding a log line when QuestManager is created early for NPC inline quests.

### Scope adjustment from codebase scout
- **D-10:** CORR-01 (event cancellation orphan fix) — codebase scout confirmed CosmeticApplyEvent is already fired BEFORE armor stand spawn. This requirement needs VERIFICATION only, not a fix. Verify the same pattern in PetSummonEvent and any other cancellable events.
- **D-11:** LIFE-01 (task cancellation in onDisable) — already implemented correctly for all 8 tick tasks. Needs verification only.
- **D-12:** LIFE-04 (synchronous data flush) — already implemented for all 5 stores. Needs verification only.

### Claude's Discretion
- Exact try-catch structure in tickAll() (wrap individual `instance.tick()` vs. wrap the removeIf predicate)
- Which specific config keys need bounds validation beyond view-distance (discover during implementation)
- Whether to add @Nullable annotations to entity reference getters

</decisions>

<specifics>
## Specific Ideas

- The user wants an "extensive" debug pass — thoroughness is preferred over speed. Every system should be verified, not just the ones flagged in CONCERNS.md.
- The null-check audit should be systematic (grep-based), not ad-hoc. Cover all 9 systems.
- Existing patterns that are already correct should be verified and documented, not rewritten.

</specifics>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Codebase analysis
- `.planning/codebase/CONCERNS.md` — Known bugs, fragile areas, security issues (primary bug list)
- `.planning/codebase/ARCHITECTURE.md` — System architecture, layer responsibilities, data flow
- `.planning/codebase/STRUCTURE.md` — File locations, naming conventions, where to add code

### Research
- `.planning/research/SUMMARY.md` — Synthesized research: stack, features, architecture approach, pitfalls
- `.planning/research/PITFALLS.md` — Critical pitfalls to avoid during implementation
- `.planning/research/ARCHITECTURE.md` — Fix ordering and component boundary analysis

### Requirements
- `.planning/REQUIREMENTS.md` — Requirements CORR-01..04, LIFE-01..04, CONF-01, CONF-03, CONF-04

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **Null-check pattern (CosmeticInstance:41-48):** `mob == null || mob.isDead() || stand == null || stand.isDead()` — canonical pattern, already used consistently in cosmetic/pet instances. Extend to all systems.
- **onDisable() structure (ServerCore:422-488):** Well-organized cancel-save-destroy per system. Add null guards to each block for partial-init safety.
- **Config defaults (ServerCoreConfig:27-29):** `config.getInt("key", default)` pattern with YAML defaults. Add validation wrapper.

### Established Patterns
- **Manager.tickAll() with removeIf:** `activeInstances.values().removeIf(list -> { list.removeIf(i -> !i.tick()); return list.isEmpty(); })` — existing pattern in CosmeticManager. Wrap `i.tick()` in try-catch.
- **Lifecycle listener cleanup:** `manager.removeCosmetics(uuid)` called from death/chunk-unload listeners. Make idempotent by checking containment first.
- **Soft dependency detection:** `getServer().getPluginManager().getPlugin("Name") != null` — consistent pattern across all 4 soft deps.

### Integration Points
- **ServerCore.onEnable() (lines 100-400):** Where config validation and soft-dep logging will be added
- **ServerCore.onDisable() (lines 422-488):** Where null guards for partial-init will be added
- **Each system's TickTask:** Where try-catch per instance will be added (via the manager's tickAll())
- **QuestManager (line 252, 280):** Where initialization guard documentation will be added

### Key Files (from scout)

| File | Lines | What's there | What's needed |
|------|-------|-------------|---------------|
| CosmeticManager.java | 35-73 | Event check before spawn ✓ | Verify only |
| CosmeticInstance.java | 41-48 | Null-before-dead pattern ✓ | Extend to all systems |
| PetInstance.java | 48-55 | Consistent null checks ✓ | Add try-catch in tick |
| ServerCore.java | 422-488 | Complete onDisable ✓ | Add null guards for partial-init |
| NPCViewTracker.java | 22-26 | No view-distance validation | Add clamp + log |
| ServerCoreConfig.java | 27-29 | No validation | Add bounds checking |
| ServerCore.java | 252, 280 | Null guard exists | Add documentation + logging |

</code_context>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 01-correctness-and-stability*
*Context gathered: 2026-03-21*
