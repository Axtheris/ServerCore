package net.axther.serverCore.quest;

import java.util.List;

/**
 * Immutable definition of a quest, including its objectives, rewards, and NPC bindings.
 */
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

    public Quest(String id, String displayName, String description,
                 String acceptNpc, String turnInNpc,
                 List<QuestObjective> objectives, List<QuestReward> rewards,
                 boolean repeatable, int cooldownSeconds) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.acceptNpc = acceptNpc;
        this.turnInNpc = turnInNpc;
        this.objectives = List.copyOf(objectives);
        this.rewards = List.copyOf(rewards);
        this.repeatable = repeatable;
        this.cooldownSeconds = cooldownSeconds;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getAcceptNpc() {
        return acceptNpc;
    }

    public String getTurnInNpc() {
        return turnInNpc;
    }

    public List<QuestObjective> getObjectives() {
        return objectives;
    }

    public List<QuestReward> getRewards() {
        return rewards;
    }

    public boolean isRepeatable() {
        return repeatable;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }
}
