package net.axther.serverCore.npc.dialogue.action;

import net.axther.serverCore.api.ServerCoreAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class AcceptQuestAction implements DialogueAction {
    private final String questId;
    public AcceptQuestAction(String questId) { this.questId = questId; }

    @Override
    public void execute(Player player) {
        var qm = ServerCoreAPI.get().getQuestManager();
        if (qm == null) return;

        if (qm.acceptQuest(player, questId)) {
            var quest = qm.getQuest(questId);
            String name = quest != null ? quest.getDisplayName() : questId;
            player.sendMessage(Component.text("Quest accepted: ", NamedTextColor.GREEN)
                    .append(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(name)));
        }
    }
}
