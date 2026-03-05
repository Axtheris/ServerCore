package net.axther.serverCore.quest;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Quest {

    private final String id;
    private final String displayName;
    private final String description;
    private final String acceptNpc;
    private final String turnInNpc;
    private final List<QuestObjective> objectives;
    private final List<QuestReward> rewards;
    private final boolean repeatable;
    private final int cooldownSeconds;
    private final String requiredPermission;
    private final List<String> prerequisites;
    private final int timeLimit; // seconds, 0 = no limit
    private final String category;
    private final boolean sequentialObjectives;

    public Quest(String id, String displayName, String description,
                 String acceptNpc, String turnInNpc,
                 List<QuestObjective> objectives, List<QuestReward> rewards,
                 boolean repeatable, int cooldownSeconds) {
        this(id, displayName, description, acceptNpc, turnInNpc,
                objectives, rewards, repeatable, cooldownSeconds,
                null, List.of(), 0, "general", false);
    }

    public Quest(String id, String displayName, String description,
                 String acceptNpc, String turnInNpc,
                 List<QuestObjective> objectives, List<QuestReward> rewards,
                 boolean repeatable, int cooldownSeconds,
                 String requiredPermission, List<String> prerequisites,
                 int timeLimit, String category, boolean sequentialObjectives) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.acceptNpc = acceptNpc;
        this.turnInNpc = turnInNpc;
        this.objectives = objectives;
        this.rewards = rewards;
        this.repeatable = repeatable;
        this.cooldownSeconds = cooldownSeconds;
        this.requiredPermission = requiredPermission;
        this.prerequisites = prerequisites;
        this.timeLimit = timeLimit;
        this.category = category;
        this.sequentialObjectives = sequentialObjectives;
    }

    @SuppressWarnings("unchecked")
    public static Quest fromConfig(ConfigurationSection section, String fallbackNpcId) {
        String id = section.getString("id", "");
        String displayName = section.getString("display-name", "<white>" + id);
        String description = section.getString("description", "");
        String acceptNpc = section.getString("accept-npc", fallbackNpcId);
        String turnInNpc = section.getString("turn-in-npc", fallbackNpcId);
        boolean repeatable = section.getBoolean("repeatable", false);
        int cooldown = section.getInt("cooldown", 0);

        List<QuestObjective> objectives = new ArrayList<>();
        List<?> objList = section.getList("objectives");
        if (objList != null) {
            for (Object obj : objList) {
                if (obj instanceof Map<?, ?> map) {
                    MemoryConfiguration objSec = new MemoryConfiguration();
                    for (Map.Entry<String, Object> entry : ((Map<String, Object>) map).entrySet()) {
                        objSec.set(entry.getKey(), entry.getValue());
                    }
                    objectives.add(QuestObjective.fromConfig(objSec));
                }
            }
        }

        List<QuestReward> rewards = new ArrayList<>();
        List<?> rewList = section.getList("rewards");
        if (rewList != null) {
            for (Object obj : rewList) {
                if (obj instanceof Map<?, ?> map) {
                    MemoryConfiguration rewSec = new MemoryConfiguration();
                    for (Map.Entry<String, Object> entry : ((Map<String, Object>) map).entrySet()) {
                        rewSec.set(entry.getKey(), entry.getValue());
                    }
                    rewards.add(QuestReward.fromConfig(rewSec));
                }
            }
        }

        String requiredPermission = section.getString("required-permission", null);
        List<String> prerequisites = section.getStringList("prerequisites");
        int timeLimit = section.getInt("time-limit", 0);
        String category = section.getString("category", "general");
        boolean sequentialObjectives = section.getBoolean("sequential-objectives", false);

        return new Quest(id, displayName, description, acceptNpc, turnInNpc,
                objectives, rewards, repeatable, cooldown,
                requiredPermission, prerequisites, timeLimit, category, sequentialObjectives);
    }

    // Getters
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public String getAcceptNpc() { return acceptNpc; }
    public String getTurnInNpc() { return turnInNpc; }
    public List<QuestObjective> getObjectives() { return objectives; }
    public List<QuestReward> getRewards() { return rewards; }
    public boolean isRepeatable() { return repeatable; }
    public int getCooldownSeconds() { return cooldownSeconds; }
    public String getRequiredPermission() { return requiredPermission; }
    public List<String> getPrerequisites() { return prerequisites; }
    public int getTimeLimit() { return timeLimit; }
    public String getCategory() { return category; }
    public boolean isSequentialObjectives() { return sequentialObjectives; }
}
