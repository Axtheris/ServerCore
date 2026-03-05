package net.axther.serverCore.quest;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

public class QuestObjective {

    public enum Type { FETCH, KILL, TALK, CRAFT, MINE, PLACE, FISH, BREED, SMELT, EXPLORE, INTERACT }

    private final Type type;
    private final String target; // material name, entity type name, or NPC id
    private final int amount;    // 1 for talk objectives
    private final String description; // nullable, custom display text
    private final double radius;      // for EXPLORE objectives, default 50.0

    public QuestObjective(Type type, String target, int amount) {
        this(type, target, amount, null, 50.0);
    }

    public QuestObjective(Type type, String target, int amount, String description) {
        this(type, target, amount, description, 50.0);
    }

    public QuestObjective(Type type, String target, int amount, String description, double radius) {
        this.type = type;
        this.target = target;
        this.amount = amount;
        this.description = description;
        this.radius = radius;
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
            case "explore" -> new QuestObjective(Type.EXPLORE,
                    section.getString("location", "world,0,64,0"), 1, description,
                    section.getDouble("radius", 50.0));
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
}
