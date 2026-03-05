package net.axther.serverCore.npc.config;

import net.axther.serverCore.npc.NPC;
import net.axther.serverCore.npc.NPCManager;
import net.axther.serverCore.npc.dialogue.DialogueTree;
import net.axther.serverCore.quest.Quest;
import net.axther.serverCore.quest.QuestManager;
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
    private QuestManager questManager;

    public NPCConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.npcsDir = new File(plugin.getDataFolder(), "npcs");
    }

    public void loadAll(NPCManager manager, QuestManager questManager) {
        this.questManager = questManager;
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

        // Load inline quest if present
        ConfigurationSection questSec = yaml.getConfigurationSection("quest");
        if (questSec != null && questManager != null) {
            Quest quest = Quest.fromConfig(questSec, id);
            if (quest.getId() != null && !quest.getId().isBlank()) {
                questManager.registerQuest(quest);
            }
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

        // Inline quest definition
        ConfigurationSection quest = yaml.createSection("quest");
        quest.set("id", "gather-wood");
        quest.set("display-name", "<gold>Lumberjack's Request");
        quest.set("description", "Gather 16 oak logs for the merchant.");
        quest.set("repeatable", true);
        quest.set("cooldown", 3600);

        var objectives = new java.util.ArrayList<Map<String, Object>>();
        Map<String, Object> fetchObj = new java.util.LinkedHashMap<>();
        fetchObj.put("type", "fetch");
        fetchObj.put("material", "OAK_LOG");
        fetchObj.put("amount", 16);
        objectives.add(fetchObj);
        quest.set("objectives", objectives);

        var rewards = new java.util.ArrayList<Map<String, Object>>();
        Map<String, Object> itemReward = new java.util.LinkedHashMap<>();
        itemReward.put("type", "item");
        itemReward.put("material", "EMERALD");
        itemReward.put("amount", 5);
        rewards.add(itemReward);
        Map<String, Object> xpReward = new java.util.LinkedHashMap<>();
        xpReward.put("type", "xp");
        xpReward.put("amount", 100);
        rewards.add(xpReward);
        quest.set("rewards", rewards);

        // Dialogue tree with quest integration
        ConfigurationSection dialogue = yaml.createSection("dialogue");

        // start node
        ConfigurationSection startNode = dialogue.createSection("start");
        startNode.set("text", java.util.List.of("<gold>Welcome, traveler!"));

        var startChoices = new java.util.ArrayList<Map<String, Object>>();

        // Quest offer choice (only shown when quest is available)
        Map<String, Object> questOffer = new java.util.LinkedHashMap<>();
        questOffer.put("label", "<yellow>Any work available?");
        questOffer.put("next", "quest-offer");
        var questAvailConds = new java.util.ArrayList<Map<String, Object>>();
        Map<String, Object> questAvailCond = new java.util.LinkedHashMap<>();
        questAvailCond.put("type", "quest_available");
        questAvailCond.put("value", "gather-wood");
        questAvailConds.add(questAvailCond);
        questOffer.put("conditions", questAvailConds);
        startChoices.add(questOffer);

        // Quest turn-in choice (only shown when objectives are complete)
        Map<String, Object> questTurnIn = new java.util.LinkedHashMap<>();
        questTurnIn.put("label", "<green>I have the logs!");
        questTurnIn.put("next", "quest-complete");
        var questCompConds = new java.util.ArrayList<Map<String, Object>>();
        Map<String, Object> questCompCond = new java.util.LinkedHashMap<>();
        questCompCond.put("type", "quest_complete");
        questCompCond.put("value", "gather-wood");
        questCompConds.add(questCompCond);
        questTurnIn.put("conditions", questCompConds);
        startChoices.add(questTurnIn);

        // Quest in-progress reminder
        Map<String, Object> questReminder = new java.util.LinkedHashMap<>();
        questReminder.put("label", "<gray>About my task...");
        questReminder.put("next", "quest-progress");
        var questActiveConds = new java.util.ArrayList<Map<String, Object>>();
        Map<String, Object> questActiveCond = new java.util.LinkedHashMap<>();
        questActiveCond.put("type", "quest_active");
        questActiveCond.put("value", "gather-wood");
        questActiveConds.add(questActiveCond);
        questReminder.put("conditions", questActiveConds);
        startChoices.add(questReminder);

        Map<String, Object> goodbyeChoice = new java.util.LinkedHashMap<>();
        goodbyeChoice.put("label", "<red>Goodbye");
        var goodbyeActions = new java.util.ArrayList<Map<String, Object>>();
        Map<String, Object> goodbyeAction = new java.util.LinkedHashMap<>();
        goodbyeAction.put("type", "message");
        goodbyeAction.put("value", "<gold>Safe travels.");
        goodbyeActions.add(goodbyeAction);
        goodbyeChoice.put("actions", goodbyeActions);
        startChoices.add(goodbyeChoice);

        startNode.set("choices", startChoices);

        // quest-offer node
        ConfigurationSection offerNode = dialogue.createSection("quest-offer");
        offerNode.set("text", java.util.List.of(
                "<gold>I need 16 oak logs for my shop.",
                "<gold>Bring them back and I'll pay you well."));

        var offerChoices = new java.util.ArrayList<Map<String, Object>>();
        Map<String, Object> acceptChoice = new java.util.LinkedHashMap<>();
        acceptChoice.put("label", "<green>I'll get them for you!");
        var acceptActions = new java.util.ArrayList<Map<String, Object>>();
        Map<String, Object> acceptAction = new java.util.LinkedHashMap<>();
        acceptAction.put("type", "accept_quest");
        acceptAction.put("value", "gather-wood");
        acceptActions.add(acceptAction);
        acceptChoice.put("actions", acceptActions);
        offerChoices.add(acceptChoice);

        Map<String, Object> declineChoice = new java.util.LinkedHashMap<>();
        declineChoice.put("label", "<red>Not right now");
        declineChoice.put("next", "start");
        offerChoices.add(declineChoice);

        offerNode.set("choices", offerChoices);

        // quest-progress node
        ConfigurationSection progressNode = dialogue.createSection("quest-progress");
        progressNode.set("text", java.util.List.of("<gold>Still working on those logs? I need 16 oak logs."));

        var progressChoices = new java.util.ArrayList<Map<String, Object>>();
        Map<String, Object> progressBack = new java.util.LinkedHashMap<>();
        progressBack.put("label", "<yellow>I'm on it!");
        progressChoices.add(progressBack);
        progressNode.set("choices", progressChoices);

        // quest-complete node
        ConfigurationSection completeNode = dialogue.createSection("quest-complete");
        completeNode.set("text", java.util.List.of(
                "<gold>Excellent work! Here's your reward."));

        var completeChoices = new java.util.ArrayList<Map<String, Object>>();
        Map<String, Object> turnInChoice = new java.util.LinkedHashMap<>();
        turnInChoice.put("label", "<green>Thanks!");
        var completeActions = new java.util.ArrayList<Map<String, Object>>();
        Map<String, Object> completeAction = new java.util.LinkedHashMap<>();
        completeAction.put("type", "complete_quest");
        completeAction.put("value", "gather-wood");
        completeActions.add(completeAction);
        turnInChoice.put("actions", completeActions);
        completeChoices.add(turnInChoice);

        completeNode.set("choices", completeChoices);

        try {
            npcsDir.mkdirs();
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save default NPC example", e);
        }
    }
}
