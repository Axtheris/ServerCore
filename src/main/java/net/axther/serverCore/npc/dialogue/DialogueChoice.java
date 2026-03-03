package net.axther.serverCore.npc.dialogue;

import net.axther.serverCore.npc.dialogue.action.DialogueAction;
import net.axther.serverCore.npc.dialogue.condition.DialogueCondition;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DialogueChoice {

    private final String label;
    private final String nextNodeId;
    private final List<DialogueCondition> conditions;
    private final List<DialogueAction> actions;

    public DialogueChoice(String label, String nextNodeId,
                          List<DialogueCondition> conditions, List<DialogueAction> actions) {
        this.label = label;
        this.nextNodeId = nextNodeId;
        this.conditions = conditions;
        this.actions = actions;
    }

    @SuppressWarnings("unchecked")
    public static DialogueChoice fromConfig(ConfigurationSection section) {
        String label = section.getString("label", "<gray>...");
        String nextNodeId = section.getString("next", null);

        List<DialogueCondition> conditions = new ArrayList<>();
        List<?> condList = section.getList("conditions");
        if (condList != null) {
            for (Object obj : condList) {
                if (obj instanceof Map<?, ?> map) {
                    ConfigurationSection condSec = toSection(section, (Map<String, Object>) map);
                    conditions.add(DialogueCondition.fromConfig(condSec));
                }
            }
        }

        List<DialogueAction> actions = new ArrayList<>();
        List<?> actList = section.getList("actions");
        if (actList != null) {
            for (Object obj : actList) {
                if (obj instanceof Map<?, ?> map) {
                    ConfigurationSection actSec = toSection(section, (Map<String, Object>) map);
                    actions.add(DialogueAction.fromConfig(actSec));
                }
            }
        }

        return new DialogueChoice(label, nextNodeId, conditions, actions);
    }

    private static ConfigurationSection toSection(ConfigurationSection parent, Map<String, Object> map) {
        ConfigurationSection sec = parent.createSection("_temp_" + System.nanoTime());
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            sec.set(entry.getKey(), entry.getValue());
        }
        return sec;
    }

    public String getLabel() { return label; }
    public String getNextNodeId() { return nextNodeId; }
    public List<DialogueCondition> getConditions() { return conditions; }
    public List<DialogueAction> getActions() { return actions; }
}
