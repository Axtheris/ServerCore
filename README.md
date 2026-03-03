<div align="center">

# ServerCore

### Mob Cosmetics, Particle Emitters & Pets for Paper 1.21

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

A feature-rich Paper plugin that brings **cosmetic accessories** to mobs, **particle emitters** to your world, and a complete **pet system** with optional [Model Engine](https://mythiccraft.io/index.php?pages/model-engine/) support for 3D models — all driven by YAML config so server operators never need to touch code.

<br>

[Features](#-features) &bull; [Installation](#-installation) &bull; [Commands](#-commands--permissions) &bull; [Configuration](#-configuration) &bull; [Building](#%EF%B8%8F-building-from-source) &bull; [API](#-api) &bull; [Contributing](#-contributing)

</div>

---

## Features

### Mob Cosmetics

Place invisible armor stands on mob heads that display held items as wearable accessories. Stands track mob movement every tick with per-mob head-offset geometry.

- **Two-tier profile system** — Java profiles for custom behavior, YAML profiles for zero-code additions
- **Live calibration** — adjust offsets in-game with the `/cosmetic calibrate` command
- **Automatic cleanup** — cosmetics are removed on entity death, chunk unload, and server stop
- **Equipment-locked marker stands** — invisible, no gravity, non-persistent, interaction-blocked

### Particle Emitters

Attach configurable particle emitters to locations or entities with full control over patterns, types, and behavior.

- **Multiple patterns** — customize particle shapes and emission behavior
- **Entity-attached or static** — bind emitters to mobs/players or world coordinates
- **Live editing** — create, modify, and remove emitters in-game

### Pet System

A complete pet system where players summon, dismiss, feed, and interact with companions.

- **10 built-in pets** — Dragon, Fox, Ghost, Mushroom, Owl, Penguin, Rat, Robot, Skull, Wisp
- **YAML-driven profiles** — each pet is a single `.yml` file, add new pets without code
- **AI state machine** — following, sitting, and idle states with smooth transitions
- **Combat-capable** — configurable attack range, damage, and cooldown per pet
- **Hunger & feeding** — pets get hungry over time; players feed them to keep them happy
- **Animations** — hover bobbing, hop animations, and configurable amplitudes/frequencies
- **Ambient sounds** — per-pet sound effects with randomized chance and pitch
- **Model Engine integration** — optional 3D models with idle, walk, attack, and sit animations

---

## Soft Dependencies

| Plugin | Required | Purpose |
|:-------|:--------:|:--------|
| [Model Engine](https://mythiccraft.io/index.php?pages/model-engine/) R4+ | No | Enables 3D custom models and animations for pets |

When Model Engine is not installed, pets render as floating head items on armor stands.

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

## Commands & Permissions

### `/cosmetic` — Mob Cosmetics

| Subcommand | Description | Permission |
|:-----------|:------------|:-----------|
| `apply` | Apply a cosmetic to the targeted mob | `servercore.cosmetic` |
| `remove` | Remove the cosmetic from the targeted mob | `servercore.cosmetic` |
| `clear` | Remove all active cosmetics | `servercore.cosmetic` |
| `info` | Show cosmetic info for the targeted mob | `servercore.cosmetic` |
| `calibrate` | Enter calibration mode to adjust offsets live | `servercore.cosmetic` |

### `/emitter` — Particle Emitters

| Subcommand | Description | Permission |
|:-----------|:------------|:-----------|
| `create` | Create a new particle emitter | `servercore.emitter` |
| `remove` | Remove an emitter | `servercore.emitter` |
| `list` | List all active emitters | `servercore.emitter` |
| `edit` | Edit an existing emitter | `servercore.emitter` |
| `info` | Show emitter details | `servercore.emitter` |

### `/pet` — Pet System

| Subcommand | Description | Permission |
|:-----------|:------------|:-----------|
| `summon` | Summon your active pet | `servercore.pet` |
| `dismiss` | Dismiss your active pet | `servercore.pet` |
| `sit` | Toggle pet sit/follow mode | `servercore.pet` |
| `follow` | Make your pet follow you | `servercore.pet` |
| `feed` | Feed your pet | `servercore.pet` |
| `list` | List your available pets | `servercore.pet` |
| `give` | Give a pet to a player | `servercore.pet` |
| `reload` | Reload pet configs | `servercore.pet` |

> All permissions default to **OP only**. Commands use player raycast targeting where applicable.

---

## Configuration

### `cosmetics.yml` — Mob Cosmetic Profiles

Define head-offset geometry per entity type. Java profiles take priority over config entries.

```yaml
mobs:
  panda:
    head-y: 1.1          # Height of head center from feet (blocks)
    head-forward-z: 0.9  # Forward offset from center to head
    head-side-x: 0.0     # Side offset (positive = right)
    use-small-stand: false
```

### `pets/*.yml` — Pet Profiles

Each pet is its own file. Add new pets by dropping in a new YAML file and running `/pet reload`.

```yaml
display-name: "Fox"
head-texture: "REPLACE_ME"
item-name: "<gradient:#FF8C00:#FFDF00><bold>Fox Pet</bold></gradient>"
item-lore:
  - "<gold>A sly and playful fox kit."
use-small-stand: true
animation-type: HOP
bob-amplitude: 0.11
bob-frequency: 0.10
hover-height: 1.0
follow-speed: 0.33
can-attack: true
attack-range: 5.0
attack-damage: 1.5
attack-cooldown-ticks: 18
feed-cooldown-ticks: 450
heart-particle-count: 5
sounds:
  sniff:
    sound: ENTITY_FOX_SNIFF
    volume: 0.5
    pitch: 1.3
    chance: 180
  yip:
    sound: ENTITY_FOX_AMBIENT
    volume: 0.3
    pitch: 1.5
    chance: 400

# Optional — requires Model Engine plugin
# model-id: "fox_pet"
# model-animations:
#   idle: "anim_idle"
#   walk: "anim_walk"
#   attack: "anim_attack"
#   sit: "anim_sit"
```

<details>
<summary><strong>Pet config reference</strong></summary>

| Key | Type | Description |
|:----|:-----|:------------|
| `display-name` | String | Display name shown to players |
| `head-texture` | String | Base64 skin texture for the pet head |
| `item-name` | String | MiniMessage-formatted item name |
| `item-lore` | List | MiniMessage-formatted lore lines |
| `use-small-stand` | Boolean | Use a small armor stand (half-scale) |
| `animation-type` | Enum | `HOP` or `BOB` |
| `bob-amplitude` | Double | Vertical bob amplitude in blocks |
| `bob-frequency` | Double | Bob speed multiplier |
| `hover-height` | Double | Base hover height above ground |
| `follow-speed` | Double | Movement speed when following |
| `can-attack` | Boolean | Whether the pet can attack mobs |
| `attack-range` | Double | Attack targeting range in blocks |
| `attack-damage` | Double | Damage dealt per hit |
| `attack-cooldown-ticks` | Integer | Ticks between attacks |
| `feed-cooldown-ticks` | Integer | Ticks between feedings |
| `heart-particle-count` | Integer | Heart particles on feed |
| `sounds.<name>.sound` | String | Bukkit Sound enum value |
| `sounds.<name>.volume` | Double | Sound volume (0.0–1.0) |
| `sounds.<name>.pitch` | Double | Sound pitch multiplier |
| `sounds.<name>.chance` | Integer | 1-in-N chance per tick to play |
| `model-id` | String | Model Engine model ID (optional) |
| `model-animations` | Map | Model Engine animation mappings (optional) |

</details>

---

## Building from Source

```bash
git clone https://github.com/Axtheris/ServerCore.git
cd ServerCore
./gradlew build
```

The output JAR will be at `build/libs/ServerCore-1.0.0.jar`.

To launch a Paper test server with the plugin auto-loaded:

```bash
./gradlew runServer
```

> Requires **Gradle 8.8+** and **Java 21**.

---

## API

Other plugins can interact with ServerCore's systems through its manager classes:

```java
ServerCore plugin = ServerCore.getInstance();

// Mob Cosmetics
CosmeticManager cosmetics = plugin.getCosmeticManager();
cosmetics.apply(mob, itemStack);
cosmetics.remove(mob);

// Particle Emitters
EmitterManager emitters = plugin.getEmitterManager();

// Pets
PetManager pets = plugin.getPetManager();
pets.summonPet(player, petProfile);
pets.dismissPet(player);
```

All managers are available after `ServerCore.onEnable()`.

---

## Contributing

1. Fork the repository
2. Create a feature branch — `git checkout -b feature/my-feature`
3. Commit your changes
4. Push to the branch — `git push origin feature/my-feature`
5. Open a Pull Request

---

## License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

<div align="center">
<sub>Built with Paper API &bull; No NMS or reflection</sub>
</div>
