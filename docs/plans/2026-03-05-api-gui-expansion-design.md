# API & GUI Framework Expansion Design

## Summary

Expand the GUI framework with YAML-driven custom menus, dynamic/animated items, and built-in system GUIs. Expand the API with fluent builders, new events, and convenience methods. Three layers: GUI core improvements, system GUIs, then API expansion.

## Layer 1: GUI Core Improvements

### YAML-Driven Custom Menus

MenuConfig already loads layouts from menus.yml but has no action binding. Expand so operators can define fully functional menus without code.

New YAML fields per item: `actions` (list of click actions), `right-actions` (right-click actions), `view-condition` (per-player visibility).

Action types: `command` (console), `player_command`, `message` (MiniMessage), `sound`, `close`, `open_menu` (opens another YAML menu by ID), `back` (return to parent).

View conditions: `permission:<node>`, `placeholder:<papi>:<operator>:<value>`.

Item names and lore support PlaceholderAPI placeholders resolved per-player at open time.

New command: `/menu <id>` opens any YAML-defined menu. Permission: `servercore.menu.<id>`.

Optional `open-sound` per menu.

### Dynamic Items and Auto-Refresh

`dynamic: true` flag on items triggers PlaceholderAPI re-resolution on refresh.

`refresh-interval: <ticks>` on a menu triggers periodic re-render of dynamic items. Default: no refresh (static). Tracked by MenuManager.

Animated items: `cycle` list of materials with `cycle-interval` ticks per frame. Rotates display material.

MenuItem gains: `dynamic` (boolean), `cycleItems` (List<Material>), `cycleInterval` (int).

MenuManager runs a tick method updating open menus needing refresh or cycling.

## Layer 2: Built-in System GUIs

### Quest Journal (`/quest gui`)

PaginatedMenu showing active quests grouped by category. Each item: display name, description lore, objective progress bars, time remaining. Left-click shows detail. Right-click abandons with ConfirmationMenu. Tab row switches Active/Completed views.

### Hologram Manager (`/hologram gui`)

PaginatedMenu listing all holograms. Shows ID, world/coords, animation, line count, spawned status. Left-click teleports. Right-click deletes with confirmation. Permission: `servercore.hologram.list`.

### NPC Browser (`/npc gui`)

PaginatedMenu listing NPCs. Shows name, location, dialogue status, skin. Left-click teleports. Permission: `servercore.npc.list`.

All register as `gui` subcommands of existing commands.

## Layer 3: API Expansion

### Fluent Builders

Factory methods on ServerCoreAPI returning builder objects:

- `api.hologram("id")` -> HologramBuilder (at, lines, billboard, animation, condition, action, spawn)
- `api.menu("title")` -> wraps existing Menu.builder(), exposed on API
- `api.emitter("id")` -> EmitterBuilder (at, particle, pattern, radius, spawn)

Builders collect parameters, delegate to managers.

### New Events

| Event | Cancellable | Fields |
|-------|:-----------:|--------|
| HologramClickEvent | Yes | player, hologramId, actions |
| QuestProgressEvent | No | player, questId, objectiveIndex, newProgress |
| QuestAbandonEvent | Yes | player, questId |
| PetStateChangeEvent | No | player, petProfile, oldState, newState |
| MenuOpenEvent | Yes | player, menuId |
| MenuCloseEvent | No | player, menuId |
| ReactiveRuleTriggeredEvent | No | player, ruleId, effects |

Fire from existing code paths.

### Convenience Methods

```
api.openQuestJournal(player)
api.isQuestActive(player, questId)
api.getQuestProgress(player, questId)
api.showHologramTo(player, hologramId)
api.hideHologramFrom(player, hologramId)
api.openMenu(player, menuId)
api.hasPetSummoned(player)
api.getPetName(player)
```

Delegate to underlying managers.

## Files Modified/Created

### Layer 1
- Modify: `MenuConfig.java` — parse actions, right-actions, view-condition, dynamic, cycle, refresh-interval
- Modify: `MenuItem.java` — add dynamic flag, cycleItems, cycleInterval fields
- Modify: `Menu.java` — add refreshInterval, openSound, support dynamic refresh
- Modify: `MenuManager.java` — tick method for refresh/cycling
- Modify: `MenuListener.java` — fire MenuOpenEvent/MenuCloseEvent
- New: `MenuAction.java` — action type enum and execution (command, player_command, message, sound, close, open_menu, back)
- New: `MenuCommand.java` — `/menu <id>` command
- New: `MenuTickTask.java` — tick task for auto-refresh and item cycling

### Layer 2
- New: `QuestGUI.java` — quest journal GUI
- New: `HologramGUI.java` — hologram manager GUI
- New: `NpcGUI.java` — NPC browser GUI
- Modify: `QuestCommand.java` — add `gui` subcommand
- Modify: `HologramCommand.java` — add `gui` subcommand
- Modify: `NPCCommand.java` — add `gui` subcommand

### Layer 3
- Modify: `ServerCoreAPI.java` — add builders, convenience methods
- New: `HologramBuilder.java` — fluent hologram builder
- New: `EmitterBuilder.java` — fluent emitter builder
- New: `HologramClickEvent.java`
- New: `QuestProgressEvent.java`
- New: `QuestAbandonEvent.java`
- New: `PetStateChangeEvent.java`
- New: `MenuOpenEvent.java`
- New: `MenuCloseEvent.java`
- New: `ReactiveRuleTriggeredEvent.java`
- Modify: `HologramInteractListener.java` — fire HologramClickEvent
- Modify: `QuestManager.java` — fire QuestProgressEvent, QuestAbandonEvent
- Modify: `ReactiveTickTask.java` — fire ReactiveRuleTriggeredEvent
