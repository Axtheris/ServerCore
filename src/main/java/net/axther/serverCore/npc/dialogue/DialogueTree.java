package net.axther.serverCore.npc.dialogue;

import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Map;

public class DialogueTree {

    private final String id;
    private final Map<String, DialogueNode> nodes;

    public DialogueTree(String id, Map<String, DialogueNode> nodes) {
        this.id = id;
        this.nodes = nodes;
    }

    public static DialogueTree fromConfig(String id, ConfigurationSection section) {
        Map<String, DialogueNode> nodes = new LinkedHashMap<>();

        for (String nodeId : section.getKeys(false)) {
            ConfigurationSection nodeSec = section.getConfigurationSection(nodeId);
            if (nodeSec != null) {
                nodes.put(nodeId, DialogueNode.fromConfig(nodeId, nodeSec));
            }
        }

        return new DialogueTree(id, nodes);
    }

    public String getId() { return id; }

    public DialogueNode getStartNode() {
        return nodes.get("start");
    }

    public DialogueNode getNode(String id) {
        return nodes.get(id);
    }

    public Map<String, DialogueNode> getNodes() { return nodes; }
}
