package net.axther.serverCore.npc.dialogue.action;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public interface DialogueAction {

    void execute(Player player);

    static DialogueAction fromConfig(ConfigurationSection section) {
        String type = section.getString("type", "");
        String value = section.getString("value", "");

        return switch (type.toLowerCase()) {
            case "command" -> new RunCommandAction(value);
            case "message" -> new SendMessageAction(value);
            case "give_item" -> new GiveItemAction(value);
            case "sound" -> new PlaySoundAction(value);
            default -> player -> {};
        };
    }
}
