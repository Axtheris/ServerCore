# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
./gradlew build          # Compile and produce build/libs/ServerCore-1.0.0.jar
./gradlew runServer      # Launch a Paper 1.21 test server with the plugin loaded
```

Gradle 8.8, Java 21. The project uses the `xyz.jpenilla.run-paper` plugin for dev server management.

## Project Overview

ServerCore is a Paper 1.21 plugin (`net.axther.serverCore`) that provides nine server enhancement systems -- mob cosmetics, particle emitters, pets, holograms, NPCs with dialogue, quests, event timelines, reactive cosmetics, and chest GUIs -- all YAML-driven.

## Architecture

**Cosmetic profile system (two-tier):**
- **Java profiles** (`cosmetic/profiles/`) — subclass `MobCosmeticProfile` for mobs needing custom behavior. Registered first in `ServerCore.onEnable()` and take priority.
- **Config profiles** (`cosmetics.yml`) — YAML-driven profiles loaded by `CosmeticConfig`. Skipped if a Java profile already exists for that entity type. This lets server operators add new mob support without code changes.

**Core classes:**
- `MobCosmeticProfile` — defines head offset geometry (Y height, forward Z, side X) and computes armor stand placement using the mob's head yaw. Base class for both Java and config profiles.
- `CosmeticManager` — central registry and lifecycle manager. Tracks profiles by `EntityType`, active cosmetics by mob UUID, and a stand UUID index for fast lookup. Handles apply/remove/tick/destroy.
- `CosmeticInstance` — links a mob UUID to its armor stand UUID. `tick()` teleports the stand to follow the mob; returns false when either entity is dead/missing to trigger cleanup.
- `CosmeticTickTask` — `BukkitRunnable` that calls `CosmeticManager.tickAll()` every tick.
- `CosmeticLifecycleListener` — cleans up cosmetics on entity death, chunk unload, and blocks armor stand interaction.
- `CosmeticCommand` — `/cosmetic <apply|remove|clear|info>` using player raycast targeting. Requires `servercore.cosmetic` permission (op-only default).
- `QuestManager` -- registry for quest definitions, tracks per-player progress (active/completed), handles accept/complete logic with dialogue integration.

## Key Conventions

- Package root: `net.axther.serverCore`
- Paper API only (no NMS or reflection) — `compileOnly` dependency
- Armor stands are invisible, marker, no-gravity, non-persistent, equipment-locked
- Version token `${version}` in `plugin.yml` is expanded from `build.gradle` during `processResources`

<!-- GSD:project-start source:PROJECT.md -->
## Project

**ServerCore Debug & Verification Sweep**

A comprehensive debugging and verification sweep of the ServerCore Paper 1.21 plugin — a nine-system server enhancement platform (cosmetics, emitters, pets, holograms, NPCs, quests, timelines, reactive cosmetics, GUIs). The goal is to audit every system, fix known bugs, address tech debt, resolve security issues, and ensure all systems are robust and production-ready.

**Core Value:** Every system must work correctly under real server conditions — no silent failures, no entity leaks, no data corruption, no crashes.

### Constraints

- **Tech stack**: Paper API only — no NMS or reflection
- **Compatibility**: Must maintain backward compatibility with existing YAML configs and data files
- **Architecture**: Preserve existing Manager/Instance/Tick/Listener patterns
- **Dependencies**: Soft dependencies must remain optional — all systems must work without them
<!-- GSD:project-end -->

<!-- GSD:stack-start source:codebase/STACK.md -->
## Technology Stack

## Languages
- Java 21 - Plugin implementation for Paper 1.21 Minecraft server
- YAML - Configuration files for all systems (cosmetics, pets, quests, NPCs, etc.)
## Runtime
- Paper 1.21 (Minecraft server fork)
- JVM 21+
- Gradle 8.8
- Plugin: `xyz.jpenilla.run-paper` v2.3.1 (for dev server automation)
## Frameworks
- Paper API 1.21.11-R0.1-SNAPSHOT - Main server API for plugin development
- JUnit 5.11.4 (Jupiter) - Test framework
- Mockito 5.14.2 - Mocking framework for unit tests
## Key Dependencies
- `io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT` - Bukkit/Paper plugin API (no NMS or reflection)
- `com.github.retrooper:packetevents-spigot:2.7.0` - Packet interception for NPC rendering and interactions
- `com.ticxo.modelengine:ModelEngine:R4.0.7` - 3D model support for pets (optional, detected at runtime)
- `me.clip:placeholderapi:2.11.6` - Placeholder integration for text variables (optional)
- `com.github.MilkBowl:VaultAPI:1.7.1` - Economy and permission API for quest rewards (optional)
## Configuration
- No environment variables required. All configuration via YAML files.
- `.env` files: None
- Config files location: Plugin data folder (`plugins/ServerCore/`)
- `build.gradle` - Build configuration
- `settings.gradle` - Root project definition
- `gradle.properties` - Empty (using defaults)
- `plugin.yml` - Minecraft plugin metadata (version templated from build.gradle)
- `config.yml` - Central configuration (system toggles, view distances)
- `cosmetics.yml` - Mob cosmetic profiles
- `quests/` - Quest definitions (YAML files)
- `pets/` - Pet profile definitions (separate YAML per pet type)
- `menus/` - Custom GUI menu definitions (YAML files)
## Platform Requirements
- Java 21 JDK
- Gradle 8.8
- IDE with Gradle support (IntelliJ IDEA, Eclipse, VS Code with extensions)
- Paper 1.21 server running on JVM 21+
- Optional plugins for full feature support:
## Encoding & Locale
- Source files: UTF-8 (enforced in `build.gradle` via `options.encoding`)
- Configuration files: UTF-8
- JVM default locale: Server-determined
## Maven Repositories
- `mavenCentral()` - Standard Java packages
- `https://repo.papermc.io/repository/maven-public/` - Paper API
- `https://mvn.lumine.io/repository/maven-public/` - ModelEngine
- `https://repo.extendedclip.com/content/repositories/placeholderapi/` - PlaceholderAPI
- `https://repo.codemc.io/repository/maven-releases/` - Community plugins
- `https://jitpack.io` - GitHub-based dependency builds
<!-- GSD:stack-end -->

<!-- GSD:conventions-start source:CONVENTIONS.md -->
## Conventions

## Naming Patterns
- Package root: `net.axther.serverCore`
- Subsystem packages organized by domain: `cosmetic`, `particle`, `pet`, `hologram`, `npc`, `quest`, `gui`, `timeline`, `reactive`, `api`, `config`, `command`
- Logical subdirectories: `data`, `listener`, `task`, `command`, `config`, `profiles`, `action`, `condition`
- Class names: PascalCase (e.g., `CosmeticManager`, `HologramBuilder`, `ScriptParser`)
- File name matches class name exactly
- Manager classes: suffix with `Manager` (e.g., `CosmeticManager`, `HologramManager`, `PetManager`)
- Config loaders: suffix with `Config` (e.g., `CosmeticConfig`, `HologramConfig`, `MenuConfig`)
- Event classes: in `api/event/` package, suffix with `Event` (e.g., `CosmeticApplyEvent`, `HologramClickEvent`)
- Builder classes: suffix with `Builder` (e.g., `HologramBuilder`, `EmitterBuilder`)
- Task/Runnable classes: suffix with `Task` (e.g., `CosmeticTickTask`, `HologramTickTask`)
- Data store classes: suffix with `Store` (e.g., `CosmeticStore`, `QuestStore`, `PetStore`)
- Instance/runtime classes: suffix with `Instance` (e.g., `CosmeticInstance`, `PetInstance`, `TimelineInstance`)
- Listener classes: suffix with `Listener` (e.g., `CosmeticLifecycleListener`, `PetLifecycleListener`)
- Command classes: suffix with `Command` (e.g., `CosmeticCommand`, `PetCommand`, `HologramCommand`)
- Profile classes: suffix with `Profile` (e.g., `MobCosmeticProfile`, `PetProfile`)
- Method names: camelCase starting with verb (e.g., `applyCosmetic`, `removeCosmetics`, `registerProfile`, `getProfile`)
- Getter/Setter pattern: `get<Property>` / `set<Property>` (e.g., `getProfile`, `setStore`, `getBillboard`, `setBillboard`)
- Boolean getters: `is<Property>` or `has<Property>` (e.g., `isVisible`, `hasCosmetics`, `isDynamic`, `isCancelled`)
- Lifecycle methods: `tick()`, `destroy()`, `onEnable()`, `onDisable()`
- Private helper methods: short descriptive names (e.g., `tokenize()`, `parse()`, `evaluate()`)
- Local variables: camelCase (e.g., `mobUuid`, `standUuid`, `reusableTarget`, `currentPage`)
- Final/immutable variables: same as regular (e.g., `this.id`, `this.manager`)
- Constants: UPPER_SNAKE_CASE (implied in Bukkit enums like `EntityType.PANDA`, `Material.PANDA_SPAWN_EGG`)
- Cached references: prefixed with `cached` (e.g., `cachedMob`, `cachedStand`)
- Loop counters: single lowercase letter (e.g., `i`, `page`)
- Reusable objects (to avoid allocation): prefixed with `reusable` (e.g., `reusableTarget`)
- Enum types: PascalCase (e.g., `PetState.FOLLOWING`, `QuestObjective.Type.FETCH`)
- Record types: PascalCase (e.g., `Token`)
- Inner static classes: PascalCase (e.g., `Parser`, `Literals`, `Arithmetic`)
- Interface implementations: class name matches interface name or has descriptive suffix
## Code Style
- Java 21 target language
- UTF-8 encoding enforced in `build.gradle`
- 4-space indentation (standard Java)
- Line width: no explicit limit, but files stay under ~800 lines (largest: `EmitterPattern.java` at 772 lines)
- No trailing whitespace
- No `.eslintrc` or Checkstyle configuration found
- Code relies on IDE formatting (IntelliJ IDEA conventions implied)
- Paper API restrictions only: Paper API only, no NMS or reflection allowed
- Public methods: used for API surface and Manager operations (e.g., `registerProfile()`, `applyCosmetic()`, `tick()`)
- Private methods: internal helpers and state management
- No protected methods in analyzed codebase
- Final classes: `ScriptParser` marked as `public final` to prevent extension
- Private constructors: `ScriptParser()` to prevent instantiation
## Import Organization
- No path aliases (`~`, `@`, etc.) in use — full package paths used
## Error Handling
- IllegalStateException for invalid state transitions
- Null safety: explicit null checks before operations (e.g., `if (location == null) throw new IllegalStateException`)
- Silent failures for out-of-bounds access: `getProgress(-1)` returns 0 instead of throwing
- Optional returns: methods use `null` to indicate absence (e.g., `getProfile(EntityType)` returns `null` if not found)
- No try-catch blocks in main code (only compile-time safety via annotations)
- Cancellation pattern: Bukkit events have `isCancelled()` / `setCancelled(boolean)` pattern
- Item stacks cloned before storage: `item.clone()` in `CosmeticInstance`
- Equipment locks applied to prevent interaction: all armor stand slots locked with `REMOVING_OR_CHANGING`
## Logging
- Bukkit plugin likely uses Bukkit Logger via `getLogger()` inherited from JavaPlugin
- No debug logging in main code
- Error messages sent to players via `player.sendMessage()` for command feedback
## Comments
- Complex grammar documentation (e.g., `ScriptParser.java` documents operator precedence)
- Algorithm explanations (e.g., ScriptParser comments on parsing approach)
- Non-obvious design decisions (e.g., "Cached entity references — avoids Bukkit.getEntity(UUID) global lookup every tick")
- Trade-offs and performance notes (e.g., location reuse to avoid allocation)
- Grammar specifications in multi-line comment blocks
- Used for public API methods and public classes
- `@param` tags document method parameters
- `@return` tags document return values
- `@throws` tags document exceptions (e.g., `@throws IllegalStateException if ServerCore has not been enabled yet`)
- Block comments explain complex logic (e.g., ScriptParser tokenization)
- Inline comments rare; self-documenting code preferred
## Function Design
- Most methods: 5-20 lines
- Commands: 30-70 lines (due to required parameter parsing and branching)
- Managers: mixture of small accessors (1-3 lines) and larger lifecycle methods (20-40 lines)
- Largest method observed: `EmitterCommand.onCommand()` at ~150 lines (due to extensive subcommand handling)
- Few parameters preferred (0-3)
- Complex configurations passed as objects (e.g., builders, config objects)
- Builders accept method chaining with `return this` pattern
- No varargs in main code
- Boolean returns for success/failure (e.g., `applyCosmetic()` returns true if successful, false otherwise)
- Null for "not found" cases (e.g., `getProfile(EntityType)` returns null if no profile)
- Void for command operations and event handling
- Collections returned as immutable views or copies
- Optional-style early returns: `if (condition) return false;`
## Module Design
- Managers are main API surface: `public class CosmeticManager`, `public class HologramManager`
- Builder pattern for complex object construction: `HologramBuilder`, `EmitterBuilder`
- Events published through Bukkit event API: all extend `Event`
- ServerCoreAPI provides singleton access to all managers
- No barrel files observed
- Each class in its own file; no package-level exports
## Performance Patterns
- Weak references used for entity caching: `WeakReference<LivingEntity> cachedMob` avoids holding strong references
- Location reuse to avoid allocation in tick loops: `private final Location reusableTarget = new Location(null, 0, 0, 0)`
- Position change tracking to skip redundant teleports: `lastX`, `lastY`, `lastZ`, `lastYaw` prevent repeated updates
- EnumMap for EntityType keys: `new EnumMap<>(EntityType.class)` is more efficient than HashMap
- Early returns to avoid unnecessary computation
- tick() methods check entity validity before proceeding
<!-- GSD:conventions-end -->

<!-- GSD:architecture-start source:ARCHITECTURE.md -->
## Architecture

## Pattern Overview
- Nine independent YAML-configurable systems (cosmetics, emitters, pets, holograms, NPCs, quests, timelines, reactive cosmetics, GUIs)
- Central registry pattern for each system via dedicated Managers
- Event-driven lifecycle management with BukkitRunnable tick tasks (1-tick frequency)
- Two-tier profile system: Java profiles (code, high priority) + YAML config profiles (operators, low priority)
- Data persistence via YAML serialization with pending state tracking
- Soft dependencies with null-safety guards (ModelEngine, PlaceholderAPI, PacketEvents, Vault)
## Layers
- Purpose: Initialize all systems conditionally, register event listeners, register commands, start tick tasks
- Location: `src/main/java/net/axther/serverCore/ServerCore.java`
- Contains: `onEnable()` and `onDisable()` lifecycle methods
- Depends on: All managers, configs, listeners, tasks
- Used by: Bukkit/Paper runtime
- Purpose: Central registry and state machine for each system (cosmetics, pets, holograms, NPCs, quests, emitters, timelines, reactive, menus)
- Location: `src/main/java/net/axther/serverCore/{system}/` (e.g., `CosmeticManager`, `PetManager`, `HologramManager`, `QuestManager`, etc.)
- Contains: Core business logic, lifecycle, registration, querying
- Depends on: Data models, stores, API events
- Used by: Commands, listeners, tick tasks, external plugins via API
- Purpose: User-facing command execution and tab completion
- Location: `src/main/java/net/axther/serverCore/{system}/command/{System}Command.java`
- Contains: Subcommand routing, permission checks, player feedback
- Depends on: Managers, configs, listeners
- Used by: Bukkit command system
- Purpose: React to Bukkit events for lifecycle management, data persistence, interaction blocking
- Location: `src/main/java/net/axther/serverCore/{system}/listener/{System}Listener.java`
- Contains: Entity death handling, chunk load/unload, interaction prevention, session management
- Depends on: Managers, stores
- Used by: BukkitEventManager
- Purpose: Per-tick state updates (position tracking, animations, progression)
- Location: `src/main/java/net/axther/serverCore/{system}/task/{System}TickTask.java`
- Contains: `BukkitRunnable` subclasses calling `manager.tickAll()` every tick
- Depends on: Managers
- Used by: BukkitScheduler
- Purpose: Load/parse/save YAML configs, registry population, profile creation
- Location: `src/main/java/net/axther/serverCore/{system}/config/{System}Config.java`
- Contains: YAML file I/O, type conversion, validation
- Depends on: Profile classes, managers
- Used by: Plugin entry point, managers
- Purpose: Serialize/deserialize state to disk for cross-restart survival
- Location: `src/main/java/net/axther/serverCore/{system}/data/{System}Store.java`
- Contains: YAML read/write operations, pending state tracking
- Depends on: Managers
- Used by: Listeners, entry point (onDisable)
- Purpose: Encapsulate system-specific behavior (mob head offsets, pet models, NPC appearance)
- Location: `src/main/java/net/axther/serverCore/{system}/profiles/`
- Contains: `MobCosmeticProfile`, `PetProfile`, custom Java implementations
- Depends on: Bukkit entities
- Used by: Managers, configs
- Purpose: Stable public interface for external plugins to integrate
- Location: `src/main/java/net/axther/serverCore/api/ServerCoreAPI.java`
- Contains: Static accessor to all managers, builder classes
- Depends on: All managers
- Used by: External plugins
- Purpose: Optional integrations with third-party plugins
- Location: `src/main/java/net/axther/serverCore/hook/`
- Contains: `PlaceholderHook`, `VaultHook`, `ModelEngineHook`
- Depends on: Third-party APIs
- Used by: Plugin entry point (only if dependent plugin is present)
## Data Flow
- **Managers** maintain in-memory maps: `Map<UUID, List<Instance>>` for active entities
- **Stores** read/write YAML on load/save; track pending state for unloaded entities
- **Listeners** clean up on lifecycle events (death, chunk unload, world unload)
- **Instances** use cached weak references to entities; skip updates if no movement detected
- **WeakReferences** prevent memory leaks when entities are unloaded
## Key Abstractions
- Purpose: Registry + lifecycle for system entities
- Examples: `CosmeticManager`, `PetManager`, `HologramManager`, `QuestManager`
- Pattern: HashMap<UUID, List<Instance>> or HashMap<String, Definition>
- Purpose: Encapsulate behavior differences across entity types
- Examples: `MobCosmeticProfile` (with head offset geometry), `PetProfile` (with model data)
- Pattern: Base class with subclasses for specific types; Java profiles take priority over YAML config profiles
- Purpose: Link owner (mob/player/NPC) to visual representation (armor stand/hologram/entity)
- Examples: `CosmeticInstance` (mob → armor stand), `PetInstance` (player → pet entity)
- Pattern: Stores both entity UUIDs, cached entity references, reusable position Location to minimize allocation
- Purpose: Per-tick state synchronization
- Examples: `CosmeticTickTask`, `PetTickTask`, `HologramTickTask`
- Pattern: `BukkitRunnable` calling `manager.tickAll()` on 1-tick interval
- Purpose: Parse YAML into managers
- Pattern: Java profiles registered first (priority), then config profiles (conditional)
- Example: `CosmeticConfig.loadAndRegister()` skips entity types already registered by Java
- Purpose: React to Bukkit events for cleanup and persistence
- Examples: `CosmeticLifecycleListener`, `PetLifecycleListener`, `QuestListener`
- Pattern: `@EventHandler` on death/unload/interact events; calls manager cleanup
## Entry Points
- Location: `src/main/java/net/axther/serverCore/ServerCore.java`
- Triggers: Server startup
- Responsibilities: Load all systems, register listeners, start tasks, initialize API
- Location: `src/main/java/net/axther/serverCore/{system}/command/{System}Command.java`
- Triggers: `/cosmetic`, `/pet`, `/hologram`, `/npc`, `/quest`, `/emitter`, `/timeline`, `/menu`, `/servercore`
- Responsibilities: User interaction, permission checks, manager invocation
- Bukkit events (death, chunk unload, interact): Trigger listeners
- Custom events (CosmeticApplyEvent, PetSummonEvent, etc.): Fired by managers, consumed by listeners/API
- Location: `src/main/java/net/axther/serverCore/api/event/`
- Location: `src/main/java/net/axther/serverCore/{system}/task/{System}TickTask.java`
- Triggers: Every 1 server tick (20ms)
- Responsibilities: Call `manager.tickAll()` for per-frame state updates
## Error Handling
- Null-safe lookups: `if (manager.getProfile(type) == null)` — skip gracefully
- Dead entity checks: `if (entity.isDead() || entity == null)` — cleanup and continue
- Event cancellation: `CosmeticApplyEvent` can be cancelled by other plugins
- Weak references: Prevent memory leaks; null on garbage collection
- Exception logging: `plugin.getLogger().severe()` on I/O failures; continue without crashing
## Cross-Cutting Concerns
- Framework: `org.bukkit.plugin.java.JavaPlugin.getLogger()`
- Patterns: Info on startup ("cosmetic system loaded"), warning on config errors, severe on I/O failures
- YAML type checking: `mobSection.getDouble("head-y", 1.0)` with defaults
- Permission checks: `player.hasPermission("servercore.cosmetic.apply")` before command execution
- EntityType validation: `EntityType.valueOf(key.toUpperCase())` with exception handling
- Bukkit permission system: All commands check `player.hasPermission()`
- Permission nodes in plugin.yml: `servercore.cosmetic.*`, `servercore.pet.*`, etc.
- Optional op-only defaults for admin commands
<!-- GSD:architecture-end -->

<!-- GSD:workflow-start source:GSD defaults -->
## GSD Workflow Enforcement

Before using Edit, Write, or other file-changing tools, start work through a GSD command so planning artifacts and execution context stay in sync.

Use these entry points:
- `/gsd:quick` for small fixes, doc updates, and ad-hoc tasks
- `/gsd:debug` for investigation and bug fixing
- `/gsd:execute-phase` for planned phase work

Do not make direct repo edits outside a GSD workflow unless the user explicitly asks to bypass it.
<!-- GSD:workflow-end -->

<!-- GSD:profile-start -->
## Developer Profile

> Profile not yet configured. Run `/gsd:profile-user` to generate your developer profile.
> This section is managed by `generate-claude-profile` -- do not edit manually.
<!-- GSD:profile-end -->
