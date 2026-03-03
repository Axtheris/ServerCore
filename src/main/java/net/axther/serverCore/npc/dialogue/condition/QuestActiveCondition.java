package net.axther.serverCore.npc.dialogue.condition;

import net.axther.serverCore.api.ServerCoreAPI;
import org.bukkit.entity.Player;

public class QuestActiveCondition implements DialogueCondition {
    private final String questId;
    public QuestActiveCondition(String questId) { this.questId = questId; }

    @Override
    public boolean test(Player player) {
        var qm = ServerCoreAPI.get().getQuestManager();
        return qm != null && qm.isActive(player.getUniqueId(), questId);
    }
}
