package net.axther.serverCore.quest;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

public class QuestObjective {

    public enum Type { FETCH, KILL, TALK }

    private final Type type;
    private final String target; // material name, entity type name, or NPC id
    private final int amount;    // 1 for talk objectives

    public QuestObjective(Type type, String target, int amount) {
        this.type = type;
        this.target = target;
        this.amount = amount;
    }

    public static QuestObjective fromConfig(ConfigurationSection section) {
        String typeStr = section.getString("type", "fetch");
        return switch (typeStr.toLowerCase()) {
            case "kill" -> new QuestObjective(Type.KILL,
                    section.getString("entity", "ZOMBIE"), section.getInt("amount", 1));
            case "talk" -> new QuestObjective(Type.TALK,
                    section.getString("npc", ""), 1);
            default -> new QuestObjective(Type.FETCH,
                    section.getString("material", "DIRT"), section.getInt("amount", 1));
        };
    }

    public Type getType() { return type; }
    public String getTarget() { return target; }
    public int getAmount() { return amount; }
}
