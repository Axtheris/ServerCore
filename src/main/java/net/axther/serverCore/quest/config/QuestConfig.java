package net.axther.serverCore.quest.config;

import net.axther.serverCore.quest.Quest;
import net.axther.serverCore.quest.QuestManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;

public class QuestConfig {

    private final JavaPlugin plugin;
    private final File questsDir;

    public QuestConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.questsDir = new File(plugin.getDataFolder(), "quests");
    }

    public void loadAll(QuestManager manager) {
        if (!questsDir.exists()) {
            questsDir.mkdirs();
            return;
        }

        File[] files = questsDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                Quest quest = Quest.fromConfig(yaml, null);
                if (quest.getId() != null && !quest.getId().isBlank()) {
                    manager.registerQuest(quest);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load quest from " + file.getName(), e);
            }
        }

        plugin.getLogger().info("Loaded " + manager.getAllQuests().size() + " quests");
    }
}
