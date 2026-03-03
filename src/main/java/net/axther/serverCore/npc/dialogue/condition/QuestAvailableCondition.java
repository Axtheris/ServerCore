package net.axther.serverCore.npc.dialogue.condition;

import net.axther.serverCore.api.ServerCoreAPI;
import org.bukkit.entity.Player;

public class QuestAvailableCondition implements DialogueCondition {
    private final String questId;
    public QuestAvailableCondition(String questId) { this.questId = questId; }

    @Override
    public boolean test(Player player) {
        var qm = ServerCoreAPI.get().getQuestManager();
        return qm != null && qm.canAccept(player, questId);
    }
}
