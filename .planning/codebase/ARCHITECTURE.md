# Architecture

**Analysis Date:** 2026-03-21

## Pattern Overview

**Overall:** Plugin Manager with Multi-System Event-Driven Architecture

**Key Characteristics:**
- Nine independent YAML-configurable systems (cosmetics, emitters, pets, holograms, NPCs, quests, timelines, reactive cosmetics, GUIs)
- Central registry pattern for each system via dedicated Managers
- Event-driven lifecycle management with BukkitRunnable tick tasks (1-tick frequency)
- Two-tier profile system: Java profiles (code, high priority) + YAML config profiles (operators, low priority)
- Data persistence via YAML serialization with pending state tracking
- Soft dependencies with null-safety guards (ModelEngine, PlaceholderAPI, PacketEvents, Vault)

## Layers

**Plugin Entry Point:**
- Purpose: Initialize all systems conditionally, register event listeners, register commands, start tick tasks
- Location: `src/main/java/net/axther/serverCore/ServerCore.java`
- Contains: `onEnable()` and `onDisable()` lifecycle methods
- Depends on: All managers, configs, listeners, tasks
- Used by: Bukkit/Paper runtime

**Manager Layer:**
- Purpose: Central registry and state machine for each system (cosmetics, pets, holograms, NPCs, quests, emitters, timelines, reactive, menus)
- Location: `src/main/java/net/axther/serverCore/{system}/` (e.g., `CosmeticManager`, `PetManager`, `HologramManager`, `QuestManager`, etc.)
- Contains: Core business logic, lifecycle, registration, querying
- Depends on: Data models, stores, API events
- Used by: Commands, listeners, tick tasks, external plugins via API

**Command Layer:**
- Purpose: User-facing command execution and tab completion
- Location: `src/main/java/net/axther/serverCore/{system}/command/{System}Command.java`
- Contains: Subcommand routing, permission checks, player feedback
- Depends on: Managers, configs, listeners
- Used by: Bukkit command system

**Event Listener Layer:**
- Purpose: React to Bukkit events for lifecycle management, data persistence, interaction blocking
- Location: `src/main/java/net/axther/serverCore/{system}/listener/{System}Listener.java`
- Contains: Entity death handling, chunk load/unload, interaction prevention, session management
- Depends on: Managers, stores
- Used by: BukkitEventManager

**Tick Task Layer:**
- Purpose: Per-tick state updates (position tracking, animations, progression)
- Location: `src/main/java/net/axther/serverCore/{system}/task/{System}TickTask.java`
- Contains: `BukkitRunnable` subclasses calling `manager.tickAll()` every tick
- Depends on: Managers
- Used by: BukkitScheduler

**Configuration Layer:**
- Purpose: Load/parse/save YAML configs, registry population, profile creation
- Location: `src/main/java/net/axther/serverCore/{system}/config/{System}Config.java`
- Contains: YAML file I/O, type conversion, validation
- Depends on: Profile classes, managers
- Used by: Plugin entry point, managers

**Data Persistence Layer:**
- Purpose: Serialize/deserialize state to disk for cross-restart survival
- Location: `src/main/java/net/axther/serverCore/{system}/data/{System}Store.java`
- Contains: YAML read/write operations, pending state tracking
- Depends on: Managers
- Used by: Listeners, entry point (onDisable)

**Profile/Model Layer:**
- Purpose: Encapsulate system-specific behavior (mob head offsets, pet models, NPC appearance)
- Location: `src/main/java/net/axther/serverCore/{system}/profiles/`
- Contains: `MobCosmeticProfile`, `PetProfile`, custom Java implementations
- Depends on: Bukkit entities
- Used by: Managers, configs

**API Layer:**
- Purpose: Stable public interface for external plugins to integrate
- Location: `src/main/java/net/axther/serverCore/api/ServerCoreAPI.java`
- Contains: Static accessor to all managers, builder classes
- Depends on: All managers
- Used by: External plugins

**Hook Layer:**
- Purpose: Optional integrations with third-party plugins
- Location: `src/main/java/net/axther/serverCore/hook/`
- Contains: `PlaceholderHook`, `VaultHook`, `ModelEngineHook`
- Depends on: Third-party APIs
- Used by: Plugin entry point (only if dependent plugin is present)

## Data Flow

**System Initialization:**

1. `ServerCore.onEnable()` creates `ServerCoreConfig`
2. For each enabled system:
   - Create Manager (registry)
   - Load and register Java profiles (e.g., `PandaCosmeticProfile`)
   - Load and register config profiles (YAML, skipping if Java profile exists)
   - Set up data persistence (Store)
   - Register event listeners
   - Register commands
   - Start tick task (runs `manager.tickAll()` every 1 tick)
3. Initialize ServerCoreAPI singleton
4. Register optional hooks (if dependencies present)

**Cosmetic Application Flow (example):**

1. `/cosmetic apply` command invokes `CosmeticCommand.handleApply(player)`
2. Player raycasts to target mob
3. Command calls `CosmeticManager.applyCosmetic(mob, item)`
4. Manager fires `CosmeticApplyEvent` (cancellable)
5. If not cancelled: creates invisible marker ArmorStand with item on head
6. Stores `CosmeticInstance` in `activeCosmetics` map and `standIndex`
7. Calls `CosmeticStore.save()` to persist cosmetics to disk
8. Every tick: `CosmeticTickTask` calls `manager.tickAll()`
   - `CosmeticInstance.tick()` retrieves mob and stand, checks if alive
   - If alive: computes stand position (using `MobCosmeticProfile.computeStandLocation()`) and teleports stand
   - If dead: removes instance, returns false
9. On mob death: `CosmeticLifecycleListener` calls `manager.removeCosmetics(mobUuid)`
10. Data persisted via YAML serialization in cosmetic-data.yml

**State Management:**

- **Managers** maintain in-memory maps: `Map<UUID, List<Instance>>` for active entities
- **Stores** read/write YAML on load/save; track pending state for unloaded entities
- **Listeners** clean up on lifecycle events (death, chunk unload, world unload)
- **Instances** use cached weak references to entities; skip updates if no movement detected
- **WeakReferences** prevent memory leaks when entities are unloaded

## Key Abstractions

**Manager Pattern:**
- Purpose: Registry + lifecycle for system entities
- Examples: `CosmeticManager`, `PetManager`, `HologramManager`, `QuestManager`
- Pattern: HashMap<UUID, List<Instance>> or HashMap<String, Definition>

**Profile Pattern:**
- Purpose: Encapsulate behavior differences across entity types
- Examples: `MobCosmeticProfile` (with head offset geometry), `PetProfile` (with model data)
- Pattern: Base class with subclasses for specific types; Java profiles take priority over YAML config profiles

**Instance Pattern:**
- Purpose: Link owner (mob/player/NPC) to visual representation (armor stand/hologram/entity)
- Examples: `CosmeticInstance` (mob → armor stand), `PetInstance` (player → pet entity)
- Pattern: Stores both entity UUIDs, cached entity references, reusable position Location to minimize allocation

**Tick Task Pattern:**
- Purpose: Per-tick state synchronization
- Examples: `CosmeticTickTask`, `PetTickTask`, `HologramTickTask`
- Pattern: `BukkitRunnable` calling `manager.tickAll()` on 1-tick interval

**Config Loading Pattern:**
- Purpose: Parse YAML into managers
- Pattern: Java profiles registered first (priority), then config profiles (conditional)
- Example: `CosmeticConfig.loadAndRegister()` skips entity types already registered by Java

**Listener Pattern:**
- Purpose: React to Bukkit events for cleanup and persistence
- Examples: `CosmeticLifecycleListener`, `PetLifecycleListener`, `QuestListener`
- Pattern: `@EventHandler` on death/unload/interact events; calls manager cleanup

## Entry Points

**Plugin Main:**
- Location: `src/main/java/net/axther/serverCore/ServerCore.java`
- Triggers: Server startup
- Responsibilities: Load all systems, register listeners, start tasks, initialize API

**Commands:**
- Location: `src/main/java/net/axther/serverCore/{system}/command/{System}Command.java`
- Triggers: `/cosmetic`, `/pet`, `/hologram`, `/npc`, `/quest`, `/emitter`, `/timeline`, `/menu`, `/servercore`
- Responsibilities: User interaction, permission checks, manager invocation

**Events:**
- Bukkit events (death, chunk unload, interact): Trigger listeners
- Custom events (CosmeticApplyEvent, PetSummonEvent, etc.): Fired by managers, consumed by listeners/API
- Location: `src/main/java/net/axther/serverCore/api/event/`

**Tick Tasks:**
- Location: `src/main/java/net/axther/serverCore/{system}/task/{System}TickTask.java`
- Triggers: Every 1 server tick (20ms)
- Responsibilities: Call `manager.tickAll()` for per-frame state updates

## Error Handling

**Strategy:** Defensive null checks, unchecked exception logging, event cancellation

**Patterns:**
- Null-safe lookups: `if (manager.getProfile(type) == null)` — skip gracefully
- Dead entity checks: `if (entity.isDead() || entity == null)` — cleanup and continue
- Event cancellation: `CosmeticApplyEvent` can be cancelled by other plugins
- Weak references: Prevent memory leaks; null on garbage collection
- Exception logging: `plugin.getLogger().severe()` on I/O failures; continue without crashing

## Cross-Cutting Concerns

**Logging:**
- Framework: `org.bukkit.plugin.java.JavaPlugin.getLogger()`
- Patterns: Info on startup ("cosmetic system loaded"), warning on config errors, severe on I/O failures

**Validation:**
- YAML type checking: `mobSection.getDouble("head-y", 1.0)` with defaults
- Permission checks: `player.hasPermission("servercore.cosmetic.apply")` before command execution
- EntityType validation: `EntityType.valueOf(key.toUpperCase())` with exception handling

**Authentication:**
- Bukkit permission system: All commands check `player.hasPermission()`
- Permission nodes in plugin.yml: `servercore.cosmetic.*`, `servercore.pet.*`, etc.
- Optional op-only defaults for admin commands

---

*Architecture analysis: 2026-03-21*
