package net.axther.serverCore.npc.dialogue.condition;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public interface DialogueCondition {

    boolean test(Player player);

    static DialogueCondition fromConfig(ConfigurationSection section) {
        String type = section.getString("type", "");
        String value = section.getString("value", "");

        return switch (type.toLowerCase()) {
            case "permission" -> new HasPermissionCondition(value);
            case "item" -> new HasItemCondition(value);
            case "quest_available" -> new QuestAvailableCondition(value);
            case "quest_active" -> new QuestActiveCondition(value);
            case "quest_complete" -> new QuestCompleteCondition(value);
            case "quest_finished" -> new QuestFinishedCondition(value);
            default -> player -> true;
        };
    }
}
