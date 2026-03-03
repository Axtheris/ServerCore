package net.axther.serverCore.npc.config;

import net.axther.serverCore.npc.NPC;
import net.axther.serverCore.npc.NPCManager;
import net.axther.serverCore.npc.dialogue.DialogueTree;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class NPCConfig {

    private final JavaPlugin plugin;
    private final File npcsDir;
    private final Map<String, DialogueTree> dialogueTrees = new HashMap<>();

    public NPCConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.npcsDir = new File(plugin.getDataFolder(), "npcs");
    }

    public void loadAll(NPCManager manager) {
        if (!npcsDir.exists()) {
            npcsDir.mkdirs();
            saveDefaultExample();
            return;
        }

        File[] files = npcsDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        dialogueTrees.clear();

        for (File file : files) {
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                loadNPC(yaml, manager);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load NPC from " + file.getName(), e);
            }
        }

        plugin.getLogger().info("Loaded " + manager.getAll().size() + " NPCs");
    }

    private void loadNPC(YamlConfiguration yaml, NPCManager manager) {
        String id = yaml.getString("id");
        if (id == null || id.isBlank()) return;

        String displayName = yaml.getString("display-name", "<white>" + id);
        String skinTexture = yaml.getString("skin-texture");
        String skinSignature = yaml.getString("skin-signature");
        String worldName = yaml.getString("world", "world");
        double x = yaml.getDouble("x");
        double y = yaml.getDouble("y");
        double z = yaml.getDouble("z");
        float yaw = (float) yaml.getDouble("yaw", 0.0);
        boolean lookAtPlayer = yaml.getBoolean("look-at-player", false);

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("NPC '" + id + "' references unknown world '" + worldName + "', skipping");
            return;
        }

        Location location = new Location(world, x, y, z, yaw, 0);

        // Load inline dialogue tree if present
        String dialogueId = null;
        ConfigurationSection dialogueSec = yaml.getConfigurationSection("dialogue");
        if (dialogueSec != null) {
            dialogueId = id;
            DialogueTree tree = DialogueTree.fromConfig(id, dialogueSec);
            dialogueTrees.put(id, tree);
        }

        NPC npc = new NPC(id, displayName, location, yaw, skinTexture, skinSignature, lookAtPlayer, dialogueId);
        manager.register(npc);
    }

    public void save(NPC npc) {
        File file = new File(npcsDir, npc.getId() + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();

        yaml.set("id", npc.getId());
        yaml.set("display-name", npc.getDisplayName());
        if (npc.getSkinTexture() != null) {
            yaml.set("skin-texture", npc.getSkinTexture());
        }
        if (npc.getSkinSignature() != null) {
            yaml.set("skin-signature", npc.getSkinSignature());
        }
        yaml.set("world", npc.getWorldName());

        Location loc = npc.getLocation();
        yaml.set("x", loc.getX());
        yaml.set("y", loc.getY());
        yaml.set("z", loc.getZ());
        yaml.set("yaw", (double) npc.getYaw());
        yaml.set("look-at-player", npc.isLookAtPlayer());

        // Save dialogue tree if it exists
        DialogueTree tree = dialogueTrees.get(npc.getDialogueId());
        if (tree != null) {
            saveDialogueTree(yaml, tree);
        }

        try {
            npcsDir.mkdirs();
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save NPC '" + npc.getId() + "'", e);
        }
    }

    private void saveDialogueTree(YamlConfiguration yaml, DialogueTree tree) {
        ConfigurationSection dialogueSec = yaml.createSection("dialogue");
        for (var entry : tree.getNodes().entrySet()) {
            var node = entry.getValue();
            ConfigurationSection nodeSec = dialogueSec.createSection(entry.getKey());
            nodeSec.set("text", node.getText());

            if (!node.getChoices().isEmpty()) {
                var choicesList = new java.util.ArrayList<Map<String, Object>>();
                for (var choice : node.getChoices()) {
                    Map<String, Object> choiceMap = new java.util.LinkedHashMap<>();
                    choiceMap.put("label", choice.getLabel());
                    if (choice.getNextNodeId() != null) {
                        choiceMap.put("next", choice.getNextNodeId());
                    }
                    choicesList.add(choiceMap);
                }
                nodeSec.set("choices", choicesList);
            }
        }
    }

    public void deleteFile(String id) {
        File file = new File(npcsDir, id + ".yml");
        if (file.exists()) {
            file.delete();
        }
        dialogueTrees.remove(id);
    }

    public DialogueTree getDialogueTree(String id) {
        return dialogueTrees.get(id);
    }

    private void saveDefaultExample() {
        File file = new File(npcsDir, "merchant.yml");
        YamlConfiguration yaml = new YamlConfiguration();

        yaml.set("id", "merchant");
        yaml.set("display-name", "<gold>Merchant Bob");
        yaml.set("world", "world");
        yaml.set("x", 0.5);
        yaml.set("y", 65.0);
        yaml.set("z", 0.5);
        yaml.set("yaw", 180.0);
        yaml.set("look-at-player", true);

        ConfigurationSection dialogue = yaml.createSection("dialogue");

        // start node
        ConfigurationSection startNode = dialogue.createSection("start");
        startNode.set("text", java.util.List.of("<gold>Welcome, traveler!"));

        var startChoices = new java.util.ArrayList<Map<String, Object>>();

        Map<String, Object> choice1 = new java.util.LinkedHashMap<>();
        choice1.put("label", "<yellow>Tell me about this place");
        choice1.put("next", "lore");
        var conds = new java.util.ArrayList<Map<String, Object>>();
        Map<String, Object> cond = new java.util.LinkedHashMap<>();
        cond.put("type", "permission");
        cond.put("value", "server.lore");
        conds.add(cond);
        choice1.put("conditions", conds);
        startChoices.add(choice1);

        Map<String, Object> choice2 = new java.util.LinkedHashMap<>();
        choice2.put("label", "<red>Goodbye");
        var actions = new java.util.ArrayList<Map<String, Object>>();
        Map<String, Object> action = new java.util.LinkedHashMap<>();
        action.put("type", "message");
        action.put("value", "<gold>Safe travels.");
        actions.add(action);
        choice2.put("actions", actions);
        startChoices.add(choice2);

        startNode.set("choices", startChoices);

        // lore node
        ConfigurationSection loreNode = dialogue.createSection("lore");
        loreNode.set("text", java.util.List.of("<gray>This town has a long history..."));

        var loreChoices = new java.util.ArrayList<Map<String, Object>>();
        Map<String, Object> loreChoice = new java.util.LinkedHashMap<>();
        loreChoice.put("label", "<yellow>Thanks");
        loreChoice.put("next", "start");
        loreChoices.add(loreChoice);
        loreNode.set("choices", loreChoices);

        try {
            npcsDir.mkdirs();
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save default NPC example", e);
        }
    }
}
