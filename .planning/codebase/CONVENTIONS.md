# Coding Conventions

**Analysis Date:** 2026-03-21

## Naming Patterns

**Packages:**
- Package root: `net.axther.serverCore`
- Subsystem packages organized by domain: `cosmetic`, `particle`, `pet`, `hologram`, `npc`, `quest`, `gui`, `timeline`, `reactive`, `api`, `config`, `command`
- Logical subdirectories: `data`, `listener`, `task`, `command`, `config`, `profiles`, `action`, `condition`

**Files:**
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

**Functions/Methods:**
- Method names: camelCase starting with verb (e.g., `applyCosmetic`, `removeCosmetics`, `registerProfile`, `getProfile`)
- Getter/Setter pattern: `get<Property>` / `set<Property>` (e.g., `getProfile`, `setStore`, `getBillboard`, `setBillboard`)
- Boolean getters: `is<Property>` or `has<Property>` (e.g., `isVisible`, `hasCosmetics`, `isDynamic`, `isCancelled`)
- Lifecycle methods: `tick()`, `destroy()`, `onEnable()`, `onDisable()`
- Private helper methods: short descriptive names (e.g., `tokenize()`, `parse()`, `evaluate()`)

**Variables:**
- Local variables: camelCase (e.g., `mobUuid`, `standUuid`, `reusableTarget`, `currentPage`)
- Final/immutable variables: same as regular (e.g., `this.id`, `this.manager`)
- Constants: UPPER_SNAKE_CASE (implied in Bukkit enums like `EntityType.PANDA`, `Material.PANDA_SPAWN_EGG`)
- Cached references: prefixed with `cached` (e.g., `cachedMob`, `cachedStand`)
- Loop counters: single lowercase letter (e.g., `i`, `page`)
- Reusable objects (to avoid allocation): prefixed with `reusable` (e.g., `reusableTarget`)

**Types/Classes:**
- Enum types: PascalCase (e.g., `PetState.FOLLOWING`, `QuestObjective.Type.FETCH`)
- Record types: PascalCase (e.g., `Token`)
- Inner static classes: PascalCase (e.g., `Parser`, `Literals`, `Arithmetic`)
- Interface implementations: class name matches interface name or has descriptive suffix

## Code Style

**Formatting:**
- Java 21 target language
- UTF-8 encoding enforced in `build.gradle`
- 4-space indentation (standard Java)
- Line width: no explicit limit, but files stay under ~800 lines (largest: `EmitterPattern.java` at 772 lines)
- No trailing whitespace

**Linting:**
- No `.eslintrc` or Checkstyle configuration found
- Code relies on IDE formatting (IntelliJ IDEA conventions implied)
- Paper API restrictions only: Paper API only, no NMS or reflection allowed

**Visibility Modifiers:**
- Public methods: used for API surface and Manager operations (e.g., `registerProfile()`, `applyCosmetic()`, `tick()`)
- Private methods: internal helpers and state management
- No protected methods in analyzed codebase
- Final classes: `ScriptParser` marked as `public final` to prevent extension
- Private constructors: `ScriptParser()` to prevent instantiation

## Import Organization

**Order:**
1. Package declaration
2. Blank line
3. Imports from other `net.axther` packages
4. Blank line (if needed)
5. Bukkit/Paper API imports (`org.bukkit.*`)
6. Third-party external APIs (ModelEngine, PlaceholderAPI, Vault)
7. Blank line
8. Jetbrains annotations (`org.jetbrains.annotations.*`)
9. Standard Java imports (`java.util.*`, `java.lang.*`)

**Example from `ServerCore.java`:**
```java
import net.axther.serverCore.command.ServerCoreCommand;
import net.axther.serverCore.config.ServerCoreConfig;
// ... many net.axther imports ...
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;
```

**Path Aliases:**
- No path aliases (`~`, `@`, etc.) in use — full package paths used

## Error Handling

**Patterns:**
- IllegalStateException for invalid state transitions
  - `throw new IllegalStateException("Location is required")` in builders when required field not set
  - `throw new IllegalStateException("ServerCoreAPI is not initialized. Is the ServerCore plugin enabled?")` for API access before plugin ready
- Null safety: explicit null checks before operations (e.g., `if (location == null) throw new IllegalStateException`)
- Silent failures for out-of-bounds access: `getProgress(-1)` returns 0 instead of throwing
- Optional returns: methods use `null` to indicate absence (e.g., `getProfile(EntityType)` returns `null` if not found)
- No try-catch blocks in main code (only compile-time safety via annotations)
- Cancellation pattern: Bukkit events have `isCancelled()` / `setCancelled(boolean)` pattern

**Defensive Copying:**
- Item stacks cloned before storage: `item.clone()` in `CosmeticInstance`
- Equipment locks applied to prevent interaction: all armor stand slots locked with `REMOVING_OR_CHANGING`

## Logging

**Framework:** Not detected in codebase (no SLF4J, Log4j, or java.util.logging imports found)

**Patterns:**
- Bukkit plugin likely uses Bukkit Logger via `getLogger()` inherited from JavaPlugin
- No debug logging in main code
- Error messages sent to players via `player.sendMessage()` for command feedback

## Comments

**When to Comment:**
- Complex grammar documentation (e.g., `ScriptParser.java` documents operator precedence)
- Algorithm explanations (e.g., ScriptParser comments on parsing approach)
- Non-obvious design decisions (e.g., "Cached entity references — avoids Bukkit.getEntity(UUID) global lookup every tick")
- Trade-offs and performance notes (e.g., location reuse to avoid allocation)
- Grammar specifications in multi-line comment blocks

**JavaDoc:**
- Used for public API methods and public classes
- `@param` tags document method parameters
- `@return` tags document return values
- `@throws` tags document exceptions (e.g., `@throws IllegalStateException if ServerCore has not been enabled yet`)
- Block comments explain complex logic (e.g., ScriptParser tokenization)
- Inline comments rare; self-documenting code preferred

**Example from `ScriptParserTest.java`:**
```java
/**
 * Tests for the particle scripting language parser and expression evaluator.
 * Covers correctness, edge cases, and operator precedence.
 */
class ScriptParserTest { ... }
```

**Example from `CosmeticInstance.java`:**
```java
/**
 * Updates the armor stand position to follow the mob.
 * @return true if the instance is still valid, false if it should be removed
 */
public boolean tick() { ... }
```

## Function Design

**Size:**
- Most methods: 5-20 lines
- Commands: 30-70 lines (due to required parameter parsing and branching)
- Managers: mixture of small accessors (1-3 lines) and larger lifecycle methods (20-40 lines)
- Largest method observed: `EmitterCommand.onCommand()` at ~150 lines (due to extensive subcommand handling)

**Parameters:**
- Few parameters preferred (0-3)
- Complex configurations passed as objects (e.g., builders, config objects)
- Builders accept method chaining with `return this` pattern
- No varargs in main code

**Return Values:**
- Boolean returns for success/failure (e.g., `applyCosmetic()` returns true if successful, false otherwise)
- Null for "not found" cases (e.g., `getProfile(EntityType)` returns null if no profile)
- Void for command operations and event handling
- Collections returned as immutable views or copies
- Optional-style early returns: `if (condition) return false;`

## Module Design

**Exports:**
- Managers are main API surface: `public class CosmeticManager`, `public class HologramManager`
- Builder pattern for complex object construction: `HologramBuilder`, `EmitterBuilder`
- Events published through Bukkit event API: all extend `Event`
- ServerCoreAPI provides singleton access to all managers

**Barrel Files:**
- No barrel files observed
- Each class in its own file; no package-level exports

## Performance Patterns

**Optimization Focus:**
- Weak references used for entity caching: `WeakReference<LivingEntity> cachedMob` avoids holding strong references
- Location reuse to avoid allocation in tick loops: `private final Location reusableTarget = new Location(null, 0, 0, 0)`
- Position change tracking to skip redundant teleports: `lastX`, `lastY`, `lastZ`, `lastYaw` prevent repeated updates
- EnumMap for EntityType keys: `new EnumMap<>(EntityType.class)` is more efficient than HashMap
- Early returns to avoid unnecessary computation
- tick() methods check entity validity before proceeding

---

*Convention analysis: 2026-03-21*
