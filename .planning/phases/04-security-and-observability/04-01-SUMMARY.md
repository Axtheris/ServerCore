---
phase: 04-security-and-observability
plan: 01
subsystem: hologram, npc
tags: [security, permissions, base64, validation, hologram-actions, npc-skins, packetevents]

# Dependency graph
requires:
  - phase: 03-async-persistence-and-performance
    provides: hologram config load/save round-trip foundation
provides:
  - HologramAction with optional permission field parsed from YAML and checked per-action
  - NPC skin Base64 validation at load time before PacketEvents receives data
affects: [hologram system, npc system, any external plugins using HologramAction.parse()]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Per-action permission gate in listener loop (silent skip with continue)
    - Inline Base64 structural validation with WARNING log naming the entity id
    - YAML round-trip pattern for optional security fields

key-files:
  created: []
  modified:
    - src/main/java/net/axther/serverCore/hologram/action/HologramAction.java
    - src/main/java/net/axther/serverCore/hologram/config/HologramConfig.java
    - src/main/java/net/axther/serverCore/hologram/listener/HologramInteractListener.java
    - src/main/java/net/axther/serverCore/npc/config/NPCConfig.java
    - src/test/java/net/axther/serverCore/hologram/HologramDataModelTest.java

key-decisions:
  - "Permission check placed in listener loop (not in execute()) — action object stays dumb, gate is responsibility of the caller (D-02)"
  - "HologramClickEvent fires unconditionally BEFORE permission filtering — external plugins see every click regardless of permission (D-03)"
  - "Permission field is null when absent (not empty string) — null check is the gate, no special sentinel value needed"
  - "Base64 validation uses java.util.Base64.getDecoder().decode() throwing IllegalArgumentException — JDK stdlib, no deps"
  - "Malformed texture nulls BOTH texture and signature (skin skipped entirely); malformed signature only nulls signature (texture preserved)"
  - "WARNING log level for malformed skin data — matches Phase 1 convention (WARNING for config errors, SEVERE for I/O failures)"

patterns-established:
  - "Permission gate pattern: action.getPermission() != null && !player.hasPermission(action.getPermission()) → continue"
  - "Optional YAML field round-trip: read with null fallback, write with null guard (if != null)"
  - "Inline Base64 validation: try { Base64.getDecoder().decode(field); } catch (IllegalArgumentException e) { log WARNING + null field }"

requirements-completed: [SEC-01, CONF-02, SEC-02]

# Metrics
duration: 2min
completed: 2026-03-21
---

# Phase 4 Plan 1: Hologram Action Permission Gating and NPC Skin Base64 Validation Summary

**Optional permission field on HologramAction with per-action YAML-driven gating, plus inline Base64 validation of NPC skin data before it reaches PacketEvents**

## Performance

- **Duration:** 2 min
- **Started:** 2026-03-21T00:13:02Z
- **Completed:** 2026-03-21T00:15:00Z
- **Tasks:** 2
- **Files modified:** 5

## Accomplishments
- Added optional `permission` field to `HologramAction` — parsed from YAML, round-tripped through saveAll, and checked per-action in the interact listener
- Players lacking the required permission are silently skipped; other actions on the same hologram still execute
- `HologramClickEvent` continues to fire unconditionally before permission filtering (external plugin invariant preserved)
- NPC skin texture and signature fields are validated as proper Base64 at load time with named WARNING logs
- Malformed skin data is rejected before it can be sent to clients via PacketEvents

## Task Commits

Each task was committed atomically:

1. **Task 1: Hologram action permission gating** - `c1425e8` (feat)
2. **Task 2: NPC skin Base64 validation** - `0429759` (feat)

## Files Created/Modified
- `src/main/java/net/axther/serverCore/hologram/action/HologramAction.java` - Added `permission` field, 3-param constructor and parse factory, `getPermission()` getter
- `src/main/java/net/axther/serverCore/hologram/config/HologramConfig.java` - Parse `actPerm` from YAML action map in loadAll(); write permission back in saveAll()
- `src/main/java/net/axther/serverCore/hologram/listener/HologramInteractListener.java` - Per-action permission check with `continue` before `execute()`
- `src/main/java/net/axther/serverCore/npc/config/NPCConfig.java` - Inline Base64 validation block for skinTexture and skinSignature after read, before NPC constructor call
- `src/test/java/net/axther/serverCore/hologram/HologramDataModelTest.java` - Updated all HologramAction.parse() call sites to 3-param signature

## Decisions Made
- Permission check placed in listener loop, not in `execute()` — action object stays dumb, the caller (listener) is responsible for the gate
- `HologramClickEvent` fires unconditionally before the permission-filtered action loop — external plugins see every click regardless of permission settings
- `permission` field uses `null` for absent (not empty string) — null check is the gate
- Base64 validation uses JDK stdlib `java.util.Base64.getDecoder().decode()` — no additional dependencies
- Malformed texture nulls both texture AND signature (the skin is skipped entirely); malformed signature only nulls signature
- WARNING level matches Phase 1 convention for config errors

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Updated HologramDataModelTest.java call sites to 3-param parse() signature**
- **Found during:** Task 1 (Add permission field to HologramAction)
- **Issue:** Changing `parse(String, String)` to `parse(String, String, String)` broke 7 existing test call sites — build would fail
- **Fix:** Updated all `HologramAction.parse(type, value)` calls to `HologramAction.parse(type, value, null)` in the test file
- **Files modified:** `src/test/java/net/axther/serverCore/hologram/HologramDataModelTest.java`
- **Verification:** `./gradlew build` passes with all tests passing
- **Committed in:** `c1425e8` (Task 1 commit)

---

**Total deviations:** 1 auto-fixed (1 bug — compile error from signature change)
**Impact on plan:** Required fix for correctness; no scope creep. Test semantics unchanged (null permission = no restriction).

## Issues Encountered
None beyond the auto-fixed test signature update.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- SEC-01, CONF-02, SEC-02 requirements satisfied
- Hologram permission field is backward-compatible — existing YAML configs without a `permission` key load with null (no restriction) — no migration needed
- Plan 04-02 (observability) can proceed independently

---
*Phase: 04-security-and-observability*
*Completed: 2026-03-21*

## Self-Check: PASSED

- FOUND: src/main/java/net/axther/serverCore/hologram/action/HologramAction.java
- FOUND: src/main/java/net/axther/serverCore/hologram/config/HologramConfig.java
- FOUND: src/main/java/net/axther/serverCore/hologram/listener/HologramInteractListener.java
- FOUND: src/main/java/net/axther/serverCore/npc/config/NPCConfig.java
- FOUND: .planning/phases/04-security-and-observability/04-01-SUMMARY.md
- FOUND commit: c1425e8 (Task 1)
- FOUND commit: 0429759 (Task 2)
