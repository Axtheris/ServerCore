# Milestones

## v1.0 ServerCore Debug & Verification Sweep (Shipped: 2026-03-22)

**Phases completed:** 4 phases, 10 plans, 23 tasks

**Key accomplishments:**

- Per-instance try-catch added to all five tick loops, Logger injected into CosmeticManager and PetManager, world-null guards in three entity tick() methods preventing NPE on partial world unload
- 1. [Rule 3 - Blocker] Stale VaultHook.class prevented incremental compilation
- One-liner:
- 10-minute standIndex audit sweep added to CosmeticManager and PetManager using ++sweepCounter >= 12000 pattern with FINE-level eviction logging
- Inline MEM-03, CORR-05, and CORR-06 VERIFIED comments added to three files confirming existing code satisfies all three requirements with no logic changes
- Dirty-flag debounced YAML persistence with snapshot-then-async writes and atomic file swap for CosmeticStore, PetStore, and QuestStore — eliminates main-thread disk I/O on every cosmetic apply/remove
- Immutable ExploreTarget record in QuestObjective eliminates per-tick String.split() and Double.parseDouble() from handleExplore() by pre-parsing coordinates at config load time
- Optional permission field on HologramAction with per-action YAML-driven gating, plus inline Base64 validation of NPC skin data before it reaches PacketEvents
- `/servercore debug` operator diagnostic command — prints active instance counts for all nine systems, dirty/clean save state for three stores, present/absent soft-dep hooks, and running/stopped tick task status

---
