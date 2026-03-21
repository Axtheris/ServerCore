# Codebase Structure

**Analysis Date:** 2026-03-21

## Directory Layout

```
ServerCore/
├── src/main/
│   ├── java/net/axther/serverCore/
│   │   ├── ServerCore.java                    # Plugin entry point
│   │   ├── api/                               # Public API and events
│   │   ├── command/                           # Admin commands
│   │   ├── config/                            # Central config
│   │   ├── cosmetic/                          # Mob cosmetics system
│   │   ├── gui/                               # Menu framework
│   │   ├── hologram/                          # Text display holograms
│   │   ├── hook/                              # Third-party integrations
│   │   ├── npc/                               # NPCs with dialogue
│   │   ├── particle/                          # Particle emitters
│   │   ├── pet/                               # Player pets system
│   │   ├── quest/                             # Quest system
│   │   ├── reactive/                          # Context-aware cosmetics
│   │   └── timeline/                          # Event sequencer
│   └── resources/
│       ├── plugin.yml                         # Plugin metadata
│       ├── config.yml                         # System toggles
│       ├── cosmetics.yml                      # Cosmetic profiles
│       ├── pets/                              # Pet definitions
│       └── quests/                            # Quest definitions
├── build.gradle                               # Gradle build config
└── CLAUDE.md                                  # Claude Code instructions
```

## Directory Purposes

**Root:**
- `src/main/java/`: Java source code
- `src/main/resources/`: Plugin resources (YAMLs, configs)
- `build.gradle`: Gradle build configuration (Java 21, Paper 1.21)

**`api/`:**
- Purpose: Public API for external plugins; custom events fired by managers
- Contains: `ServerCoreAPI.java` (static manager accessor), builder classes, event classes
- Key files:
  - `ServerCoreAPI.java`: Static getter for all managers
  - `builder/`: Fluent builders for emitters, holograms
  - `event/`: Custom Bukkit events (CosmeticApplyEvent, PetSummonEvent, HologramClickEvent, etc.)

**`cosmetic/`:**
- Purpose: Mob cosmetics system (armor stand helmets)
- Contains: Manager, profiles (Java + YAML), command, listeners, data persistence
- Structure:
  - `CosmeticManager.java`: Registry for profiles, lifecycle for cosmetics
  - `MobCosmeticProfile.java`: Base profile with head offset geometry
  - `profiles/`: Java subclasses (e.g., `PandaCosmeticProfile.java`)
  - `CosmeticInstance.java`: Links mob UUID to armor stand UUID; handles ticking
  - `command/CosmeticCommand.java`: `/cosmetic apply|remove|clear|info|calibrate|gui`
  - `config/CosmeticConfig.java`: Loads cosmetics.yml; registers profiles
  - `data/CosmeticStore.java`: Saves/loads cosmetic-data.yml
  - `listener/CosmeticLifecycleListener.java`: Cleans up on entity death/unload
  - `task/CosmeticTickTask.java`: Ticks all active cosmetics every server tick
  - `calibrate/`: Calibration tools for head offset tuning

**`pet/`:**
- Purpose: Player pet system
- Contains: Manager, profiles, command, listeners, data, model integration
- Structure:
  - `PetManager.java`: Registry for pets (pet definitions); tracks owned pets per player
  - `PetInstance.java`: Links player to pet entity
  - `PetProfile.java`: Base pet behavior definition
  - `profiles/`: Java subclasses (e.g., `RatPetProfile.java`)
  - `command/PetCommand.java`: `/pet summon|dismiss|sit|follow|feed|list|give|gui`
  - `config/PetConfig.java`: Loads pet definitions from YAML
  - `data/PetStore.java`: Saves/loads pet-data.yml
  - `listener/PetLifecycleListener.java`: Cleans up when pets/players die
  - `listener/PetItemListener.java`: Handles pet items (food, summon items)
  - `task/PetTickTask.java`: Ticks all active pets
  - `model/ModelEngineHook.java`: Optional ModelEngine integration
  - `util/`: Pet utility functions

**`hologram/`:**
- Purpose: Text display holograms with animations and interactions
- Contains: Manager, command, listeners, config, action/condition system
- Structure:
  - `HologramManager.java`: Registry for holograms
  - `Hologram.java`: Represents a hologram (text lines, position, visibility)
  - `command/HologramCommand.java`: `/hologram create|remove|addline|edit|setanimation|gui`
  - `config/HologramConfig.java`: Loads hologram definitions from YAML
  - `listener/HologramLifecycleListener.java`: Chunk/world unload cleanup
  - `listener/HologramInteractListener.java`: Click event handling
  - `action/`: Hologram actions (send message, run command, execute dialogue)
  - `condition/`: Visibility conditions (time-based, player-level, etc.)
  - `visibility/HologramVisibilityTracker.java`: Tracks which players see which holograms
  - `gui/`: Hologram editor GUI

**`npc/`:**
- Purpose: NPCs with dialogue trees and quests
- Contains: Manager, dialogue system, rendering, PacketEvents integration
- Structure:
  - `NPCManager.java`: Registry for NPCs
  - `NPC.java`: Represents an NPC (name, location, appearance, dialogue)
  - `command/NPCCommand.java`: `/npc create|remove|movehere|list|gui`
  - `config/NPCConfig.java`: Loads NPC definitions from YAML
  - `listener/NPCListener.java`: Session management for player-NPC interactions
  - `listener/NPCPacketListener.java`: PacketEvents integration for interaction detection
  - `dialogue/`: Dialogue tree system with branching logic
  - `dialogue/action/`: Actions executed during dialogue (quests, commands, rewards)
  - `dialogue/condition/`: Conditions for dialogue branches (level, completed quest, etc.)
  - `render/NPCRenderer.java`: Renders NPC (armor stands + cosmetics)
  - `render/NPCViewTracker.java`: View distance culling for performance
  - `gui/`: NPC editor GUI

**`quest/`:**
- Purpose: Quest system with objectives, rewards, progress tracking
- Contains: Manager, quest definitions, listener, data persistence
- Structure:
  - `QuestManager.java`: Registry for quests; tracks per-player active/completed quests
  - `Quest.java`: Quest definition (id, name, objectives, rewards, prerequisites)
  - `QuestProgress.java`: Per-player quest state (active objectives)
  - `QuestObjective.java`: Individual quest objective
  - `QuestReward.java`: Rewards on completion (items, money, commands, pets)
  - `command/QuestCommand.java`: `/quest active|completed|abandon|gui`
  - `config/QuestConfig.java`: Loads quest definitions from YAML
  - `data/QuestStore.java`: Saves/loads quest-data.yml
  - `listener/QuestListener.java`: Tracks progress (kills, block breaks, etc.); action bar display
  - `gui/QuestGUI.java`: Quest journal GUI

**`timeline/`:**
- Purpose: Event sequencer (timed actions)
- Contains: Manager, timeline definitions, action system
- Structure:
  - `TimelineManager.java`: Registry for timelines; tracks running timelines
  - `Timeline.java`: Timeline definition (sequences of timed actions)
  - `TimelineAction.java`: Actions executed at specific times
  - `command/TimelineCommand.java`: `/timeline play|stop|list`
  - `config/TimelineConfig.java`: Loads timeline definitions from YAML
  - `task/TimelineTickTask.java`: Ticks active timelines
  - `action/`: Built-in timeline actions

**`emitter/` (Particle Emitter System - shown as `particle/`):**
- Purpose: Particle effect emitters at locations
- Contains: Manager, emitter definitions, command, listeners
- Structure:
  - `EmitterManager.java`: Registry for emitters
  - `EmitterData.java`: Emitter definition (location, particle type, rate)
  - `command/EmitterCommand.java`: `/emitter create|remove|list|edit|info|gui`
  - `config/EmitterConfig.java`: Loads emitter definitions from YAML
  - `listener/EmitterLifecycleListener.java`: Chunk/world unload cleanup
  - `task/EmitterTickTask.java`: Ticks all active emitters
  - `script/`: Particle script evaluation

**`gui/`:**
- Purpose: Menu framework (inventories, click handlers)
- Contains: Menu base classes, config, command, listener
- Structure:
  - `MenuManager.java`: Registry for open menus
  - `Menu.java`: Base menu class (inventory, click handlers, close handlers)
  - `MenuItem.java`: Individual menu item (display, click behavior)
  - `MenuConfig.java`: Loads menu definitions from YAML files
  - `MenuListener.java`: Inventory click/close event handler
  - `command/MenuCommand.java`: `/menu <id>`
  - `task/MenuTickTask.java`: Ticks active menus (animations, updates)

**`reactive/`:**
- Purpose: Context-aware cosmetics (apply/remove cosmetics based on conditions)
- Contains: Manager, rule definitions, condition/effect system
- Structure:
  - `ReactiveManager.java`: Registry for reactive rules
  - `ReactiveRule.java`: Rule definition (conditions + effects)
  - `command/`: (if applicable)
  - `config/ReactiveConfig.java`: Loads rule definitions from YAML
  - `condition/`: Condition types (wearing armor, has item, in region, etc.)
  - `effect/`: Effect types (apply cosmetic, remove cosmetic, etc.)
  - `task/ReactiveTickTask.java`: Evaluates rules; applies/removes cosmetics (20-tick interval)

**`hook/`:**
- Purpose: Optional integrations with third-party plugins
- Contains: PlaceholderAPI hook, Vault hook, ModelEngine hook
- Key files:
  - `PlaceholderHook.java`: Registers placeholders (%servercore_pet_name%, etc.)
  - `VaultHook.java`: Economy and permission integration for quest rewards
  - `ModelEngineHook.java`: Custom ModelEngine models for pets

**`config/`:**
- Purpose: Central configuration
- Contains: `ServerCoreConfig.java` — reads config.yml (system enabled/disabled flags)

**`command/`:**
- Purpose: Admin commands
- Contains: `ServerCoreCommand.java` — `/servercore reload`

## Key File Locations

**Entry Points:**
- `src/main/java/net/axther/serverCore/ServerCore.java`: Plugin main class

**Configuration:**
- `src/main/resources/plugin.yml`: Plugin metadata, commands, permissions
- `src/main/resources/config.yml`: System enable/disable toggles
- `src/main/resources/cosmetics.yml`: Cosmetic profile definitions
- `src/main/resources/pets/`: Pet definition files (*.yml)
- `src/main/resources/quests/`: Quest definition files (*.yml)

**Core Logic:**
- `src/main/java/net/axther/serverCore/cosmetic/CosmeticManager.java`: Cosmetic lifecycle
- `src/main/java/net/axther/serverCore/pet/PetManager.java`: Pet lifecycle
- `src/main/java/net/axther/serverCore/hologram/HologramManager.java`: Hologram lifecycle
- `src/main/java/net/axther/serverCore/quest/QuestManager.java`: Quest lifecycle
- `src/main/java/net/axther/serverCore/npc/NPCManager.java`: NPC lifecycle

**Data Persistence:**
- `src/main/java/net/axther/serverCore/cosmetic/data/CosmeticStore.java`: Cosmetic serialization
- `src/main/java/net/axther/serverCore/pet/data/PetStore.java`: Pet serialization
- `src/main/java/net/axther/serverCore/quest/data/QuestStore.java`: Quest progress serialization

**Testing:**
- `src/test/java/`: Test files (JUnit 5, Mockito)

## Naming Conventions

**Files:**
- Managers: `{System}Manager.java` (e.g., `CosmeticManager.java`, `PetManager.java`)
- Configs: `{System}Config.java` (e.g., `CosmeticConfig.java`, `PetConfig.java`)
- Commands: `{System}Command.java` (e.g., `CosmeticCommand.java`, `PetCommand.java`)
- Listeners: `{System}Listener.java` or `{System}LifecycleListener.java`
- Tasks: `{System}TickTask.java` (e.g., `CosmeticTickTask.java`)
- Data stores: `{System}Store.java` (e.g., `CosmeticStore.java`)
- Profiles: `{Mob/Pet}{System}Profile.java` (e.g., `PandaCosmeticProfile.java`, `RatPetProfile.java`)
- Instances: `{System}Instance.java` (e.g., `CosmeticInstance.java`, `PetInstance.java`)

**Directories:**
- System directories: lowercase plural or system name (`cosmetic/`, `pet/`, `hologram/`, `npc/`, `quest/`, `particle/`, `timeline/`, `reactive/`, `gui/`, `api/`, `hook/`, `command/`, `config/`)
- Sub-directories: lowercase function (`command/`, `config/`, `listener/`, `task/`, `profiles/`, `data/`, `action/`, `condition/`, `dialogue/`, `render/`, `builder/`, `event/`, `visibility/`, `gui/`, `util/`, `script/`, `effect/`)

**Classes:**
- Manager classes: CamelCase, ends with "Manager" (e.g., `CosmeticManager`)
- Profile classes: CamelCase, ends with "Profile" (e.g., `PandaCosmeticProfile`)
- Instance classes: CamelCase, ends with "Instance" (e.g., `CosmeticInstance`)
- Command classes: CamelCase, ends with "Command" (e.g., `CosmeticCommand`)
- Listener classes: CamelCase, ends with "Listener" (e.g., `CosmeticLifecycleListener`)
- Task classes: CamelCase, ends with "TickTask" (e.g., `CosmeticTickTask`)
- Data classes: CamelCase, ends with "Store" or data model name (e.g., `CosmeticStore`, `PetData`)
- Config classes: CamelCase, ends with "Config" (e.g., `CosmeticConfig`)

**Methods:**
- camelCase for methods (e.g., `applyCosmetic()`, `getProfile()`, `removeCosmetics()`)
- Getters: `get{Property}()` (e.g., `getProfile()`, `getManager()`)
- Setters: `set{Property}()` (e.g., `setStore()`)
- Check/query: `is{State}()`, `has{Thing}()` (e.g., `isDead()`, `hasCosmetics()`)
- Action methods: verb (e.g., `apply()`, `remove()`, `tick()`, `destroy()`)

## Where to Add New Code

**New System (e.g., new cosmetic system):**
- Create directory: `src/main/java/net/axther/serverCore/{system}/`
- Core files:
  - `{System}Manager.java` — registry and lifecycle
  - `{System}Instance.java` — state per entity
  - `{System}Config.java` — YAML loading
  - `{System}Store.java` — data persistence
  - `listener/{System}LifecycleListener.java` — cleanup on events
  - `command/{System}Command.java` — user-facing commands
  - `task/{System}TickTask.java` — per-tick updates
- Wire in `ServerCore.onEnable()` using same pattern as existing systems

**New Mob Cosmetic Profile:**
- File: `src/main/java/net/axther/serverCore/cosmetic/profiles/{MobName}CosmeticProfile.java`
- Extend: `MobCosmeticProfile`
- Example: `PandaCosmeticProfile extends MobCosmeticProfile`
- Register in: `ServerCore.onEnable()` in `registerProfile()` calls

**New Pet Profile:**
- File: `src/main/java/net/axther/serverCore/pet/profiles/{PetName}PetProfile.java`
- Extend: `PetProfile`
- Example: `RatPetProfile extends PetProfile`
- Register in: `ServerCore.registerJavaPetProfiles()`

**New Event (for API):**
- File: `src/main/java/net/axther/serverCore/api/event/{Action}Event.java`
- Extend: `org.bukkit.event.Event`
- Implement: `Cancellable` if needed
- Fire in: Manager methods using `Bukkit.getPluginManager().callEvent(event)`

**New Builder (fluent API):**
- File: `src/main/java/net/axther/serverCore/api/builder/{Name}Builder.java`
- Pattern: Fluent chain methods returning `this`
- Make accessible via: `ServerCoreAPI.get().create{Name}Builder()`

**New Utility Function:**
- File: `src/main/java/net/axther/serverCore/{system}/util/{UtilName}.java`
- Use: Static methods if stateless, or utility class pattern
- Example: `PetUtil.java` with static helper methods

**New Command Subcommand:**
- Edit: `src/main/java/net/axther/serverCore/{system}/command/{System}Command.java`
- Add case in `onCommand()` switch statement
- Add subcommand to `SUBCOMMANDS` list
- Update plugin.yml usage line

**New YAML Configuration:**
- File: `src/main/resources/{system}s.yml` or `{system}s/{name}.yml`
- Loader: Create config class extending existing pattern
- Register in: `ServerCore.onEnable()` after creating manager

## Special Directories

**`api/event/`:**
- Purpose: Custom Bukkit events for plugin interop
- Generated: No
- Committed: Yes
- Examples: `CosmeticApplyEvent.java`, `PetSummonEvent.java`, `HologramClickEvent.java`

**`cosmetic/profiles/`, `pet/profiles/`:**
- Purpose: Java subclasses for entity-specific behavior
- Generated: No (manually created)
- Committed: Yes
- Example: `PandaCosmeticProfile.java`

**`hook/`:**
- Purpose: Optional soft-dependency integrations
- Generated: No
- Committed: Yes
- Loaded conditionally in `ServerCore.onEnable()` to avoid NoClassDefFoundError

**`npc/dialogue/`, `hologram/action/`, `hologram/condition/`, `timeline/action/`, `reactive/condition/`, `reactive/effect/`:**
- Purpose: Pluggable system components for YAML scripting
- Generated: No
- Committed: Yes
- Registration: Via type mapping (String → Class) in config loaders

---

*Structure analysis: 2026-03-21*
