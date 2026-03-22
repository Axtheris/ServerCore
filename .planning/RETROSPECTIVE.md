# Retrospective

Living document capturing lessons learned across milestones.

## Milestone: v2.4.0 — ServerCore Debug & Verification Sweep

**Shipped:** 2026-03-22
**Phases:** 4 | **Plans:** 10

### What Was Built
- Per-instance try-catch tick loops with canonical null-check order across all nine systems
- Bounded memory via cooldown eviction and standIndex safety-net sweeps
- Dirty-flag debounced persistence with snapshot-then-async writes and atomic file swap
- Zero-allocation quest explore checks via pre-parsed ExploreTarget record
- Hologram action permission gating and NPC skin Base64 validation
- Live `/servercore debug` command with full 9-system diagnostics

### What Worked
- **Dependency-ordered phases:** Phase 1 (correctness) → Phase 2 (memory) → Phase 3 (persistence) → Phase 4 (security) gave each phase stable ground to build on
- **Verification-only requirements:** CORR-05, CORR-06, MEM-03 were already satisfied by existing code — verify-and-document approach saved effort vs. unnecessary rewrites
- **Tick counter pattern reuse:** Phase 2's sweep counter pattern was reused by Phase 3's SaveFlushTask, maintaining consistency
- **Parallel Wave 1 execution:** All phases had independent plans that could execute in parallel, maximizing throughput

### What Was Inefficient
- **Phase 2 ROADMAP checkbox not auto-updated:** Phase 2 shows `roadmap_complete: false` despite all plans and verification passing — manual checkbox tracking
- **Quest/NPC shutdown ordering missed:** The onDisable() audit in Phase 1 Plan 03 fixed emitter/pet ordering but missed quest/NPC block — integration checker caught it at milestone audit
- **SUMMARY.md one-liners inconsistent:** Not all summaries had extractable one-liner frontmatter, making automated accomplishment extraction unreliable

### Patterns Established
- WARNING for operational issues, SEVERE for I/O failures (consistent across all phases)
- `++sweepCounter >= N` with explicit reset for periodic sweeps (Phases 2, 3)
- Snapshot-then-async with volatile `saving` guard for main-thread-safe persistence
- `ExploreTarget` nested record pattern for pre-parsed config data

### Key Lessons
- **Verify existing code before fixing:** Multiple requirements were already satisfied — codebase scout before planning saves significant effort
- **Integration checks catch ordering bugs:** The quest/NPC shutdown ordering defect was invisible at the per-phase level but surfaced in cross-phase integration analysis
- **Atomic file writes need Windows fallback:** `ATOMIC_MOVE` throws on NTFS when target exists — always add `REPLACE_EXISTING` fallback

### Cost Observations
- Model mix: ~60% sonnet (research, checking, verification), ~40% opus (planning, execution)
- Sessions: 1 session for full autonomous run
- Notable: Parallel execution of Wave 1 plans cut wall-clock time roughly in half per phase

---

## Cross-Milestone Trends

| Milestone | Phases | Plans | Requirements | Duration |
|-----------|--------|-------|-------------|----------|
| v2.4.0 | 4 | 10 | 24/24 | 1 day |
