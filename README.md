# ServerCore

A Paper 1.21 plugin providing mob cosmetics, particle emitters, and a full pet system with optional [Model Engine](https://mythiccraft.io/index.php?pages/model-engine/) support.

[![Build](https://github.com/Axtheris/ServerCore/actions/workflows/build.yml/badge.svg)](https://github.com/Axtheris/ServerCore/actions/workflows/build.yml)
![Java 21](https://img.shields.io/badge/Java-21-orange)
![Paper 1.21](https://img.shields.io/badge/Paper-1.21-blue)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
![Model Engine](https://img.shields.io/badge/Model%20Engine-optional-lightgrey)

## Features

### Mob Cosmetics
Place invisible armor stands on mob heads that display held items as cosmetic accessories. Stands track mob movement every tick with configurable head-offset geometry.

- **Java profiles** for mobs needing custom behavior
- **YAML profiles** (`cosmetics.yml`) so server operators can add new mob support without code changes
- Per-mob offset calibration (Y height, forward Z, side X)

### Particle Emitters
Create and manage particle emitters attached to locations or entities with configurable patterns, particle types, and behavior.

### Pet System
A full pet system where players can summon, dismiss, feed, and interact with configurable pets.

- **YAML-driven pet profiles** (`pets/*.yml`) — each pet has its own config file
- Built-in profiles: dragon, fox, ghost, mushroom, owl, penguin, rat, robot, skull, wisp
- Pet states: following, sitting, idle with smooth AI transitions
- Hunger/feeding mechanics
- Optional **Model Engine** integration for 3D pet models

## Soft Dependencies

| Plugin | Required | Purpose |
|--------|----------|---------|
| [Model Engine](https://mythiccraft.io/index.php?pages/model-engine/) R4+ | No | Enables 3D custom models for pets |

## Requirements

- Java 21
- Paper 1.21+

## Installation

1. Download the latest JAR from [Releases](https://github.com/Axtheris/ServerCore/releases)
2. Place it in your server's `plugins/` folder
3. Restart the server
4. Configure cosmetics in `plugins/ServerCore/cosmetics.yml` and pets in `plugins/ServerCore/pets/`

## Commands & Permissions

| Command | Usage | Permission | Default |
|---------|-------|------------|---------|
| `/cosmetic` | `/cosmetic <apply\|remove\|clear\|info\|calibrate>` | `servercore.cosmetic` | OP |
| `/emitter` | `/emitter <create\|remove\|list\|edit\|info>` | `servercore.emitter` | OP |
| `/pet` | `/pet <summon\|dismiss\|sit\|follow\|feed\|list\|give\|reload>` | `servercore.pet` | OP |

All commands use player raycast targeting where applicable.

## Configuration

### cosmetics.yml
Defines mob cosmetic profiles with head offset geometry (Y height, forward Z, side X) per entity type. Java-based profiles take priority over config-based ones.

### pets/*.yml
Each pet has its own YAML file defining appearance, behavior, sounds, hunger settings, and optional Model Engine model IDs. Add new pets by creating a new YAML file — no code changes needed.

## Building from Source

```bash
./gradlew build
```

The output JAR will be at `build/libs/ServerCore-1.0.0.jar`.

To launch a test server with the plugin loaded:

```bash
./gradlew runServer
```

Requires Gradle 8.8+ and Java 21.

## API

Other plugins can interact with ServerCore through its manager classes:

- **`CosmeticManager`** — register profiles, apply/remove cosmetics on mobs
- **`EmitterManager`** — create and manage particle emitters
- **`PetManager`** — summon, dismiss, and query player pets

Access them via `ServerCore.getInstance()`.

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/my-feature`)
3. Commit your changes
4. Push to the branch
5. Open a Pull Request

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
