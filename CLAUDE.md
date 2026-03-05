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
