# Quest Parameters Expansion Design

## Summary

Expand the quest system's objective types, reward types, quest-level structure parameters, and progress display. All new features remain YAML-configurable. Vault integration is soft/optional.

## New Objective Types

| Type | YAML fields | Tracked via |
|------|------------|-------------|
| CRAFT | `material`, `amount` | CraftItemEvent |
| MINE | `material`, `amount` | BlockBreakEvent |
| PLACE | `material`, `amount` | BlockPlaceEvent |
| FISH | `material` (optional, default ANY), `amount` | PlayerFishEvent |
| BREED | `entity`, `amount` | EntityBreedEvent |
| SMELT | `material` (result item), `amount` | FurnaceExtractEvent |
| EXPLORE | `location` (world,x,y,z), `radius` | PlayerMoveEvent with distance threshold |
| INTERACT | `material` or `entity`, `amount` | PlayerInteractEvent / PlayerInteractEntityEvent |

Each objective supports an optional `description` field that overrides auto-generated display text.

## New Reward Types

| Type | YAML fields | Behavior |
|------|------------|---------|
| MONEY | `amount` | Vault economy deposit. Soft dependency: logs warning if Vault absent |
| PERMISSION | `value` (node), `duration` (seconds, 0 = permanent) | Vault Permissions. Timed perms revoked via scheduled task |
| PET | `value` (pet profile ID), `amount` | Gives pet item via PetProfile.createItem() |

Vault integration via a `VaultHook` utility class (same pattern as ModelEngineHook).

## Quest Structure Parameters

| Field | YAML key | Default | Behavior |
|-------|----------|---------|---------|
| Permission | `required-permission` | none | Must have perm to accept |
| Prerequisites | `prerequisites` (list) | empty | All must be completed first |
| Time limit | `time-limit` (seconds) | 0 (none) | Auto-abandon on expiry with player notification |
| Max active | `max-active-quests` in config.yml | 0 (unlimited) | Global cap on simultaneous active quests |
| Category | `category` | "general" | Grouping/filtering in /quest active |
| Sequential | `sequential-objectives` | false | Objectives must complete in order |

Time limits tracked via `startedAt` timestamp on QuestProgress. Checked in listener handlers.

Sequential objectives: listeners skip objectives past the first incomplete index.

Prerequisites checked in `canAccept()` alongside repeatable/cooldown.

## Display and Notifications

- Custom `description` per objective overrides auto-generated text
- Action bar progress notifications (global toggle `quest-action-bar` in config.yml)
- `/quest active` groups quests by category with header lines

## Files Modified

- `QuestObjective.java` — new enum values + fromConfig parsing
- `QuestReward.java` — new enum values + fromConfig parsing + give() logic
- `Quest.java` — new fields (prerequisites, required-permission, time-limit, category, sequential-objectives)
- `QuestProgress.java` — add startedAt timestamp
- `QuestManager.java` — new handler methods (handleCraft, handleMine, handlePlace, handleFish, handleBreed, handleSmelt, handleExplore, handleInteract), updated canAccept() for prerequisites/permissions/max-active, time limit expiry checks
- `QuestListener.java` — register new event listeners
- `QuestConfig.java` — parse new YAML fields
- `QuestCommand.java` — category grouping in active display
- New: `VaultHook.java` — soft Vault economy/permissions integration
- `config.yml` — add max-active-quests, quest-action-bar toggle
- `ServerCore.java` — init VaultHook, pass to QuestManager
