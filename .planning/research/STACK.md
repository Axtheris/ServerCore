# Stack Research

**Domain:** Paper 1.21 plugin debugging, testing, and hardening pass
**Researched:** 2026-03-21
**Confidence:** HIGH (core tooling), MEDIUM (version numbers where Maven Central not directly queried)

---

## Context

This is a fix/harden milestone on an existing plugin. The base stack (Java 21, Gradle 8.8, Paper API 1.21.11) is locked. This research covers only the **testing, static analysis, profiling, and debugging toolchain** needed to execute the quality pass.

The existing codebase already declares:
- `junit-jupiter:5.11.4` (testImplementation)
- `mockito-core:5.14.2` (testImplementation)
- `junit-platform-launcher` (testRuntimeOnly)

These are stale by one minor cycle but functional. Recommendations below specify whether to update or keep.

---

## Recommended Stack

### Core Technologies

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| JUnit Jupiter | 5.14.3 | Test runner, assertions, parameterized tests | Current stable JUnit 5 release. JUnit 6.0.3 shipped Feb 2026 but is a breaking rename — stay on 5.x until ecosystem catches up. Upgrade from 5.11.4 to 5.14.3 for `@ParameterizedTest` improvements in 5.11+. |
| Mockito | 5.23.0 | Mock Bukkit interfaces, manager dependencies | Standard Java mock framework. Already in the project. Upgrade from 5.14.2 for Java 21 virtual thread safety fixes. Paper API interfaces are abstract — Mockito stubs them without a running server. |
| MockBukkit | 4.108.0 | Full mock Paper server environment | The only framework that mocks the scheduler, event bus, entity registry, and plugin lifecycle without a real JVM. Required for testing manager tick logic, lifecycle listeners, and event-driven systems. Cannot be replaced by raw Mockito for tests that need `Bukkit.getServer()` or event firing. |
| spark (bundled) | Paper 1.21+ built-in | CPU profiling, memory heap dumps | Paper 1.21+ bundles spark natively. No install needed. Use `/spark profiler` for tick regression, `/spark heapdump` to take `.hprof` snapshots for memory leak analysis. Primary in-server diagnostic tool. |

### Supporting Libraries

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| AssertJ | 3.27.7 | Fluent assertion DSL | Use instead of raw `assertEquals` when asserting on collections, optionals, or map contents. Produces clearer failure messages. Add for any test class asserting on manager state (e.g., `assertThat(manager.getActiveInstances()).hasSize(3)`). |
| SpotBugs Gradle plugin | 6.4.8 | Static bytecode analysis — finds null dereferences, synchronization bugs, resource leaks | Add to build.gradle to catch the documented CONCERNS: unguarded null returns, unsynchronized HashMap access, unclosed resources. Runs on compiled bytecode so catches issues Checkstyle misses. |
| Eclipse Memory Analyzer (MAT) | 1.16.0 | Offline heap dump analysis | Use after `/spark heapdump` to analyze `.hprof` files. Provides dominator tree and retained-heap queries. Critical for diagnosing the documented unbounded cooldown map growth and stand UUID index leaks. Standalone desktop tool — not a Gradle dependency. |

### Development Tools

| Tool | Purpose | Notes |
|------|---------|-------|
| IntelliJ IDEA remote debugger | Live breakpoints in running Paper server | Add `-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005` to `runServer` task via `jvmArgs` in build.gradle. Then create an IntelliJ "Remote JVM Debug" run config pointing to port 5005. Lets you set breakpoints directly in manager tick methods without modifying code. |
| Gradle `-Xlint` compiler flags | Surface unchecked casts and deprecation warnings at compile time | Add `options.compilerArgs << "-Xlint:unchecked" << "-Xlint:deprecation"` to the existing `tasks.withType(JavaCompile)` block. The 636 null/instanceof patterns in the codebase likely hide unchecked cast suppressions worth surfacing. |
| VisualVM | Live JVM monitoring, heap histogram, thread analysis | Standalone download from https://visualvm.github.io/. Connect to the Paper JVM PID while running `runServer`. Use heap histogram to track HashMap growth over time without taking a full dump. |

---

## Installation

```groovy
// build.gradle additions for the debug/harden milestone

plugins {
    // existing
    id 'java'
    id("xyz.jpenilla.run-paper") version "2.3.1"
    // add:
    id("com.github.spotbugs") version "6.4.8"
}

repositories {
    // existing repos unchanged
    mavenCentral()
    // add for MockBukkit:
    maven { url = "https://repo.papermc.io/repository/maven-public/" } // already present
}

dependencies {
    // existing — update versions:
    testImplementation("org.junit.jupiter:junit-jupiter:5.14.3")       // was 5.11.4
    testImplementation("org.mockito:mockito-core:5.23.0")              // was 5.14.2

    // add:
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v1.21:4.108.0")
    testImplementation("org.assertj:assertj-core:3.27.7")
}

tasks {
    runServer {
        minecraftVersion("1.21")
        // add for remote debugging:
        jvmArgs("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005")
    }
}

// Add to existing tasks.withType(JavaCompile) block:
tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
    options.compilerArgs << "-Xlint:unchecked" << "-Xlint:deprecation"
    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible()) {
        options.release.set(targetJavaVersion)
    }
}

// SpotBugs configuration:
spotbugs {
    toolVersion = "4.8.6"
    effort = "max"
    reportLevel = "medium"
    ignoreFailures = false
}

spotbugsMain {
    reports {
        html {
            required = true
            outputLocation = file("$buildDir/reports/spotbugs/main.html")
        }
    }
}
```

---

## Alternatives Considered

| Recommended | Alternative | When to Use Alternative |
|-------------|-------------|-------------------------|
| MockBukkit 4.108.0 | Raw Mockito stubs for Bukkit APIs | Raw Mockito is fine for pure Java logic that doesn't call `Bukkit.*` or fire events. Already the pattern for QuestDataModelTest and HologramDataModelTest — keep those as-is. Only reach for MockBukkit when you need scheduler control or event bus. |
| SpotBugs | PMD | PMD focuses on source-level style issues (unused vars, empty catch blocks). SpotBugs focuses on bytecode-level bugs (NPE paths, race conditions, resource leaks). For a harden pass targeting the documented CONCERNS, SpotBugs is more valuable. Both can coexist but SpotBugs is the priority. |
| SpotBugs | Checkstyle | Checkstyle enforces formatting conventions. Out of scope for a fix/harden pass — don't add it now, as it would generate hundreds of style warnings that dilute focus from real bugs. |
| Eclipse MAT (offline) | VisualVM heap browser | VisualVM gives live histogram useful for trending. MAT gives deep retained-heap analysis for identifying a specific leak path. Use VisualVM for monitoring, MAT for diagnosis once a leak is confirmed. |
| IntelliJ remote debug | Log-based debugging | Logs are coarser. For issues like "what is the manager's state when tick fires?" remote debugging with breakpoints is far faster. The `runServer` task already runs a Paper server locally, making remote attach trivial. |

---

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| JUnit 6 (6.0.3) | Released Feb 2026 — MockBukkit 4.x, Mockito 5.x, and most test utilities have not validated compatibility. API renamed from `junit-jupiter` to `junit-framework`. Breakage risk on a fix-focused milestone. | Stay on JUnit 5.14.3; migrate to JUnit 6 in a future dedicated upgrade milestone. |
| Timings (Paper built-in, pre-1.21) | Deprecated and turned off by default in Paper 1.21 in favor of spark. Reports are hard to read. | spark (bundled, no install required on Paper 1.21). |
| `com.github.seeseemelk:MockBukkit-v1.21` (old coordinates) | Old Maven group ID — the project migrated to `org.mockbukkit.mockbukkit:mockbukkit-v1.21`. Old artifact may lag on updates. | `org.mockbukkit.mockbukkit:mockbukkit-v1.21:4.108.0` |
| Async saves with third-party YAML libraries (e.g., SnakeYAML direct) | The codebase already uses Paper's built-in YAML handling. Introducing a second YAML library for async writes adds complexity. | Use `Bukkit.getScheduler().runTaskAsynchronously()` with the existing YamlConfiguration for debounced saves — no new library needed. |
| JProfiler / YourKit | Commercial profilers with per-developer license costs. spark is free, bundled, and purpose-built for Minecraft. | spark (bundled in Paper 1.21). |

---

## Stack Patterns by Variant

**For tests with no Bukkit API calls (data model, logic classes):**
- Use plain JUnit 5 + raw Mockito
- Examples: `QuestDataModelTest`, `HologramDataModelTest` — already correct, keep this pattern
- No MockBukkit needed; it adds startup overhead that slows the test suite

**For tests that need event firing, scheduler ticks, or `Bukkit.getServer()`:**
- Use MockBukkit + JUnit 5
- Example candidates: `CosmeticManager` tick lifecycle, `HologramInteractListener` cooldown behavior, `QuestManager` sequential objective progression
- MockBukkit lets you advance the scheduler by N ticks, fire events, and verify side effects

**For tests exercising concurrent access patterns:**
- Use JUnit 5 + Mockito + `ExecutorService` to fire concurrent calls
- No framework can fully simulate Bukkit's main-thread/async split — verify ConcurrentHashMap behavior by hammering collections from multiple threads in tests

**For in-server profiling and memory leak diagnosis:**
- spark: `/spark profiler start` → reproduce issue → `/spark profiler stop` (uploads to spark.lucko.me)
- spark heap: `/spark heapdump` → open `.hprof` in Eclipse MAT → run "Leak Suspects" report

**For static analysis during CI / build:**
- SpotBugs: `./gradlew spotbugsMain` — treat medium+ severity findings as build failures during the harden pass

---

## Version Compatibility

| Package | Compatible With | Notes |
|---------|-----------------|-------|
| mockbukkit-v1.21:4.108.0 | Paper API 1.21.x, JUnit 5.x, Java 21 | Artifact suffix (`-v1.21`) is tied to MC version, not semver. 4.108.0 is the current latest on Maven Central as of research date. |
| mockito-core:5.23.0 | JUnit 5.14.x, Java 21 | Mockito 5.x requires Java 11+; fully compatible with Java 21 including virtual threads. |
| spotbugs plugin:6.4.8 | Gradle 8.x, Java 21 (analysis target) | SpotBugs requires Java 11+ to run, analyzes any bytecode target. Gradle 8.8 is supported. |
| assertj-core:3.27.7 | JUnit 5.x, Java 21 | No transitive conflicts. Pull in only for test scope. |

---

## Sources

- https://github.com/MockBukkit/MockBukkit — MockBukkit v1.21 branch, README
- https://central.sonatype.com/artifact/org.mockbukkit.mockbukkit/mockbukkit-v1.21/4.108.0 — version 4.108.0 confirmed on Maven Central (HIGH confidence)
- https://plugins.gradle.org/plugin/com.github.spotbugs — SpotBugs Gradle plugin 6.4.8, released Dec 11, 2025 (HIGH confidence)
- https://spark.lucko.me/ — spark profiler; Paper 1.21+ bundles it natively (HIGH confidence)
- https://docs.papermc.io/paper/profiling/ — Paper official profiling docs confirming spark bundling in 1.21
- https://docs.papermc.io/paper/dev/debugging/ — Paper official debugging guide, JDWP remote attach pattern
- https://mvnrepository.com/artifact/org.assertj/assertj-core — AssertJ 3.27.7 latest stable (MEDIUM confidence — not directly verified on Maven Central)
- https://github.com/mockito/mockito/releases — Mockito 5.23.0 latest (MEDIUM confidence — sourced from search result)
- https://junit.org/junit5/docs/current/release-notes/ — JUnit 5.14.3 current stable (HIGH confidence)
- https://eclipse.dev/mat/ — Eclipse MAT 1.16.0 released Dec 20, 2024 (HIGH confidence)

---

*Stack research for: Paper 1.21 plugin debug/harden pass (ServerCore)*
*Researched: 2026-03-21*
