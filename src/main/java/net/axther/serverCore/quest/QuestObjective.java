package net.axther.serverCore.quest;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

public class QuestObjective {

    public enum Type { FETCH, KILL, TALK, CRAFT, MINE, PLACE, FISH, BREED, SMELT, EXPLORE, INTERACT }

    /**
     * PERF-01: Pre-parsed explore target coordinates. Immutable record created at config load time.
     * Stores world NAME (not World reference) because worlds may not be loaded at config parse time (D-19).
     */
    public record ExploreTarget(String worldName, double x, double y, double z) {
        public ExploreTarget {
            if (worldName == null || worldName.isEmpty()) {
                throw new IllegalArgumentException("ExploreTarget worldName must not be empty");
            }
        }
    }

    private final Type type;
    private final String target; // material name, entity type name, or NPC id
    private final int amount;    // 1 for talk objectives
    private final String description; // nullable, custom display text
    private final double radius;      // for EXPLORE objectives, default 50.0
    private final ExploreTarget exploreTarget; // null for non-EXPLORE objectives

    public QuestObjective(Type type, String target, int amount) {
        this(type, target, amount, null, 50.0, null);
    }

    public QuestObjective(Type type, String target, int amount, String description) {
        this(type, target, amount, description, 50.0, null);
    }

    public QuestObjective(Type type, String target, int amount, String description, double radius) {
        this(type, target, amount, description, radius, null);
    }

    public QuestObjective(Type type, String target, int amount, String description, double radius, ExploreTarget exploreTarget) {
        this.type = type;
        this.target = target;
        this.amount = amount;
        this.description = description;
        this.radius = radius;
        this.exploreTarget = exploreTarget;
    }

    /**
     * Parses a location string of the form "worldName,x,y,z" into an ExploreTarget.
     * Returns null if the string is null, malformed, or has a non-numeric coordinate.
     * Callers should log a warning if null is returned for an EXPLORE objective (D-20).
     */
    private static ExploreTarget parseExploreTarget(String locationStr) {
        if (locationStr == null) return null;
        String[] parts = locationStr.split(",");
        if (parts.length != 4) return null;
        try {
            return new ExploreTarget(
                parts[0].trim(),
                Double.parseDouble(parts[1].trim()),
                Double.parseDouble(parts[2].trim()),
                Double.parseDouble(parts[3].trim())
            );
        } catch (IllegalArgumentException e) {
            return null; // caller logs warning (D-20); catches NumberFormatException and empty worldName
        }
    }

    public static QuestObjective fromConfig(ConfigurationSection section) {
        String typeStr = section.getString("type", "fetch");
        String description = section.getString("description", null);
        return switch (typeStr.toLowerCase()) {
            case "kill" -> new QuestObjective(Type.KILL,
                    section.getString("entity", "ZOMBIE"), section.getInt("amount", 1), description);
            case "talk" -> new QuestObjective(Type.TALK,
                    section.getString("npc", ""), 1, description);
            case "craft" -> new QuestObjective(Type.CRAFT,
                    section.getString("material", "DIRT"), section.getInt("amount", 1), description);
            case "mine" -> new QuestObjective(Type.MINE,
                    section.getString("material", "DIRT"), section.getInt("amount", 1), description);
            case "place" -> new QuestObjective(Type.PLACE,
                    section.getString("material", "DIRT"), section.getInt("amount", 1), description);
            case "fish" -> new QuestObjective(Type.FISH,
                    section.getString("material", "ANY"), section.getInt("amount", 1), description);
            case "breed" -> new QuestObjective(Type.BREED,
                    section.getString("entity", "COW"), section.getInt("amount", 1), description);
            case "smelt" -> new QuestObjective(Type.SMELT,
                    section.getString("material", "DIRT"), section.getInt("amount", 1), description);
            case "explore" -> {
                String locationStr = section.getString("location", "world,0,64,0");
                double radius = section.getDouble("radius", 50.0);
                ExploreTarget parsed = parseExploreTarget(locationStr);
                // D-20: Malformed targets return null. Caller (Quest.fromConfig/QuestConfig) should
                // check and log if needed. The objective is still created with exploreTarget=null
                // so the quest structure is preserved but handleExplore() will skip it.
                yield new QuestObjective(Type.EXPLORE, locationStr, 1, description, radius, parsed);
            }
            case "interact" -> new QuestObjective(Type.INTERACT,
                    section.getString("material", section.getString("entity", "DIRT")),
                    section.getInt("amount", 1), description);
            default -> new QuestObjective(Type.FETCH,
                    section.getString("material", "DIRT"), section.getInt("amount", 1), description);
        };
    }

    public Type getType() { return type; }
    public String getTarget() { return target; }
    public int getAmount() { return amount; }
    public String getDescription() { return description; }
    public double getRadius() { return radius; }
    public ExploreTarget getExploreTarget() { return exploreTarget; }
}
