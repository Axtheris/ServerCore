# Quest System Design

## Overview

A YAML-driven quest system integrated with the NPC dialogue system. Players accept quests from NPCs, complete objectives (fetch items, kill mobs, talk to NPCs), and return for rewards. Quests are purely server-side with chat-based UX through the existing dialogue system.

## Data Model

### Quest (definition, from YAML)
- `id` -- unique string identifier
- `displayName` -- MiniMessage-formatted name
- `description` -- short description text
- `acceptNpc` -- NPC id that offers the quest
- `turnInNpc` -- NPC id that accepts completion (can differ from acceptNpc)
- `objectives` -- ordered list of QuestObjective
- `rewards` -- list of QuestReward
- `repeatable` -- boolean, default false
- `cooldownSeconds` -- int, seconds before repeatable quest can be retaken (0 = immediate)

### QuestObjective (three types)
- **fetch** -- `material` (Material name) + `amount` (int). Items consumed on turn-in.
- **kill** -- `entityType` (EntityType name) + `amount` (int). Tracked via EntityDeathEvent.
- **talk** -- `npcId` (String). Marked complete on NPC interaction.

### QuestReward (three types)
- **item** -- `material` (Material name) + `amount` (int)
- **xp** -- `amount` (int), vanilla experience points
- **command** -- `value` (String), supports `%player%` placeholder

### QuestProgress (per-player per-quest runtime state)
- `questId` -- references Quest definition
- `playerUuid` -- owning player
- `objectiveProgress` -- int array parallel to quest's objective list (e.g. kills so far)
- `status` -- ACTIVE or COMPLETE (all objectives met, awaiting turn-in)

## Config Format

### Separate quest file (`quests/gather-wood.yml`)
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

### Inline in NPC YAML (`npcs/merchant.yml`)
```yaml
quest:
  id: gather-wood
  display-name: "<gold>Lumberjack's Request"
  description: "Gather 16 oak logs."
  objectives:
    - type: fetch
      material: OAK_LOG
      amount: 16
  rewards:
    - type: item
      material: DIAMOND
      amount: 3
```
When inline, `accept-npc` and `turn-in-npc` default to the parent NPC's id.

## Dialogue Integration

### New conditions
| Condition | True when |
|:----------|:----------|
| `quest_available` | Player hasn't accepted/completed the quest, or cooldown has expired |
| `quest_active` | Player has this quest in progress |
| `quest_complete` | Player has completed all objectives (ready for turn-in) |
| `quest_finished` | Player has already turned in this quest |

### New actions
| Action | Effect |
|:-------|:-------|
| `accept_quest` | Starts the quest for the player |
| `complete_quest` | Turns in the quest: gives rewards, consumes fetch items |

## Progress Tracking

- **Kill**: `EntityDeathEvent` listener checks killer's active quests for matching kill objectives, increments counter.
- **Fetch**: Checked on-demand when dialogue evaluates `quest_complete` condition. Scans player inventory for required materials and amounts.
- **Talk**: Marked complete when player interacts with target NPC while quest is active. Checked in `NPCListener.handleInteraction()`.
- **Persistence**: `quest-data.yml` (same pattern as `pet-data.yml`). Saved on plugin disable. Stores active quests with progress and completed quest ids with timestamps.

## Command

`/quest <active|completed|abandon|reload>` with `servercore.quest` permission.

| Subcommand | Description |
|:-----------|:------------|
| `active` | List player's active quests with objective progress |
| `completed` | List quests the player has finished |
| `abandon <id>` | Drop an active quest |
| `reload` | Reload quest definitions from YAML (op) |

## API

- `QuestManager` exposed via `ServerCoreAPI.getQuestManager()`
- `QuestStartEvent` -- fired when player accepts a quest, cancellable
- `QuestCompleteEvent` -- fired when player turns in a quest

## Package Structure

```
net.axther.serverCore.quest/
  Quest.java              -- quest definition
  QuestObjective.java     -- objective sealed interface + records
  QuestReward.java        -- reward sealed interface + records
  QuestProgress.java      -- per-player runtime state
  QuestManager.java       -- registry, progress tracking, accept/complete logic
  command/
    QuestCommand.java     -- /quest command
  config/
    QuestConfig.java      -- loads quests/*.yml + inline NPC quests
  data/
    QuestStore.java       -- persistence to quest-data.yml
  listener/
    QuestListener.java    -- EntityDeathEvent for kills, delegates talk tracking
api/event/
  QuestStartEvent.java
  QuestCompleteEvent.java
```

## Integration Points

- `DialogueCondition.fromConfig()` -- add quest_available, quest_active, quest_complete, quest_finished cases
- `DialogueAction.fromConfig()` -- add accept_quest, complete_quest cases
- `NPCListener.handleInteraction()` -- check talk objectives
- `NPCConfig.loadNPC()` -- load inline quest definitions
- `ServerCore.onEnable()` -- init quest system under npcs enabled check
- `ServerCoreAPI.init()` -- expose QuestManager
- `plugin.yml` -- register /quest command
- `config.yml` -- quest system inherits npcs enabled toggle
