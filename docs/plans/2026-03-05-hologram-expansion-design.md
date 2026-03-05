# Hologram System Expansion Design

## Summary

Expand the hologram system with dynamic PlaceholderAPI content, conditional per-player visibility, click actions, NPC dialogue holograms, and visual styling options. All YAML-configurable.

## Dynamic Content (PlaceholderAPI)

Hologram lines can contain PlaceholderAPI placeholders. A configurable `update-interval` (ticks, default 20) controls refresh rate. Server-wide placeholders work on all holograms. Player-specific placeholders resolve per viewer via per-player metadata packets during visibility updates.

## Conditional Visibility

Holograms can define a `conditions` list. When present, only players meeting all conditions see the hologram. Uses Paper's `player.hideEntity()` / `player.showEntity()` — no packet manipulation needed.

Condition types: `permission`, `quest_active`, `quest_complete`, `placeholder` (with equals/min/max), `world`.

A `HologramVisibilityTracker` re-evaluates conditions per nearby player on the update interval (within configurable `view-distance`, default 48 blocks).

## Click/Interact Actions

Optional `actions` list triggers on `PlayerInteractAtEntityEvent`. Respects a `click-cooldown` (ticks, default 20). Hidden holograms cannot be interacted with.

Action types: `command` (console), `player_command`, `message` (MiniMessage), `sound`, `menu` (GUI system), `dialogue` (NPC system).

## NPC Dialogue Holograms

NPCs with `dialogue-hologram: true` display dialogue text as a temporary TextDisplay above their head, visible only to the conversing player. Spawns on dialogue node fire, despawns on node change or conversation end. Transient — not persisted to holograms.yml.

Config per NPC: `dialogue-hologram` (bool), `dialogue-hologram-offset` (blocks above NPC, default 0.5).

Implemented via a `DialogueHologram` class created/destroyed by the NPC dialogue system.

## Visual Options

New TextDisplay styling fields, all mapping directly to Paper's TextDisplay API:

- `background` — ARGB hex string (e.g. "#80000000")
- `text-shadow` — boolean (default false)
- `billboard` — CENTER, FIXED, HORIZONTAL, VERTICAL (default CENTER)
- `line-width` — max pixel width (default 200)
- `see-through` — boolean (default false)
- `alignment` — LEFT, CENTER, RIGHT (default CENTER)
- `view-range` — float multiplier (default 1.0)

## Files Modified/Created

- `Hologram.java` — new fields (visual options, update-interval, conditions, actions, click-cooldown), spawn() applies visual settings, tick() refreshes placeholder text
- `HologramAnimation.java` — unchanged
- `HologramManager.java` — visibility tracking integration
- `HologramConfig.java` — parse new YAML fields
- `HologramCommand.java` — potential new subcommands for visual settings
- `HologramLifecycleListener.java` — unchanged
- `HologramTickTask.java` — pass player context for visibility updates
- New: `HologramVisibilityTracker.java` — per-player visibility evaluation
- New: `HologramCondition.java` — condition type enum and evaluation
- New: `HologramAction.java` — action type enum and execution
- New: `HologramInteractListener.java` — click handler with cooldown
- New: `DialogueHologram.java` — transient per-player dialogue display
- Modified: NPC dialogue system files — spawn/despawn DialogueHologram on node changes
- `holograms.yml` — updated default example
