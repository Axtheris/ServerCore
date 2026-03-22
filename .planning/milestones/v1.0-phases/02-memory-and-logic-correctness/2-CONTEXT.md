# Phase 2: Memory and Logic Correctness - Context

**Gathered:** 2026-03-21
**Status:** Ready for planning

<domain>
## Phase Boundary

Bound all unbounded maps and fix quest objective progression consistency. Memory usage must stay bounded over server lifetime. Quest FETCH objectives must behave consistently across accept/abandon/re-accept cycles. No new features — targeted fixes to memory leaks and logic bugs.

**Requirements:** CORR-05, CORR-06, MEM-01, MEM-02, MEM-03

</domain>

<decisions>
## Implementation Decisions

### Cooldown map eviction (MEM-01)
- **D-01:** Add a periodic sweep to the existing hologram tick task (or a separate low-frequency task) that removes expired entries from the `cooldowns` HashMap. Sweep every 5 minutes (6000 ticks). An entry is expired when `System.currentTimeMillis() > expiresAt`.
- **D-02:** The sweep uses `cooldowns.entrySet().removeIf(e -> now > e.getValue())` — single-line, no allocation, runs on main thread (safe for HashMap).
- **D-03:** No need to switch to ConcurrentHashMap — cooldowns are only accessed from the main thread (event handlers + tick task).

### Stand UUID index cleanup (MEM-02)
- **D-04:** Codebase scout confirmed CosmeticManager has 4 cleanup points and PetManager has 7 cleanup points. The standIndex is already well-synchronized with activeCosmetics/activePets. No unbounded growth risk exists under normal operation.
- **D-05:** Add a periodic safety-net audit sweep (every 10 minutes) that checks each standIndex entry against `Bukkit.getEntity(uuid)` — if the entity no longer exists, remove the entry and log at FINE/DEBUG level. This catches edge cases where an entity is removed without triggering any cleanup path.
- **D-06:** The sweep runs in the existing tick task at a low frequency counter (12000 ticks = 10 minutes).

### Hologram world-null guard (MEM-03)
- **D-07:** Phase 1 already added the world-null guard to HologramVisibilityTracker (lines 37-39). This requirement is to VERIFY it's sufficient and ensure no other code paths in the hologram system can NPE on null world. No new guard expected — verification only.

### Quest objective centralization (CORR-05)
- **D-08:** Codebase scout confirmed `getFirstIncompleteIndex()` is already a single private helper method (line 249-254) called from 3 locations. No duplication exists. This requirement is ALREADY SATISFIED — verification comment only.

### Quest FETCH consistency (CORR-06)
- **D-09:** The on-demand inventory check in `areObjectivesComplete()` is the CORRECT design — it ensures the player actually holds the items at completion time. This is not a bug; it's intentional.
- **D-10:** The "inconsistency" from CONCERNS.md (progress shows 0 after abandon/re-accept) is expected because FETCH objectives never write to QuestProgress — they always read live inventory. The fix is to update the quest UI/action bar display to show live inventory count for FETCH objectives instead of cached progress value (which is always 0).
- **D-11:** When `areObjectivesComplete()` checks FETCH objectives, fire a lightweight progress event so the action bar can display accurate FETCH counts. This is a display fix, not a logic fix.

### Claude's Discretion
- Exact tick counter implementation for periodic sweeps (static counter vs. scheduled task)
- Whether to combine cooldown sweep and standIndex sweep into one periodic task or keep separate
- Log level for swept entries (WARNING seems too noisy; INFO or FINE)

</decisions>

<specifics>
## Specific Ideas

- The user wants an "extensive" debug pass — all 9 systems should be checked for unbounded maps, not just the ones documented in CONCERNS.md.
- Phase 1 patterns (try-catch, WARNING logging) should be followed for consistency.
- Verification-only requirements (CORR-05, MEM-03) should get comments confirming they're satisfied, similar to Phase 1's approach.

</specifics>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Codebase analysis
- `.planning/codebase/CONCERNS.md` — Known bugs including cooldown map growth (line 33-37) and stand UUID growth (line 149-152)
- `.planning/codebase/ARCHITECTURE.md` — Manager/Instance/Tick pattern

### Prior phase
- `.planning/phases/01-correctness-and-stability/1-CONTEXT.md` — Phase 1 decisions (logging levels, error handling patterns)
- `.planning/phases/01-correctness-and-stability/01-VERIFICATION.md` — What was verified in Phase 1

### Research
- `.planning/research/SUMMARY.md` — Phase 2 approach: memory eviction, quest single-source-of-truth
- `.planning/research/FEATURES.md` — Table stakes for bounded data structures

### Requirements
- `.planning/REQUIREMENTS.md` — Requirements CORR-05, CORR-06, MEM-01, MEM-02, MEM-03

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **HologramInteractListener cooldowns (line 20):** `Map<String, Long>` with key format `playerUUID:hologramId`. Entries added at line 41 with `now + (cooldown * 50L)`. Expiry checked at line 38 but never cleaned up.
- **CosmeticManager standIndex (line 21):** `Map<UUID, CosmeticInstance>`. Already has 4 cleanup points (lines 91, 108, 115, 133). Well-synchronized.
- **PetManager standIndex (line 21):** `Map<UUID, PetInstance>`. Already has 7 cleanup points. Well-synchronized.
- **QuestManager.getFirstIncompleteIndex() (line 249-254):** Already centralized. 3 call sites (lines 229, 278, 334).
- **QuestManager.areObjectivesComplete() (lines 104-124):** FETCH handling at lines 116-119 uses `countMaterial()` for on-demand inventory check.
- **HologramVisibilityTracker world guard (lines 37-39):** Already implemented in Phase 1.

### Established Patterns
- **Tick counter pattern:** Tick tasks already run every 1 tick. Periodic sweeps can use a counter (`if (++sweepCounter % 6000 == 0)`) inside the existing tick method.
- **removeIf pattern:** Used in `tickAll()` across all managers. Same pattern works for cooldown eviction.

### Integration Points
- **HologramInteractListener:** Add periodic sweep call, either from HologramTickTask or a new counter inside the listener itself
- **CosmeticManager/PetManager:** Add safety-net sweep in their existing tickAll() with a frequency counter
- **QuestManager:** Modify action bar display for FETCH objectives to show live inventory count
- **QuestListener:** May need to fire progress events for FETCH during periodic checks

### Key Files (from scout)

| File | Lines | What's there | What's needed |
|------|-------|-------------|---------------|
| HologramInteractListener.java | 20, 35-41 | Unbounded cooldown map | Add periodic eviction sweep |
| CosmeticManager.java | 21, 91-133 | standIndex with 4 cleanup points ✓ | Add safety-net audit sweep |
| PetManager.java | 21, 90-198 | standIndex with 7 cleanup points ✓ | Add safety-net audit sweep |
| QuestManager.java | 249-254 | getFirstIncompleteIndex centralized ✓ | Verify only (CORR-05) |
| QuestManager.java | 104-124 | FETCH on-demand inventory check | Fix display, not logic (CORR-06) |
| HologramVisibilityTracker.java | 37-39 | World-null guard ✓ | Verify only (MEM-03) |

</code_context>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 02-memory-and-logic-correctness*
*Context gathered: 2026-03-21*
