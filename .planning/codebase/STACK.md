# Technology Stack

**Analysis Date:** 2026-03-21

## Languages

**Primary:**
- Java 21 - Plugin implementation for Paper 1.21 Minecraft server
- YAML - Configuration files for all systems (cosmetics, pets, quests, NPCs, etc.)

## Runtime

**Environment:**
- Paper 1.21 (Minecraft server fork)
- JVM 21+

**Build Tool:**
- Gradle 8.8
- Plugin: `xyz.jpenilla.run-paper` v2.3.1 (for dev server automation)

## Frameworks

**Core Minecraft:**
- Paper API 1.21.11-R0.1-SNAPSHOT - Main server API for plugin development

**Build/Dev:**
- JUnit 5.11.4 (Jupiter) - Test framework
- Mockito 5.14.2 - Mocking framework for unit tests

## Key Dependencies

**Critical (compileOnly):**
- `io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT` - Bukkit/Paper plugin API (no NMS or reflection)
- `com.github.retrooper:packetevents-spigot:2.7.0` - Packet interception for NPC rendering and interactions
- `com.ticxo.modelengine:ModelEngine:R4.0.7` - 3D model support for pets (optional, detected at runtime)

**Soft Dependencies (plugins):**
- `me.clip:placeholderapi:2.11.6` - Placeholder integration for text variables (optional)
- `com.github.MilkBowl:VaultAPI:1.7.1` - Economy and permission API for quest rewards (optional)

## Configuration

**Environment:**
- No environment variables required. All configuration via YAML files.
- `.env` files: None
- Config files location: Plugin data folder (`plugins/ServerCore/`)

**Build:**
- `build.gradle` - Build configuration
- `settings.gradle` - Root project definition
- `gradle.properties` - Empty (using defaults)
- `plugin.yml` - Minecraft plugin metadata (version templated from build.gradle)

**Plugin Config Files:**
- `config.yml` - Central configuration (system toggles, view distances)
- `cosmetics.yml` - Mob cosmetic profiles
- `quests/` - Quest definitions (YAML files)
- `pets/` - Pet profile definitions (separate YAML per pet type)
- `menus/` - Custom GUI menu definitions (YAML files)

## Platform Requirements

**Development:**
- Java 21 JDK
- Gradle 8.8
- IDE with Gradle support (IntelliJ IDEA, Eclipse, VS Code with extensions)

**Production:**
- Paper 1.21 server running on JVM 21+
- Optional plugins for full feature support:
  - PacketEvents (required for NPC/dialogue system)
  - ModelEngine (optional, enhances pet rendering with 3D models)
  - PlaceholderAPI (optional, enables placeholders in text)
  - Vault (optional, enables economy/permission quest rewards)

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

---

*Stack analysis: 2026-03-21*
