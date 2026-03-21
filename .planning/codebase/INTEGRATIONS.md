# External Integrations

**Analysis Date:** 2026-03-21

## APIs & External Services

**PacketEvents (NPC System):**
- SDK/Client: `com.github.retrooper:packetevents-spigot:2.7.0`
- Auth: None (plugin-based)
- Purpose: Packet interception for NPC rendering and click detection
  - Implementation: `NPCRenderer` and `NPCPacketListener` in `npc/render/` and `npc/listener/`
  - Integration point: `ServerCore.initNpcPacketSystem()` (line 400)
  - Soft dependency: Plugin fails gracefully if not installed

**ModelEngine (Pet Models):**
- SDK/Client: `com.ticxo.modelengine:ModelEngine:R4.0.7`
- Auth: None (plugin-based)
- Purpose: 3D model support for enhanced pet rendering
  - Implementation: `ModelEngineHook` in `pet/model/ModelEngineHook.java`
  - Detection: Runtime check via `Bukkit.getPluginManager().getPlugin("ModelEngine")`
  - Soft dependency: Optional; features degrade gracefully if not installed

**PlaceholderAPI:**
- SDK/Client: `me.clip:placeholderapi:2.11.6`
- Auth: None (plugin-based)
- Purpose: Text placeholder substitution in GUI menus, holograms, NPCs
  - Implementation: `PlaceholderHook` extends `PlaceholderExpansion`
  - Location: `hook/PlaceholderHook.java` (lines 23-118)
  - Soft dependency: Only classloaded at runtime if plugin is present
  - Supported placeholders:
    - `%servercore_pet_name%` - Active pet display name
    - `%servercore_pet_type%` - Active pet ID
    - `%servercore_pet_count%` - Total owned pets
    - `%servercore_cosmetic_count%` - Active cosmetics
    - `%servercore_emitter_count%` - Active particle emitters
    - `%servercore_hologram_count%` - Total holograms
    - `%servercore_quest_active%` - Active quest count
    - `%servercore_quest_completed%` - Completed quest count

**Vault API:**
- SDK/Client: `com.github.MilkBowl:VaultAPI:1.7.1`
- Auth: None (plugin-based)
- Purpose: Economy and permission system integration for quest rewards
  - Implementation: `VaultHook` in `hook/VaultHook.java`
  - Integration point: `ServerCore.setupVault()` (line 415)
  - Services accessed:
    - `Economy` (depositPlayer) - Add currency to players
    - `Permission` (playerAdd/playerRemove) - Grant/revoke permission nodes
  - Soft dependency: Optional; quests work without it (rewards just skip currency/permission steps)

## Data Storage

**Databases:**
- Type: Local YAML files (no external database)
- Persistence: File-based, in-memory during runtime

**Data Files:**
- `cosmetic-data.yml` - Active cosmetics per mob (UUID-indexed)
  - Location: `{plugin-folder}/cosmetic-data.yml`
  - Client: Paper's built-in `YamlConfiguration` (lines 30-77 in `CosmeticStore.java`)

- `pet-data.yml` - Player pet ownership and active pets
  - Location: `{plugin-folder}/pet-data.yml`
  - Client: `YamlConfiguration`

- `quest-progress.yml` - Player quest progress (active/completed)
  - Location: `{plugin-folder}/quest-progress.yml`
  - Client: `YamlConfiguration`

**File Storage:**
- Local filesystem only - YAML files in plugin data directory
- No cloud/remote storage

**Caching:**
- In-memory only during server runtime
- Managers hold active instances in maps: `Map<UUID, List<CosmeticInstance>>`, etc.
- No Redis or memcached

## Authentication & Identity

**Auth Provider:**
- Custom (Minecraft player UUID-based)
- Implementation: Built into Paper API
- All systems use `Player.getUniqueId()` for player identification

**Permissions:**
- Permission nodes defined in `plugin.yml` (lines 37-260)
- Checked via `Player.hasPermission(node)` (Paper API)
- Integration with Vault optional for custom permission plugins

## Monitoring & Observability

**Error Tracking:**
- None (no external service)
- Errors logged via `JavaPlugin.getLogger()` and `java.util.logging`
- Log level: Configurable at server level

**Logs:**
- Approach: Paper's built-in logging
- Output: Server console and `logs/latest.log`
- Log messages: Informational startup messages, warnings, errors

**Examples:**
- Line 181 (`ServerCore.java`): "Model Engine detected -- pet models enabled"
- Line 273: "NPC system enabled with PacketEvents"
- Line 318: "Quest system loaded with X quests"

## CI/CD & Deployment

**Hosting:**
- Paper 1.21 server (self-hosted or managed Minecraft hosting)
- Plugin deployment: `.jar` file in `plugins/` folder

**CI Pipeline:**
- None detected - no GitHub Actions, Jenkins, or CI config files
- Manual build: `./gradlew build` produces `build/libs/ServerCore-1.0.0.jar`
- Dev server: `./gradlew runServer` launches embedded Paper instance

**Deployment:**
- Copy JAR to `plugins/` folder
- Restart/reload server: `/reload` command or server restart
- Config files auto-load from `plugins/ServerCore/` data folder

## Environment Configuration

**Required env vars:**
- None - all configuration via YAML files

**Secrets location:**
- No API keys or secrets in use
- No `.env` files
- All configuration in checked-in YAML files (`plugin.yml`, `config.yml`, etc.)

## Webhooks & Callbacks

**Incoming:**
- None - Plugin runs only server-side, no inbound webhooks

**Outgoing:**
- None - Plugin does not make outbound HTTP requests

**Event System:**
- Internal event bus via Paper's `PluginManager.registerEvents()`
- Custom events published by ServerCore managers:
  - `CosmeticApplyEvent`, `CosmeticRemoveEvent` - Cosmetic lifecycle
  - `PetSummonEvent`, `PetDismissEvent`, `PetStateChangeEvent` - Pet lifecycle
  - `HologramCreateEvent`, `HologramClickEvent` - Hologram interactions
  - `DialogueStartEvent` - NPC dialogue initiation
  - `QuestStartEvent`, `QuestCompleteEvent`, `QuestProgressEvent`, `QuestAbandonEvent` - Quest lifecycle
  - `MenuOpenEvent`, `MenuCloseEvent` - GUI menu lifecycle
  - `TimelinePlayEvent` - Timeline sequencer events
  - `ReactiveRuleTriggeredEvent` - Context-aware cosmetic triggers
- Listeners: Other plugins can hook via `@EventHandler` annotations

---

*Integration audit: 2026-03-21*
