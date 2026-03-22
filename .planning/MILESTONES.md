# Milestones

## v2.4.0 Debug & Verification Sweep (Shipped: 2026-03-22)

**Phases completed:** 4 phases, 10 plans, 24 requirements

**Key accomplishments:**

- Per-instance try-catch in all tick loops with canonical null-check order across all nine systems
- Bounded memory via hologram cooldown eviction and standIndex safety-net sweeps
- Dirty-flag debounced YAML persistence with snapshot-then-async writes and atomic file swap
- Zero-allocation quest explore checks via pre-parsed ExploreTarget record
- Hologram action permission gating and NPC skin Base64 validation
- Live `/servercore debug` command with full 9-system diagnostics

---
