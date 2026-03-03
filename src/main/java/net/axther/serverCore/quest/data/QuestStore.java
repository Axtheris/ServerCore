package net.axther.serverCore.quest.data;

import net.axther.serverCore.quest.QuestManager;
import net.axther.serverCore.quest.QuestProgress;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class QuestStore {

    private final JavaPlugin plugin;
    private final File file;

    public QuestStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "quest-data.yml");
    }

    public void load(QuestManager manager) {
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = config.getConfigurationSection("players");
        if (players == null) return;

        for (String uuidStr : players.getKeys(false)) {
            UUID playerId;
            try {
                playerId = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                continue;
            }

            ConfigurationSection playerSec = players.getConfigurationSection(uuidStr);
            if (playerSec == null) continue;

            // Load active quests
            ConfigurationSection activeSec = playerSec.getConfigurationSection("active");
            if (activeSec != null) {
                for (String questId : activeSec.getKeys(false)) {
                    List<Integer> progressList = activeSec.getIntegerList(questId + ".progress");
                    int[] progressArr = progressList.stream().mapToInt(Integer::intValue).toArray();
                    QuestProgress progress = new QuestProgress(questId, progressArr);
                    manager.getAllActiveQuests()
                            .computeIfAbsent(playerId, k -> new ArrayList<>()).add(progress);
                }
            }

            // Load completed quests
            ConfigurationSection completedSec = playerSec.getConfigurationSection("completed");
            if (completedSec != null) {
                for (String questId : completedSec.getKeys(false)) {
                    long timestamp = completedSec.getLong(questId);
                    manager.getAllCompletedQuests()
                            .computeIfAbsent(playerId, k -> new HashMap<>()).put(questId, timestamp);
                }
            }
        }
    }

    public void save(QuestManager manager) {
        YamlConfiguration config = new YamlConfiguration();

        // Save active quests
        for (var entry : manager.getAllActiveQuests().entrySet()) {
            String path = "players." + entry.getKey().toString();
            for (QuestProgress progress : entry.getValue()) {
                List<Integer> progressList = new ArrayList<>();
                for (int val : progress.getObjectiveProgress()) {
                    progressList.add(val);
                }
                config.set(path + ".active." + progress.getQuestId() + ".progress", progressList);
            }
        }

        // Save completed quests
        for (var entry : manager.getAllCompletedQuests().entrySet()) {
            String path = "players." + entry.getKey().toString();
            for (var questEntry : entry.getValue().entrySet()) {
                config.set(path + ".completed." + questEntry.getKey(), questEntry.getValue());
            }
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save quest data", e);
        }
    }
}
