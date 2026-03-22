# Phase 03: Async Persistence and Performance - Research

**Researched:** 2026-03-21
**Domain:** Bukkit async I/O, YAML persistence, Java NIO atomic file operations, allocation-free tick patterns
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Dirty-flag scope (PERS-01)**
- D-01: Add dirty-flag tracking to all three data stores (CosmeticStore, PetStore, QuestStore) for consistency, even though PetStore and QuestStore currently only save in `onDisable()`.
- D-02: CosmeticStore is the critical fix — it currently calls `store.save(this)` on every `applyCosmetic()` (line 79) and `removeCosmetics()` (line 97), producing full-file YAML serialization and disk write on the main thread per operation.
- D-03: Mutation methods (`addPet`, `removePet`, quest accept/complete/abandon, cosmetic apply/remove) set the dirty flag instead of triggering immediate saves. The store tracks `private boolean dirty = false` with `markDirty()` and `isDirty()` accessors.

**Periodic flush interval (PERS-01)**
- D-04: A periodic flush runs every 6000 ticks (5 minutes) for each store.
- D-05: The flush is driven by the existing tick task infrastructure — either a counter in the manager's `tickAll()` or a dedicated low-frequency BukkitRunnable. Claude's discretion on which approach is cleaner.
- D-06: The flush checks `isDirty()` before writing — if the store is clean, the flush is a no-op.

**Snapshot-then-async pattern (PERS-02)**
- D-07: Save cycle: (1) main thread snapshots data into a `YamlConfiguration` object, (2) async thread writes the YamlConfiguration to a temp file (`cosmetic-data.yml.tmp`), (3) async thread renames temp to real file.
- D-08: The snapshot captures a deep copy of all data needed for serialization.
- D-09: Only one async write can be in flight per store at a time. A `private volatile boolean saving = false` guard prevents overlapping writes. If a flush finds `saving == true`, it skips that cycle.
- D-10: The async task uses `Bukkit.getScheduler().runTaskAsynchronously(plugin, ...)`.

**Synchronous flush in onDisable (PERS-03)**
- D-11: `onDisable()` performs a SYNCHRONOUS save regardless of dirty flag — writes all data directly on the main thread.
- D-12: The existing `onDisable()` pattern (cancel task → save → destroy) stays correct.
- D-13: Add a `saveSync()` / `saveAsync()` distinction, or a boolean parameter `async` to the save method.

**Atomic write safety (PERS-02)**
- D-14: Write to `<filename>.tmp` first, then use `Files.move(tmp, real, ATOMIC_MOVE, REPLACE_EXISTING)` to atomically swap. If `ATOMIC_MOVE` fails (some filesystems), fall back to `REPLACE_EXISTING` alone.
- D-15: On startup, if a `.tmp` file exists alongside the real file, delete the `.tmp` file. If ONLY a `.tmp` file exists (real file missing), rename `.tmp` to real.

**Explore coordinate pre-parsing (PERF-01)**
- D-16: Add an immutable record `ExploreTarget(String worldName, double x, double y, double z)` to QuestObjective (or as a nested class/record).
- D-17: Parse the target string `"world,100,64,200"` at config load time in `QuestObjective.fromConfig()` (line 53).
- D-18: `QuestManager.handleExplore()` (line 342) uses `obj.getExploreTarget()` instead of `obj.getTarget().split(",")`.
- D-19: World lookup stays at check time — the pre-parsed record stores the world NAME, not a World reference.
- D-20: Malformed explore targets caught at config load time with a WARNING log; the objective is skipped.

### Claude's Discretion
- Whether the periodic flush uses a counter in existing tick tasks or a separate BukkitRunnable
- Whether the snapshot method is on the Store class or extracted to a helper
- Whether `saveSync()`/`saveAsync()` are separate methods or a boolean parameter
- Exact placement of the `ExploreTarget` record (nested in QuestObjective vs. standalone class)
- Whether to combine all three store flushes into one periodic task or keep separate

### Deferred Ideas (OUT OF SCOPE)
None — discussion stayed within phase scope
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| PERS-01 | Data stores use dirty-flag with periodic batch write instead of saving on every mutation | Dirty-flag pattern, flush counter in tickAll(), `markDirty()` / `isDirty()` API |
| PERS-02 | Async saves use snapshot-then-async pattern — main thread snapshots data, async thread serializes to disk | `Bukkit.getScheduler().runTaskAsynchronously()`, `volatile boolean saving`, `YamlConfiguration` snapshot, `Files.move()` with `ATOMIC_MOVE` |
| PERS-03 | `onDisable()` synchronous flush guarantees all pending dirty data is written before process exits | `saveSync()` path bypasses async, called from onDisable() unconditionally |
| PERF-01 | Quest explore objective target coordinates are pre-parsed at QuestConfig load time — no per-tick `String.split()` allocation | Java 16+ `record` type for `ExploreTarget`, parse in `QuestObjective.fromConfig()`, null return for non-EXPLORE objectives |
</phase_requirements>

---

## Summary

Phase 3 addresses two independent problems. The persistence problem (PERS-01/PERS-02/PERS-03) is a main-thread blocking I/O issue: CosmeticStore currently calls `config.save(file)` — a full YAML serialization + disk write — synchronously on the main thread inside `applyCosmetic()` and `removeCosmetics()`. This can cause tick spikes during normal gameplay. PetStore and QuestStore only save in `onDisable()` today, but they share the same pattern risk. The fix is dirty-flag debouncing with a snapshot-then-async write cycle, plus a guaranteed synchronous path for server shutdown.

The performance problem (PERF-01) is a per-tick allocation in `QuestManager.handleExplore()`. On every tick, for every active player with an explore quest, the code calls `obj.getTarget().split(",")` which allocates a new String array, plus three `Double.parseDouble()` calls that allocate boxed Double intermediates. For a server with 20 players each on an explore quest, this is 20 array allocations + 60 parse calls per tick from a source string that never changes after config load. The fix is pre-parsing into an immutable record at config load time.

The two problems are entirely independent changes — they can be planned as separate tasks. Both use established patterns already present in this codebase (tick counters from Phase 2, BukkitRunnable async from existing Bukkit documentation, Java records introduced in Java 16).

**Primary recommendation:** Implement dirty-flag + flush counter per store, then snapshot-then-async write. Plan tasks in store-by-store order (CosmeticStore first as the critical bug, then PetStore, then QuestStore), followed by the ExploreTarget record extraction.

---

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `org.bukkit.configuration.file.YamlConfiguration` | Paper 1.21.x | YAML serialization/deserialization and in-memory snapshot | Already used by all three stores; snapshot is just `new YamlConfiguration()` populated on main thread |
| `Bukkit.getScheduler().runTaskAsynchronously()` | Paper 1.21.x | Dispatch async write task off main thread | Standard Bukkit async API; no extra dependency |
| `java.nio.file.Files.move()` | JDK 21 | Atomic file rename for crash-safe swap | Available in all JDK versions; `StandardCopyOption.ATOMIC_MOVE` + `REPLACE_EXISTING` |
| Java 16+ `record` | JDK 21 | Immutable `ExploreTarget` value type | Already on Java 21 target; zero-boilerplate for coordinate tuple |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `java.util.concurrent.atomic.AtomicBoolean` | JDK 21 | Alternative to `volatile boolean saving` for the in-flight guard | Either works; `volatile boolean` is simpler, `AtomicBoolean` is more idiomatic for cross-thread flags |
| `java.nio.file.Path` | JDK 21 | Construct `.tmp` file path from real file path | Prefer over `new File(parent, name + ".tmp")` for NIO operations |

**Version verification:** All standard library — no external packages to version-check. All APIs are part of JDK 21 and Paper 1.21.x which are already on the classpath.

---

## Architecture Patterns

### Pattern 1: Dirty-Flag + Flush Counter in tickAll()

**What:** Each Store class tracks `private boolean dirty = false`. Mutation call sites call `store.markDirty()` instead of `store.save()`. The Manager's `tickAll()` already has a `sweepCounter` (Phase 2 established this). Add a second counter `saveFlushCounter` that triggers `store.flushIfDirty()` every 6000 ticks.

**When to use:** When you want periodic batch writes without a separate timer task. The existing tick infrastructure already runs every tick — piggybacking is zero overhead beyond the counter increment.

**Existing pattern (Phase 2 sweep counter from CosmeticManager.java:130):**
```java
// Already in CosmeticManager.tickAll():
if (++sweepCounter >= 12000) {
    sweepCounter = 0;
    // standIndex audit
}
```

**Phase 3 addition — flush counter follows the same idiom:**
```java
// Add to CosmeticManager.tickAll():
if (++saveFlushCounter >= 6000) {
    saveFlushCounter = 0;
    if (store != null) store.flushIfDirty(this);
}
```

**Note on quest manager tick coverage:** QuestManager has NO existing tick task. The periodic flush for QuestStore must be driven differently — either a dedicated low-frequency `BukkitRunnable` for QuestStore, or a counter inside the QuestListener's player-move handler. The cleanest approach (Claude's discretion) is a standalone `SaveFlushTask` (a single BukkitRunnable running every 6000 ticks via `runTaskTimer(plugin, 6000L, 6000L)`) that flushes all three stores at once. This is especially clean because QuestStore has no manager tick to piggyback on.

### Pattern 2: Snapshot-Then-Async Write

**What:** Split the current `save()` method into two phases: (1) snapshot on main thread (fast, in-memory only), (2) write on async thread (slow, disk I/O).

**Existing CosmeticStore.save() structure (lines 30-77):** Already iterates manager data and builds a `YamlConfiguration`. This iteration IS the snapshot — it just needs to happen on the main thread, and then hand off the populated `YamlConfiguration` to an async task.

**Pattern:**
```java
// In CosmeticStore — called from main thread (tick flush or onDisable sync path)
public void flushIfDirty(CosmeticManager manager) {
    if (!dirty) return;
    if (saving) return;  // async write still in flight, skip this cycle
    saving = true;
    dirty = false;

    // Snapshot on main thread — fast in-memory copy only
    YamlConfiguration snapshot = buildSnapshot(manager);  // extract from existing save()

    // Hand off to async thread
    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> writeSnapshot(snapshot));
}

private void writeSnapshot(YamlConfiguration snapshot) {
    // Runs on async thread — blocking I/O is safe here
    try {
        Path tmp = file.toPath().resolveSibling(file.getName() + ".tmp");
        snapshot.save(tmp.toFile());
        try {
            Files.move(tmp, file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    } catch (IOException e) {
        plugin.getLogger().log(Level.SEVERE, "Failed to save cosmetic data", e);
    } finally {
        saving = false;
    }
}
```

**Thread-safety note:** The `saving` flag must be `volatile` (not plain boolean) because it is written by the async thread and read by the main thread. The existing store data structures (HashMap, ArrayList) are only ever accessed from the main thread (event handlers + tick tasks run on the server thread). The snapshot captures copies of the data, so there is no concurrent access to the live collections after the snapshot is taken.

### Pattern 3: saveSync() for onDisable

**What:** `onDisable()` must write synchronously because async tasks submitted after `onDisable()` starts may not execute before the JVM exits. The existing `onDisable()` calls `store.save()` directly — keep this working.

**Implementation choice:** Two methods is cleaner than a boolean parameter because it makes the call site's intent self-documenting:
```java
// Called from onDisable() — blocks main thread, no async dispatch
public void saveSync(CosmeticManager manager) {
    dirty = false;  // clear before write so flag is consistent
    YamlConfiguration snapshot = buildSnapshot(manager);
    writeSnapshotSync(snapshot);  // same write logic, no async dispatch
}
```

**onDisable() change:** Replace `cosmeticStore.save(cosmeticManager)` with `cosmeticStore.saveSync(cosmeticManager)`. The `flushIfDirty()` path is never called from onDisable.

### Pattern 4: Startup .tmp Cleanup

**What:** On `CosmeticStore` (and PetStore, QuestStore) construction or load, check for orphaned `.tmp` files and clean them up.

```java
public void load(CosmeticManager manager) {
    // D-15: Clean up any orphaned .tmp file from a prior crashed write
    Path tmp = file.toPath().resolveSibling(file.getName() + ".tmp");
    if (Files.exists(tmp)) {
        if (!file.exists()) {
            // Successful write but rename failed — promote the .tmp
            try { Files.move(tmp, file.toPath()); }
            catch (IOException e) { /* log warning */ }
        } else {
            // Normal case: real file exists, .tmp is a failed write — delete it
            try { Files.deleteIfExists(tmp); }
            catch (IOException e) { /* log warning */ }
        }
    }
    // ... existing load logic
}
```

### Pattern 5: ExploreTarget Record

**What:** Java 16+ record for the pre-parsed coordinate tuple. Nested inside `QuestObjective` (Claude's discretion — nested is cleaner since it is only used by QuestObjective and QuestManager).

```java
// Inside QuestObjective.java
public record ExploreTarget(String worldName, double x, double y, double z) {}

private final ExploreTarget exploreTarget; // null for non-EXPLORE objectives

// In fromConfig() switch case "explore":
String locationStr = section.getString("location", "world,0,64,0");
ExploreTarget parsed = parseExploreTarget(locationStr);
if (parsed == null) {
    plugin.getLogger().warning("...");
    // skip or use default
}
```

**In QuestManager.handleExplore() — after the fix:**
```java
ExploreTarget target = obj.getExploreTarget();
if (target == null) continue;  // non-EXPLORE or malformed — skipped at load
if (location.getWorld() == null ||
    !location.getWorld().getName().equalsIgnoreCase(target.worldName())) continue;
double distSq = (location.getX() - target.x()) * (location.getX() - target.x())
              + (location.getY() - target.y()) * (location.getY() - target.y())
              + (location.getZ() - target.z()) * (location.getZ() - target.z());
if (distSq <= obj.getRadius() * obj.getRadius()) { ... }
```

Note: Using `(x - tx) * (x - tx)` instead of `Math.pow(x - tx, 2)` is also a micro-optimization (no autoboxing) and matches current code style.

### Recommended Project Structure (no structural changes needed)

All changes are in-place modifications to existing files. No new packages or top-level classes required except possibly:
- `ExploreTarget` as a nested record inside `QuestObjective` (preferred) or a standalone class in the `quest` package
- Optional: `SaveFlushTask` in a new `task/` subdirectory under each system, or a shared one — but this is only needed if QuestStore flush is driven by a dedicated task

### Anti-Patterns to Avoid

- **Capturing live collection references in the async Runnable:** The lambda passed to `runTaskAsynchronously` must receive a detached snapshot (`YamlConfiguration`), not a reference to `activeCosmetics`. Capturing `this` or a live map is a race condition.
- **Leaving `dirty` true during async write:** Marking dirty=false at the point of snapshot (not at write completion) means a mutation that happens AFTER the snapshot is taken but BEFORE the write finishes will set dirty=true again — correct behavior. Do not wait for write completion to clear the flag.
- **Async writes in onDisable:** Paper's scheduler may not run async tasks submitted during `onDisable()`. Always use the sync path from shutdown. (This is confirmed in Bukkit documentation and community consensus — async tasks submitted during disable may be silently dropped.)
- **`saving = false` outside a finally block:** If `snapshot.save()` throws, `saving` would stay `true` forever, permanently blocking all future saves. Always reset in `finally`.
- **Using `new File(parent, name + ".tmp")` for NIO operations:** Prefer `file.toPath().resolveSibling(...)` to stay in the NIO API and avoid mixing `File` and `Path` APIs.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Async task dispatch | Custom thread pool or ExecutorService | `Bukkit.getScheduler().runTaskAsynchronously()` | Bukkit scheduler integrates with server lifecycle; custom thread pools bypass shutdown sequencing |
| Atomic file swap | Custom rename logic with fallback | `Files.move(tmp, real, ATOMIC_MOVE, REPLACE_EXISTING)` + `AtomicMoveNotSupportedException` catch | NIO handles OS differences; ATOMIC_MOVE is a single syscall on Linux, best-effort on Windows |
| YAML serialization | Custom string builder for YAML | `YamlConfiguration.save(File)` (unchanged) | Already handles escaping, indentation, list serialization correctly |
| Periodic timer | Dedicated background thread with `Thread.sleep()` | Counter in tickAll() or `BukkitRunnable.runTaskTimer()` | Background threads don't integrate with server tick; Bukkit scheduler handles thread management |

**Key insight:** The Bukkit scheduler and NIO file APIs together provide everything needed. The core work is reorganizing existing code (extract snapshot from save, add dirty flag, add flush counter) — not introducing new mechanisms.

---

## Common Pitfalls

### Pitfall 1: Async Write Racing Concurrent Mutation
**What goes wrong:** An async write reads a live `HashMap<UUID, List<CosmeticInstance>>` while the main thread modifies it (e.g., player applies a cosmetic mid-write cycle). ConcurrentModificationException or corrupt YAML output.
**Why it happens:** HashMap is not thread-safe. The async thread and main thread access the same object concurrently.
**How to avoid:** Build the entire `YamlConfiguration` snapshot on the main thread (synchronously) before dispatching the async task. The async thread receives only a `YamlConfiguration` object (fully built, no live references). The `YamlConfiguration` itself is not shared back — it is write-once from the main thread's perspective.
**Warning signs:** `ConcurrentModificationException` in async stack traces; YAML files with truncated entries.

### Pitfall 2: saving Flag Not volatile
**What goes wrong:** The main thread reads `saving == false` from its CPU cache even though the async thread has set it to `true`. Two async writes start simultaneously. File corruption on some OS + JVM combinations.
**Why it happens:** Without `volatile`, Java's memory model does not guarantee cross-thread visibility for plain boolean fields.
**How to avoid:** Declare `private volatile boolean saving = false`. This is the minimal synchronization needed for a single-writer (async thread writes, main thread reads) pattern.
**Warning signs:** Overlapping tmp files; async write exception about file-in-use (Windows).

### Pitfall 3: Async Tasks Not Running After onDisable
**What goes wrong:** Server shuts down, `onDisable()` dispatches an async task via `runTaskAsynchronously`, the task never runs, data is lost.
**Why it happens:** Bukkit cancels all pending and in-flight tasks when the plugin disables. The async scheduler is shut down as part of server stop.
**How to avoid:** `onDisable()` always calls `saveSync()` directly. Never call `flushIfDirty()` (which uses the async path) from `onDisable()`.
**Warning signs:** Data reverts to pre-session state after clean server shutdown.

### Pitfall 4: QuestObjective fromConfig Receives No Plugin Logger
**What goes wrong:** D-20 requires a WARNING log when an explore target is malformed. But `QuestObjective.fromConfig()` is a static factory method that currently has no logger reference — it cannot call `plugin.getLogger()`.
**Why it happens:** The static factory design (established before this phase) doesn't pass a logger.
**How to avoid:** Two options: (a) return null from the explore case and log the warning in the `QuestConfig` caller that has a logger reference; or (b) throw a descriptive `IllegalArgumentException` caught by the caller. Option (a) is consistent with the project's null-for-absent pattern. The `ExploreTarget` constructor should throw `IllegalArgumentException` on bad input; `fromConfig()` catches and returns null; `QuestConfig.loadAll()` logs and skips.
**Warning signs:** Silent bad-parse behavior, objectives with `x=0, y=0, z=0` from failed parses (currently caught via NumberFormatException that is silently ignored).

### Pitfall 5: Files.move() with ATOMIC_MOVE on Windows
**What goes wrong:** `Files.move(tmp, real, ATOMIC_MOVE)` throws `AtomicMoveNotSupportedException` on Windows when the source and destination are on NTFS (even same volume, different directories) or when the target already exists.
**Why it happens:** Windows NTFS does not support atomic rename-over-existing-file in the same way Linux does. The target must not exist, or must be on the same volume.
**How to avoid:** Always catch `AtomicMoveNotSupportedException` and fall back to `Files.move(tmp, real, REPLACE_EXISTING)`. The dev environment is Windows (confirmed in project env); test the fallback path. The try/catch is mandatory, not optional.
**Warning signs:** `AtomicMoveNotSupportedException` in logs on dev server; works on Linux CI but fails locally.

### Pitfall 6: Snapshot Iterates Bukkit.getEntity() on Async Thread
**What goes wrong:** CosmeticStore.save() currently calls `Bukkit.getEntity(mobUuid)` (line 42) to determine entity type and world for active cosmetics. If this lookup is in the snapshot code, it runs on the async thread, which is not allowed in Bukkit — entity lookups are main-thread only.
**Why it happens:** The existing save() code was written for synchronous use. Moving it to async without auditing all API calls creates a subtle thread-safety violation.
**How to avoid:** The snapshot code (which runs on the main thread) must capture everything needed — entity type, world name, item serialization — before the async task is dispatched. No Bukkit API calls (especially `Bukkit.getEntity()`) should remain in the async write path. Audit every line of the snapshot method for Bukkit API calls.
**Warning signs:** Occasional NPEs in async stack traces; entity lookups returning null on the async thread.

---

## Code Examples

### Adding dirty flag to a store (minimal, correct pattern)
```java
// Source: established Java volatile pattern for single-writer cross-thread visibility
private boolean dirty = false;
private volatile boolean saving = false;  // volatile: written async, read main

public void markDirty() { dirty = true; }
public boolean isDirty() { return dirty; }
```

### Flush counter in tickAll() — matching Phase 2 sweep pattern
```java
// Source: CosmeticManager.java:130 (Phase 2 sweep counter pattern, same repo)
// Add second counter alongside existing sweepCounter
private int saveFlushCounter = 0;

// In tickAll():
if (++saveFlushCounter >= 6000) {
    saveFlushCounter = 0;
    if (store != null) store.flushIfDirty(this);
}
```

### Atomic file move with Windows fallback
```java
// Source: java.nio.file.Files.move javadoc, JDK 21
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;

Path tmp = file.toPath().resolveSibling(file.getName() + ".tmp");
try {
    Files.move(tmp, file.toPath(),
        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
} catch (AtomicMoveNotSupportedException ignored) {
    Files.move(tmp, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
}
```

### ExploreTarget record (Java 21, nested in QuestObjective)
```java
// Source: JEP 395 (Records), available in Java 16+; project targets Java 21
public record ExploreTarget(String worldName, double x, double y, double z) {
    // compact constructor for validation
    public ExploreTarget {
        if (worldName == null || worldName.isEmpty())
            throw new IllegalArgumentException("worldName must not be empty");
    }
}
```

### Parsing ExploreTarget safely at config load time
```java
// Source: QuestObjective.fromConfig() line 53, this repo
private static ExploreTarget parseExploreTarget(String locationStr) {
    if (locationStr == null) return null;
    String[] parts = locationStr.split(",");
    if (parts.length != 4) return null;
    try {
        return new ExploreTarget(
            parts[0].trim(),
            Double.parseDouble(parts[1].trim()),
            Double.parseDouble(parts[2].trim()),
            Double.parseDouble(parts[3].trim())
        );
    } catch (NumberFormatException e) {
        return null;  // caller logs warning
    }
}
```

### QuestStore snapshot — deep copy required for mutable int[] in QuestProgress
```java
// Source: QuestProgress.java — objectiveProgress is int[], mutable, must be copied
// In buildSnapshot() for QuestStore:
for (QuestProgress progress : entry.getValue()) {
    // Must copy int[] — it's mutable and modified by quest progress handlers
    int[] progressCopy = Arrays.copyOf(progress.getObjectiveProgress(),
                                        progress.getObjectiveProgress().length);
    List<Integer> progressList = new ArrayList<>();
    for (int val : progressCopy) progressList.add(val);
    config.set(path + ".active." + progress.getQuestId() + ".progress", progressList);
}
```

### PetStore snapshot — deep copy of ownedPets (Set<String> values are immutable Strings)
```java
// Source: PetStore.java:16 — ownedPets is Map<UUID, Set<String>>
// Strings are immutable; copy the Set to avoid iterator/modification issues
Map<UUID, Set<String>> snapshot = new HashMap<>();
for (var entry : ownedPets.entrySet()) {
    snapshot.put(entry.getKey(), new LinkedHashSet<>(entry.getValue()));
}
// Pass snapshot (not ownedPets) to the async write path
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Sync save on every mutation (current CosmeticManager lines 79, 97) | Dirty-flag + periodic async flush | Phase 3 | Eliminates per-operation disk I/O from main thread |
| Per-tick `String.split(",")` in handleExplore (current line 342) | Pre-parsed `ExploreTarget` record at config load | Phase 3 | Zero allocation per explore tick |
| Overwrite-in-place file save (current `config.save(file)`) | Write-to-temp then atomic rename | Phase 3 | YAML file never in a partially-written state |

**Not deprecated:**
- `YamlConfiguration.save(File)` — still correct, just called from async thread via temp file path
- `Bukkit.getScheduler().runTaskAsynchronously()` — current standard for off-thread work in Paper plugins
- The overall Store pattern (YamlConfiguration-based persistence) — no change to format or structure, only timing and threading

---

## Implementation Notes (Discretion Guidance)

### Discretion: Flush task placement

**Recommendation: Single shared SaveFlushTask** running every 6000 ticks (initial delay 6000, period 6000). One task flushes all three stores. Reasons:
1. QuestManager has no tickAll() — there is no tick counter to piggyback on for QuestStore.
2. Adding `saveFlushCounter` to CosmeticManager and PetManager works, but then QuestStore needs a third mechanism. A single shared task is simpler.
3. One task vs. three tasks is negligible — BukkitRunnable has minimal overhead.
4. Counterpoint: Keeping flush inside tickAll() for Cosmetic/Pet (following Phase 2 sweep counter pattern) is valid for consistency. If chosen, QuestStore gets a dedicated single-store flush task.

**Either approach is correct.** The planner should pick one and use it consistently across all three stores.

### Discretion: saveSync() vs. boolean parameter

**Recommendation: Two separate methods** (`flushIfDirty(manager)` for async, `saveSync(manager)` for shutdown). Reasons:
1. `onDisable()` call sites are self-documenting: `cosmeticStore.saveSync(cosmeticManager)` makes the synchronous contract obvious.
2. The boolean parameter form `save(manager, true/false)` can be accidentally called with wrong value; two methods make the contract enforced at compile time.
3. The internal `writeSnapshot()` and `writeSnapshotSync()` helper methods can share the file-write logic without the async dispatch.

### Discretion: ExploreTarget placement

**Recommendation: Nested record inside QuestObjective**. It is only used by `QuestObjective` (stores it) and `QuestManager` (reads it). No other class needs it. Nested records in Java are implicitly static.

### Discretion: Snapshot method location

**Recommendation: Private method on the Store class** (e.g., `CosmeticStore.buildSnapshot(CosmeticManager manager)`). It operates on store-private state (the `pending` map) as well as manager state, and is a detail of the persistence mechanism. No need to extract to a helper.

---

## Open Questions

1. **Is `saving = false` in the finally block of the async Runnable sufficient, or should there be a happens-before guarantee for the snapshot data?**
   - What we know: The `YamlConfiguration` snapshot is built on the main thread and handed to the async runnable via the lambda capture. Java's lambda capture provides a happens-before guarantee for the captured reference. The async thread reads the snapshot (immutable after construction); the main thread does not touch it after dispatch.
   - What's unclear: Whether `YamlConfiguration` itself is safe to read from an async thread (it's a plain Java object, no synchronization). In practice it is read-only after `buildSnapshot()` returns, so there is no race — but this is worth a comment in the code.
   - Recommendation: Add inline comment confirming snapshot is read-only after main thread hands it off.

2. **Does Paper 1.21 cancel in-flight async tasks when onDisable() is called mid-write?**
   - What we know: The synchronous flush in `onDisable()` (D-11) makes the question moot for data correctness. If an async write is in flight when `onDisable()` starts, the `saving` flag will be `true`; the sync flush proceeds immediately without waiting.
   - What's unclear: Whether the in-flight async write's `finally { saving = false; }` can run after `onDisable()` completes and the plugin object is gone.
   - Recommendation: Accept this as a known edge case. The sync flush ensures data is saved. The in-flight async write either completes (redundant but harmless) or is cancelled mid-stream (tmp file left behind, cleaned up on next startup per D-15). Data integrity is maintained either way.

3. **CosmeticStore.save() currently calls Bukkit.getEntity() during serialization (line 42) to get entity type and world name for active cosmetics.**
   - What we know: This call must NOT be in the async write path. The snapshot must capture entity type and world name on the main thread.
   - What's unclear: Whether `CosmeticInstance` stores the entity type and world name (avoiding the Bukkit API call entirely), or whether the snapshot must call `Bukkit.getEntity()` at snapshot time.
   - Recommendation: The snapshot runs on the main thread, so `Bukkit.getEntity()` is safe there. Alternatively, `CosmeticInstance` could cache entity type (it has `cachedMob` WeakReference). Either works. The planner should ensure the snapshot is the only place this call happens.

---

## Sources

### Primary (HIGH confidence)
- Java NIO `Files.move()` javadoc, JDK 21 — `ATOMIC_MOVE`, `REPLACE_EXISTING`, `AtomicMoveNotSupportedException`
- Bukkit/Paper API `BukkitScheduler.runTaskAsynchronously()` — standard async task dispatch pattern
- JEP 395 (Java Records, JDK 16+) — `record` type semantics and nested record behavior
- Java Memory Model — `volatile` semantics for cross-thread flag visibility (Java Language Specification §17.4)
- CosmeticManager.java lines 130-138 (this repo, Phase 2) — sweep counter pattern used as template

### Secondary (MEDIUM confidence)
- Paper plugin development community: async tasks submitted in `onDisable()` are unreliable — widely documented in Paper/Spigot forums and consistent with Bukkit scheduler lifecycle docs
- `AtomicMoveNotSupportedException` on Windows NTFS — documented in JDK source and confirmed in multiple Java file I/O community references

### Tertiary (LOW confidence — not applicable)
None required. All findings verified against JDK documentation and direct code inspection.

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all APIs are JDK 21 + Paper 1.21.x already in project; no new dependencies
- Architecture: HIGH — patterns derived directly from existing codebase (Phase 2 sweep counter) and JDK documentation
- Pitfalls: HIGH for Pitfalls 1-5 (verified against JDK docs and existing code); MEDIUM for Pitfall 6 (requires runtime observation to confirm thread violation)

**Research date:** 2026-03-21
**Valid until:** Stable — JDK 21 and Paper 1.21.x APIs will not change; these patterns are long-established
