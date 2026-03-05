<div align="center">

# ServerCore

### The All-in-One Server Enhancement Plugin for Paper 1.21

[![Build](https://github.com/Axtheris/ServerCore/actions/workflows/build.yml/badge.svg)](https://github.com/Axtheris/ServerCore/actions/workflows/build.yml)
&nbsp;
![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
&nbsp;
![Paper 1.21](https://img.shields.io/badge/Paper-1.21-2B6CB0?logo=data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI+PHBhdGggZD0iTTMgM2gxOHYxOEgzVjN6IiBmaWxsPSJ3aGl0ZSIgZmlsbC1vcGFjaXR5PSIwLjMiLz48L3N2Zz4=)
&nbsp;
[![License: MIT](https://img.shields.io/badge/License-MIT-22C55E.svg)](LICENSE)
&nbsp;
![Model Engine](https://img.shields.io/badge/Model%20Engine-optional-9CA3AF)

<br>

Nine plug-and-play systems -- **cosmetics**, **particle emitters**, **pets**, **holograms**, **NPCs with dialogue**, **quests**, **event timelines**, **reactive cosmetics**, and **chest GUIs** -- all YAML-driven so server operators never need to touch code. Power users get a particle scripting language, a public API with custom Bukkit events, and PlaceholderAPI support.

<br>

[Features](#features) &bull; [Installation](#installation) &bull; [Commands](#commands--permissions) &bull; [Configuration](#configuration) &bull; [Building](#building-from-source) &bull; [API](#api) &bull; [Contributing](#contributing)

</div>

---

## Features

### Mob Cosmetics

Place invisible armor stands on mob heads that display held items as wearable accessories. Stands track mob movement every tick with per-mob head-offset geometry.

- **Two-tier profile system**: Java profiles for custom behavior, YAML profiles for zero-code additions
- **Live calibration**: adjust offsets in-game with `/cosmetic calibrate`
- **Persistence**: cosmetics survive server restarts and chunk reloads
- **GUI browser**: browse supported mobs and manage cosmetics from a chest menu

### Particle Emitters

Attach configurable particle emitters to locations with 32 built-in patterns and a scripting language for custom effects.

- **33 patterns**: rings, helixes, galaxies, tornadoes, DNA strands, hearts, crowns, and more
- **Particle scripting**: write mathematical expressions in YAML to define custom particle behaviors
- **Live editing**: create, modify, and remove emitters in-game
- **GUI manager**: browse and teleport to emitters from a chest menu

### Pet System

A complete pet system where players summon, dismiss, feed, and interact with companions.

- **10 built-in pets**: Dragon, Fox, Ghost, Mushroom, Owl, Penguin, Rat, Robot, Skull, Wisp
- **Pet collection**: players own and collect pets; ownership persists across sessions
- **AI state machine**: following, sitting, and attacking states with smooth transitions
- **Combat-capable**: configurable attack range, damage, and cooldown per pet
- **GUI collection**: browse owned pets and summon them from a chest menu
- **Model Engine integration**: optional 3D models with idle, walk, attack, and sit animations

### Holograms

Floating text using Paper 1.21 text display entities with MiniMessage formatting and animations.

- **MiniMessage support**: gradients, colors, bold, italic, and all MiniMessage features
- **Animations**: bob, rotate, and pulse effects with configurable amplitude and frequency
- **Chunk-aware**: holograms automatically spawn and despawn with chunk loading

### NPCs & Dialogue

Place static NPCs with full player-model skins and branching dialogue trees. Powered by PacketEvents for zero TPS impact -- NPCs are purely client-side entities.

- **Full player-model NPCs**: humanoid NPCs with visible bodies, arms, legs, and custom skins via PacketEvents
- **Dialogue trees**: branching conversations with conditions and actions
- **Conditions**: permission checks, item checks for gating dialogue options
- **Actions**: run commands, send messages, give items, play sounds
- **Look-at-player**: NPCs turn to face nearby players
- **Clickable choices**: dialogue options rendered as clickable chat messages
- **Per-player visibility**: NPCs appear and disappear based on configurable view distance

### Quests

A YAML-driven quest system integrated with NPC dialogue. Players accept quests from NPCs, complete objectives, and return for rewards.

- **Three objective types**: fetch items, kill mobs, talk to NPCs
- **Three reward types**: items, XP, console commands with player placeholders
- **Dialogue integration**: quest conditions and actions in NPC dialogue trees
- **Repeatable quests**: optional cooldown timers for repeatable content
- **Persistence**: quest progress survives server restarts
- **Inline or standalone**: define quests in NPC files or separate quest files

### Event Timelines

Schedule sequences of actions on a tick timeline for boss intros, server events, and coordinated shows.

- **Keyframe system**: define actions at specific tick offsets
- **Built-in actions**: titles, sounds, commands, mob spawning, weather, time, camera shake
- **Looping support**: timelines can loop for continuous effects
- **Audience targeting**: nearby players or all online players

### Reactive Cosmetics

Cosmetics and pets that visually change based on world state.

- **Conditions**: time of day, weather, biome, player health, nearby players
- **Effects**: glow toggle, particle trails, item swapping, color changes
- **Automatic**: no player interaction needed; effects apply and remove themselves

### GUI System

Chest-based menus with pagination, confirmation dialogs, and integration across all systems.

- **Paginated menus**: automatic page navigation for large lists
- **Confirmation dialogs**: yes/no prompts before destructive actions
- **Glass pane borders**: polished UI with no item duplication exploits
- **MiniMessage titles**: gradient and styled menu titles

---

## Soft Dependencies

| Plugin | Required | Purpose |
|:-------|:--------:|:--------|
| [Model Engine](https://mythiccraft.io/index.php?pages/model-engine/) R4+ | No | 3D custom models and animations for pets |
| [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) | No | Exposes ServerCore data as placeholders |
| [PacketEvents](https://github.com/retrooper/packetevents) 2.7+ | For NPCs | Full player-model NPCs with skins |

---

## Requirements

| | Version |
|:--|:--------|
| Java | 21+ |
| Server | [Paper](https://papermc.io/) 1.21+ |

---

## Installation

1. Download the latest JAR from [Releases](https://github.com/Axtheris/ServerCore/releases)
2. Drop it into your server's `plugins/` folder
3. Restart the server
4. Edit the generated configs in `plugins/ServerCore/`

---

## Central Configuration

All systems can be individually enabled or disabled in `config.yml`:

```yaml
systems:
  cosmetics:
    enabled: true
  emitters:
    enabled: true
  pets:
    enabled: true
  holograms:
    enabled: true
  npcs:
    enabled: true
  quests:
    enabled: true
  timelines:
    enabled: true
  reactive:
    enabled: true
  gui:
    enabled: true
```

Use `/servercore reload` to apply changes without restarting.

---

## Commands & Permissions

### `/servercore` - Plugin Management

| Subcommand | Description | Permission |
|:-----------|:------------|:-----------|
| `reload` | Reload central config | `servercore.admin` |

### `/cosmetic` - Mob Cosmetics

| Subcommand | Description | Permission |
|:-----------|:------------|:-----------|
| `apply` | Apply held item as cosmetic to targeted mob | `servercore.cosmetic` |
| `remove` | Remove cosmetic from targeted mob | `servercore.cosmetic` |
| `clear` | Remove all active cosmetics | `servercore.cosmetic` |
| `info` | Show item info for held item | `servercore.cosmetic` |
| `calibrate` | Enter calibration mode | `servercore.cosmetic` |
| `gui` | Open cosmetic manager GUI | `servercore.cosmetic` |

### `/emitter` - Particle Emitters

| Subcommand | Description | Permission |
|:-----------|:------------|:-----------|
| `create` | Create a new particle emitter | `servercore.emitter` |
| `remove` | Remove an emitter | `servercore.emitter` |
| `list` | List all active emitters | `servercore.emitter` |
| `edit` | Edit an existing emitter | `servercore.emitter` |
| `info` | Show emitter details | `servercore.emitter` |
| `gui` | Open emitter manager GUI | `servercore.emitter` |

### `/pet` - Pet System

| Subcommand | Description | Permission |
|:-----------|:------------|:-----------|
| `summon <type>` | Summon a pet | `servercore.pet` |
| `dismiss` | Dismiss active pets | `servercore.pet` |
| `sit` | Toggle sit mode | `servercore.pet` |
| `follow` | Make pets follow you | `servercore.pet` |
| `feed` | Feed your pets | `servercore.pet` |
| `list` | List owned pets | `servercore.pet` |
| `give <type> [player]` | Give a pet item | `servercore.pet` |
| `gui` | Open pet collection GUI | `servercore.pet` |
| `reload` | Reload pet configs | `servercore.pet` |

### `/hologram` - Holograms

| Subcommand | Description | Permission |
|:-----------|:------------|:-----------|
| `create <id> <text>` | Create a hologram | `servercore.hologram` |
| `remove <id>` | Remove a hologram | `servercore.hologram` |
| `addline <id> <text>` | Add a text line | `servercore.hologram` |
| `removeline <id> <line#>` | Remove a text line | `servercore.hologram` |
| `edit <id> <line#> <text>` | Edit a text line | `servercore.hologram` |
| `list` | List all holograms | `servercore.hologram` |
| `near [radius]` | Find nearby holograms | `servercore.hologram` |
| `movehere <id>` | Move hologram to you | `servercore.hologram` |
| `setanimation <id> <type>` | Set hologram animation | `servercore.hologram` |

### `/npc` - NPCs & Dialogue

| Subcommand | Description | Permission |
|:-----------|:------------|:-----------|
| `create <id>` | Create an NPC at your location | `servercore.npc` |
| `remove <id>` | Remove an NPC | `servercore.npc` |
| `movehere <id>` | Teleport NPC to you | `servercore.npc` |
| `list` | List all NPCs | `servercore.npc` |
| `reload` | Reload NPC configs | `servercore.npc` |

### `/quest` - Quests

| Subcommand | Description | Permission |
|:-----------|:------------|:-----------|
| `active` | List your active quests with progress | `servercore.quest` |
| `completed` | List quests you have finished | `servercore.quest` |
| `abandon <id>` | Drop an active quest | `servercore.quest` |
| `reload` | Reload quest definitions from YAML | `servercore.quest` |

### `/timeline` - Event Timelines

| Subcommand | Description | Permission |
|:-----------|:------------|:-----------|
| `play <id>` | Play a timeline at your location | `servercore.timeline` |
| `stop [id]` | Stop a timeline (or all) | `servercore.timeline` |
| `list` | List all timelines | `servercore.timeline` |
| `reload` | Reload timeline configs | `servercore.timeline` |

> All permissions default to **OP only**.

---

## Configuration

### Particle Scripting

Define custom particle behaviors with mathematical expressions:

```yaml
emitters:
  my-spiral:
    particle: FLAME
    count: 20
    interval: 1
    script:
      x: "cos(t * 0.1 + i * 2 * pi / n) * 2"
      y: "t * 0.05 % 3"
      z: "sin(t * 0.1 + i * 2 * pi / n) * 2"
      r: "128 + 127 * sin(t * 0.05)"
      g: "50"
      b: "200"
```

**Variables:** `t` (tick), `i` (particle index), `n` (count), `pi`, `e`

**Functions:** `sin`, `cos`, `tan`, `abs`, `min`, `max`, `sqrt`, `rand`, `floor`, `ceil`, `lerp`

### Holograms (`holograms.yml`)

```yaml
holograms:
  spawn-welcome:
    world: world
    x: 0.5
    y: 68.0
    z: 0.5
    animation: BOB
    bob-amplitude: 0.1
    bob-frequency: 0.08
    lines:
      - "<gradient:#FF6B6B:#FFE66D><bold>Welcome to the Server</bold></gradient>"
      - "<gray>Type /help to get started"
```

### NPCs (`npcs/*.yml`)

```yaml
id: merchant
display-name: "<gold>Merchant Bob"
world: world
x: 100.5
y: 65.0
z: -50.5
yaw: 180.0
look-at-player: true
dialogue:
  start:
    text:
      - "<gold>Welcome, traveler!"
    choices:
      - label: "<yellow>Open shop"
        actions:
          - type: command
            value: "openshop merchant %player%"
      - label: "<red>Goodbye"
        actions:
          - type: message
            value: "<gold>Safe travels."
```

### Quests (`quests/*.yml`)

```yaml
id: gather-wood
display-name: "<gold>Lumberjack's Request"
description: "Gather 16 oak logs for the merchant."
accept-npc: merchant
turn-in-npc: merchant
repeatable: true
cooldown: 3600
objectives:
  - type: fetch
    material: OAK_LOG
    amount: 16
  - type: kill
    entity: ZOMBIE
    amount: 5
rewards:
  - type: item
    material: DIAMOND
    amount: 3
  - type: xp
    amount: 100
  - type: command
    value: "eco give %player% 500"
```

Quests can also be defined inline in NPC YAML files under a `quest:` key, where `accept-npc` and `turn-in-npc` default to the parent NPC.

### Timelines (`timelines/*.yml`)

```yaml
id: boss-intro
loop: false
audience: nearby
radius: 50
keyframes:
  0:
    - type: title
      title: "<red><bold>THE WARDEN AWAKENS"
      subtitle: "<dark_gray>Prepare yourself..."
      fade-in: 10
      stay: 40
      fade-out: 10
    - type: sound
      sound: ENTITY_WITHER_SPAWN
      volume: 1.0
      pitch: 0.5
  40:
    - type: command
      value: "summon minecraft:warden %x% %y% %z%"
  60:
    - type: camera-shake
      duration: 10
```

### Reactive Rules (`reactive.yml`)

```yaml
rules:
  night-glow:
    conditions:
      - type: time-of-day
        range: [13000, 23000]
    targets: [pets, cosmetics]
    effects:
      - type: glow
        color: AQUA
      - type: particle
        particle: END_ROD
        count: 2
```

---

## PlaceholderAPI

If PlaceholderAPI is installed, these placeholders are available:

| Placeholder | Description |
|:------------|:------------|
| `%servercore_pet_name%` | Active pet display name |
| `%servercore_pet_type%` | Active pet type ID |
| `%servercore_pet_count%` | Number of owned pets |
| `%servercore_cosmetic_count%` | Active cosmetics count |
| `%servercore_emitter_count%` | Active emitters count |
| `%servercore_hologram_count%` | Total holograms count |
| `%servercore_quest_active%` | Number of active quests |
| `%servercore_quest_completed%` | Number of completed quests |

---

## Building from Source

```bash
git clone https://github.com/Axtheris/ServerCore.git
cd ServerCore
./gradlew build
```

The output JAR will be at `build/libs/ServerCore-2.0.0.jar`.

To launch a Paper test server with the plugin auto-loaded:

```bash
./gradlew runServer
```

> Requires **Gradle 8.8+** and **Java 21**.

### Testing

```bash
./gradlew test
```

The test suite includes 275 tests covering particle emitter patterns, the scripting engine, and GUI pagination. All particle patterns are benchmarked for stability and performance under sustained load.

---

## API

Other plugins can interact with ServerCore through the public API:

```java
ServerCoreAPI api = ServerCoreAPI.get();

// Pets
api.getPetManager().summonPet(player, profile);

// Cosmetics
api.getCosmeticManager().applyCosmetic(mob, item);

// Emitters
api.getEmitterManager();

// Holograms
api.getHologramManager();

// Timelines
api.getTimelineManager().play("boss-intro", location);

// Quests
api.getQuestManager().acceptQuest(player, "gather-wood");

// NPCs
api.getNPCManager();
```

### Custom Events

Listen for ServerCore events in your plugin:

```java
@EventHandler
void onPetSummon(PetSummonEvent event) {
    // Cancel, modify, or react to pet summons
}
```

Available events: `PetSummonEvent`, `PetDismissEvent`, `CosmeticApplyEvent`, `CosmeticRemoveEvent`, `EmitterCreateEvent`, `HologramCreateEvent`, `DialogueStartEvent`, `TimelinePlayEvent`, `QuestStartEvent`, `QuestCompleteEvent`

---

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit your changes
4. Push to the branch: `git push origin feature/my-feature`
5. Open a Pull Request

---

## License

This project is licensed under the **MIT License**. See the [LICENSE](LICENSE) file for details.

---

<div align="center">
<sub>Built with Paper API &bull; No NMS or reflection</sub>
</div>
