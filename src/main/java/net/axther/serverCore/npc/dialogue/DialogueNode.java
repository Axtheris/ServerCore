package net.axther.serverCore.npc.dialogue;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DialogueNode {

    private final String id;
    private final List<String> text;
    private final List<DialogueChoice> choices;

    public DialogueNode(String id, List<String> text, List<DialogueChoice> choices) {
        this.id = id;
        this.text = text;
        this.choices = choices;
    }

    @SuppressWarnings("unchecked")
    public static DialogueNode fromConfig(String id, ConfigurationSection section) {
        List<String> text = section.getStringList("text");

        List<DialogueChoice> choices = new ArrayList<>();
        List<?> choiceList = section.getList("choices");
        if (choiceList != null) {
            for (Object obj : choiceList) {
                if (obj instanceof Map<?, ?> map) {
                    ConfigurationSection choiceSec = section.createSection("_choice_" + System.nanoTime());
                    for (Map.Entry<String, Object> entry : ((Map<String, Object>) map).entrySet()) {
                        choiceSec.set(entry.getKey(), entry.getValue());
                    }
                    choices.add(DialogueChoice.fromConfig(choiceSec));
                }
            }
        }

        return new DialogueNode(id, text, choices);
    }

    public String getId() { return id; }
    public List<String> getText() { return text; }
    public List<DialogueChoice> getChoices() { return choices; }
}
