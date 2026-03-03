package net.axther.serverCore.npc.dialogue.action;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

public class SendMessageAction implements DialogueAction {

    private final String message;

    public SendMessageAction(String message) {
        this.message = message;
    }

    @Override
    public void execute(Player player) {
        player.sendMessage(MiniMessage.miniMessage().deserialize(message));
    }
}
