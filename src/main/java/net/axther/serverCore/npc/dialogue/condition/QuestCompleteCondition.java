package net.axther.serverCore.npc.dialogue.condition;

import net.axther.serverCore.api.ServerCoreAPI;
import org.bukkit.entity.Player;

public class QuestCompleteCondition implements DialogueCondition {
    private final String questId;
    public QuestCompleteCondition(String questId) { this.questId = questId; }

    @Override
    public boolean test(Player player) {
        var qm = ServerCoreAPI.get().getQuestManager();
        return qm != null && qm.areObjectivesComplete(player, questId);
    }
}
