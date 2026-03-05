# Quest System Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a YAML-driven quest system integrated with the NPC dialogue system, supporting fetch/kill/talk objectives with item/XP/command rewards.

**Architecture:** Quests are data objects loaded from `quests/*.yml` and inline NPC YAML. `QuestManager` tracks definitions and per-player progress. The dialogue system gains new conditions (`quest_available`, `quest_active`, `quest_complete`, `quest_finished`) and actions (`accept_quest`, `complete_quest`) that delegate to `QuestManager`. Kill tracking via `EntityDeathEvent`, fetch checking on-demand, talk tracking on NPC interaction. Progress persisted to `quest-data.yml`.

**Tech Stack:** Paper 1.21 API, YAML config, existing dialogue condition/action interfaces.

---

### Task 1: Quest data model classes

**Files:**
- Create: `src/main/java/net/axther/serverCore/quest/Quest.java`
- Create: `src/main/java/net/axther/serverCore/quest/QuestObjective.java`
- Create: `src/main/java/net/axther/serverCore/quest/QuestReward.java`
- Create: `src/main/java/net/axther/serverCore/quest/QuestProgress.java`

**Step 1: Create QuestObjective**

```java
package net.axther.serverCore.quest;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

public class QuestObjective {

    public enum Type { FETCH, KILL, TALK }

    private final Type type;
    private final String target; // material name, entity type name, or NPC id
    private final int amount;    // 1 for talk objectives

    public QuestObjective(Type type, String target, int amount) {
        this.type = type;
        this.target = target;
        this.amount = amount;
    }

    public static QuestObjective fromConfig(ConfigurationSection section) {
        String typeStr = section.getString("type", "fetch");
        return switch (typeStr.toLowerCase()) {
            case "kill" -> new QuestObjective(Type.KILL,
                    section.getString("entity", "ZOMBIE"), section.getInt("amount", 1));
            case "talk" -> new QuestObjective(Type.TALK,
                    section.getString("npc", ""), 1);
            default -> new QuestObjective(Type.FETCH,
                    section.getString("material", "DIRT"), section.getInt("amount", 1));
        };
    }

    public Type getType() { return type; }
    public String getTarget() { return target; }
    public int getAmount() { return amount; }
}
```

**Step 2: Create QuestReward**

```java
package net.axther.serverCore.quest;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class QuestReward {

    public enum Type { ITEM, XP, COMMAND }

    private final Type type;
    private final String value;  // material name, xp amount, or command string
    private final int amount;

    public QuestReward(Type type, String value, int amount) {
        this.type = type;
        this.value = value;
        this.amount = amount;
    }

    public static QuestReward fromConfig(ConfigurationSection section) {
        String typeStr = section.getString("type", "item");
        return switch (typeStr.toLowerCase()) {
            case "xp" -> new QuestReward(Type.XP, "", section.getInt("amount", 0));
            case "command" -> new QuestReward(Type.COMMAND, section.getString("value", ""), 1);
            default -> new QuestReward(Type.ITEM,
                    section.getString("material", "DIRT"), section.getInt("amount", 1));
        };
    }

    public void give(Player player) {
        switch (type) {
            case ITEM -> {
                Material material = Material.matchMaterial(value);
                if (material != null) {
                    player.getInventory().addItem(new ItemStack(material, amount));
                }
            }
            case XP -> player.giveExp(amount);
            case COMMAND -> {
                String cmd = value.replace("%player%", player.getName());
                player.getServer().dispatchCommand(player.getServer().getConsoleSender(), cmd);
            }
        }
    }

    public Type getType() { return type; }
    public String getValue() { return value; }
    public int getAmount() { return amount; }
}
```

**Step 3: Create Quest**

```java
package net.axther.serverCore.quest;

import org.bukkit.configuration.ConfigurationSection;

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

    public Quest(String id, String displayName, String description,
                 String acceptNpc, String turnInNpc,
                 List<QuestObjective> objectives, List<QuestReward> rewards,
                 boolean repeatable, int cooldownSeconds) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.acceptNpc = acceptNpc;
        this.turnInNpc = turnInNpc;
        this.objectives = objectives;
        this.rewards = rewards;
        this.repeatable = repeatable;
        this.cooldownSeconds = cooldownSeconds;
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
                    ConfigurationSection objSec = section.createSection("_obj_" + System.nanoTime());
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
                    ConfigurationSection rewSec = section.createSection("_rew_" + System.nanoTime());
                    for (Map.Entry<String, Object> entry : ((Map<String, Object>) map).entrySet()) {
                        rewSec.set(entry.getKey(), entry.getValue());
                    }
                    rewards.add(QuestReward.fromConfig(rewSec));
                }
            }
        }

        return new Quest(id, displayName, description, acceptNpc, turnInNpc,
                objectives, rewards, repeatable, cooldown);
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
}
```

**Step 4: Create QuestProgress**

```java
package net.axther.serverCore.quest;

public class QuestProgress {

    private final String questId;
    private final int[] objectiveProgress;

    public QuestProgress(String questId, int objectiveCount) {
        this.questId = questId;
        this.objectiveProgress = new int[objectiveCount];
    }

    public QuestProgress(String questId, int[] objectiveProgress) {
        this.questId = questId;
        this.objectiveProgress = objectiveProgress;
    }

    public String getQuestId() { return questId; }
    public int[] getObjectiveProgress() { return objectiveProgress; }

    public int getProgress(int index) {
        return index >= 0 && index < objectiveProgress.length ? objectiveProgress[index] : 0;
    }

    public void setProgress(int index, int value) {
        if (index >= 0 && index < objectiveProgress.length) {
            objectiveProgress[index] = value;
        }
    }

    public void increment(int index) {
        if (index >= 0 && index < objectiveProgress.length) {
            objectiveProgress[index]++;
        }
    }
}
```

**Step 5: Commit**

```bash
git add src/main/java/net/axther/serverCore/quest/
git commit -m "feat(quest): add quest data model classes"
```

---

### Task 2: QuestManager -- registry and progress logic

**Files:**
- Create: `src/main/java/net/axther/serverCore/quest/QuestManager.java`

**Step 1: Create QuestManager**

Core logic class. Manages quest definitions, per-player active quests, and completion history.

```java
package net.axther.serverCore.quest;

import net.axther.serverCore.quest.data.QuestStore;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class QuestManager {

    private final Map<String, Quest> quests = new LinkedHashMap<>();
    private QuestStore store;

    // Player UUID -> list of active quest progress
    private final Map<UUID, List<QuestProgress>> activeQuests = new HashMap<>();
    // Player UUID -> map of completed quest id -> completion timestamp (epoch millis)
    private final Map<UUID, Map<String, Long>> completedQuests = new HashMap<>();

    public void setStore(QuestStore store) {
        this.store = store;
    }

    public void registerQuest(Quest quest) {
        quests.put(quest.getId(), quest);
    }

    public Quest getQuest(String id) {
        return quests.get(id);
    }

    public Collection<Quest> getAllQuests() {
        return Collections.unmodifiableCollection(quests.values());
    }

    // --- Accept / Complete ---

    public boolean canAccept(Player player, String questId) {
        Quest quest = quests.get(questId);
        if (quest == null) return false;

        // Already active?
        if (getActiveProgress(player.getUniqueId(), questId) != null) return false;

        // Already completed and not repeatable?
        Map<String, Long> completed = completedQuests.get(player.getUniqueId());
        if (completed != null && completed.containsKey(questId)) {
            if (!quest.isRepeatable()) return false;
            // Check cooldown
            if (quest.getCooldownSeconds() > 0) {
                long completedAt = completed.get(questId);
                long elapsed = (System.currentTimeMillis() - completedAt) / 1000;
                if (elapsed < quest.getCooldownSeconds()) return false;
            }
        }

        return true;
    }

    public boolean acceptQuest(Player player, String questId) {
        if (!canAccept(player, questId)) return false;
        Quest quest = quests.get(questId);

        QuestProgress progress = new QuestProgress(questId, quest.getObjectives().size());
        activeQuests.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(progress);
        return true;
    }

    public boolean areObjectivesComplete(Player player, String questId) {
        Quest quest = quests.get(questId);
        if (quest == null) return false;

        QuestProgress progress = getActiveProgress(player.getUniqueId(), questId);
        if (progress == null) return false;

        List<QuestObjective> objectives = quest.getObjectives();
        for (int i = 0; i < objectives.size(); i++) {
            QuestObjective obj = objectives.get(i);
            int current = progress.getProgress(i);

            if (obj.getType() == QuestObjective.Type.FETCH) {
                // Check inventory on-demand
                current = countMaterial(player, obj.getTarget());
            }

            if (current < obj.getAmount()) return false;
        }
        return true;
    }

    public boolean completeQuest(Player player, String questId) {
        Quest quest = quests.get(questId);
        if (quest == null) return false;

        if (!areObjectivesComplete(player, questId)) return false;

        // Consume fetch items
        for (QuestObjective obj : quest.getObjectives()) {
            if (obj.getType() == QuestObjective.Type.FETCH) {
                removeMaterial(player, obj.getTarget(), obj.getAmount());
            }
        }

        // Give rewards
        for (QuestReward reward : quest.getRewards()) {
            reward.give(player);
        }

        // Remove from active, add to completed
        removeActiveProgress(player.getUniqueId(), questId);
        completedQuests.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                .put(questId, System.currentTimeMillis());

        return true;
    }

    public void abandonQuest(UUID playerId, String questId) {
        removeActiveProgress(playerId, questId);
    }

    // --- Progress helpers ---

    public QuestProgress getActiveProgress(UUID playerId, String questId) {
        List<QuestProgress> list = activeQuests.get(playerId);
        if (list == null) return null;
        for (QuestProgress p : list) {
            if (p.getQuestId().equals(questId)) return p;
        }
        return null;
    }

    public List<QuestProgress> getActiveQuests(UUID playerId) {
        List<QuestProgress> list = activeQuests.get(playerId);
        return list != null ? Collections.unmodifiableList(list) : List.of();
    }

    public boolean hasCompleted(UUID playerId, String questId) {
        Map<String, Long> completed = completedQuests.get(playerId);
        return completed != null && completed.containsKey(questId);
    }

    public Map<String, Long> getCompletedQuests(UUID playerId) {
        Map<String, Long> completed = completedQuests.get(playerId);
        return completed != null ? Collections.unmodifiableMap(completed) : Map.of();
    }

    public boolean isActive(UUID playerId, String questId) {
        return getActiveProgress(playerId, questId) != null;
    }

    // --- Kill tracking (called from listener) ---

    public void handleKill(UUID playerId, String entityTypeName) {
        List<QuestProgress> list = activeQuests.get(playerId);
        if (list == null) return;

        for (QuestProgress progress : list) {
            Quest quest = quests.get(progress.getQuestId());
            if (quest == null) continue;

            List<QuestObjective> objectives = quest.getObjectives();
            for (int i = 0; i < objectives.size(); i++) {
                QuestObjective obj = objectives.get(i);
                if (obj.getType() == QuestObjective.Type.KILL
                        && obj.getTarget().equalsIgnoreCase(entityTypeName)
                        && progress.getProgress(i) < obj.getAmount()) {
                    progress.increment(i);
                }
            }
        }
    }

    // --- Talk tracking (called from NPC interaction) ---

    public void handleTalk(UUID playerId, String npcId) {
        List<QuestProgress> list = activeQuests.get(playerId);
        if (list == null) return;

        for (QuestProgress progress : list) {
            Quest quest = quests.get(progress.getQuestId());
            if (quest == null) continue;

            List<QuestObjective> objectives = quest.getObjectives();
            for (int i = 0; i < objectives.size(); i++) {
                QuestObjective obj = objectives.get(i);
                if (obj.getType() == QuestObjective.Type.TALK
                        && obj.getTarget().equalsIgnoreCase(npcId)
                        && progress.getProgress(i) < 1) {
                    progress.setProgress(i, 1);
                }
            }
        }
    }

    // --- Cleanup ---

    public void destroyAll() {
        quests.clear();
        // Don't clear player data -- that's managed by the store
    }

    public Map<UUID, List<QuestProgress>> getAllActiveQuests() { return activeQuests; }
    public Map<UUID, Map<String, Long>> getAllCompletedQuests() { return completedQuests; }

    // --- Inventory helpers ---

    private int countMaterial(Player player, String materialName) {
        Material material = Material.matchMaterial(materialName);
        if (material == null) return 0;
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private void removeMaterial(Player player, String materialName, int amount) {
        Material material = Material.matchMaterial(materialName);
        if (material == null) return;
        int remaining = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material && remaining > 0) {
                int take = Math.min(remaining, item.getAmount());
                item.setAmount(item.getAmount() - take);
                remaining -= take;
            }
        }
    }

    private void removeActiveProgress(UUID playerId, String questId) {
        List<QuestProgress> list = activeQuests.get(playerId);
        if (list != null) {
            list.removeIf(p -> p.getQuestId().equals(questId));
            if (list.isEmpty()) activeQuests.remove(playerId);
        }
    }
}
```

**Step 2: Commit**

```bash
git add src/main/java/net/axther/serverCore/quest/QuestManager.java
git commit -m "feat(quest): add QuestManager with accept/complete/tracking logic"
```

---

### Task 3: QuestStore -- persistence

**Files:**
- Create: `src/main/java/net/axther/serverCore/quest/data/QuestStore.java`

**Step 1: Create QuestStore**

Follows same pattern as `PetStore` -- YAML file with player UUID keys.

```java
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
```

**Step 2: Commit**

```bash
git add src/main/java/net/axther/serverCore/quest/data/QuestStore.java
git commit -m "feat(quest): add QuestStore persistence"
```

---

### Task 4: QuestConfig -- load quest YAML files and inline NPC quests

**Files:**
- Create: `src/main/java/net/axther/serverCore/quest/config/QuestConfig.java`
- Modify: `src/main/java/net/axther/serverCore/npc/config/NPCConfig.java:54-87` -- load inline quest section

**Step 1: Create QuestConfig**

Loads standalone quest files from `quests/` directory.

```java
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
```

**Step 2: Modify NPCConfig to load inline quests**

In `NPCConfig.loadNPC()` (line ~84), after loading dialogue, add quest loading. The `QuestManager` needs to be passed in. Modify `NPCConfig.loadAll()` to accept a `QuestManager` parameter.

Add after line 83 in `loadNPC`:
```java
        // Load inline quest if present
        ConfigurationSection questSec = yaml.getConfigurationSection("quest");
        if (questSec != null && questManager != null) {
            Quest quest = Quest.fromConfig(questSec, id);
            if (quest.getId() != null && !quest.getId().isBlank()) {
                questManager.registerQuest(quest);
            }
        }
```

Change `loadAll` signature to `loadAll(NPCManager manager, QuestManager questManager)` and store `questManager` as a field. Pass `null` if quest system is not active.

**Step 3: Commit**

```bash
git add src/main/java/net/axther/serverCore/quest/config/ src/main/java/net/axther/serverCore/npc/config/NPCConfig.java
git commit -m "feat(quest): add QuestConfig and inline NPC quest loading"
```

---

### Task 5: Dialogue conditions and actions for quests

**Files:**
- Create: `src/main/java/net/axther/serverCore/npc/dialogue/condition/QuestAvailableCondition.java`
- Create: `src/main/java/net/axther/serverCore/npc/dialogue/condition/QuestActiveCondition.java`
- Create: `src/main/java/net/axther/serverCore/npc/dialogue/condition/QuestCompleteCondition.java`
- Create: `src/main/java/net/axther/serverCore/npc/dialogue/condition/QuestFinishedCondition.java`
- Create: `src/main/java/net/axther/serverCore/npc/dialogue/action/AcceptQuestAction.java`
- Create: `src/main/java/net/axther/serverCore/npc/dialogue/action/CompleteQuestAction.java`
- Modify: `src/main/java/net/axther/serverCore/npc/dialogue/condition/DialogueCondition.java:14-17` -- add quest cases to switch
- Modify: `src/main/java/net/axther/serverCore/npc/dialogue/action/DialogueAction.java:14-19` -- add quest cases to switch

**Step 1: Create quest conditions**

All four follow the same pattern -- they access `QuestManager` via `ServerCoreAPI`. This avoids changing the `DialogueCondition.test(Player)` signature.

```java
// QuestAvailableCondition.java
package net.axther.serverCore.npc.dialogue.condition;

import net.axther.serverCore.api.ServerCoreAPI;
import org.bukkit.entity.Player;

public class QuestAvailableCondition implements DialogueCondition {
    private final String questId;
    public QuestAvailableCondition(String questId) { this.questId = questId; }

    @Override
    public boolean test(Player player) {
        var qm = ServerCoreAPI.get().getQuestManager();
        return qm != null && qm.canAccept(player, questId);
    }
}
```

```java
// QuestActiveCondition.java
package net.axther.serverCore.npc.dialogue.condition;

import net.axther.serverCore.api.ServerCoreAPI;
import org.bukkit.entity.Player;

public class QuestActiveCondition implements DialogueCondition {
    private final String questId;
    public QuestActiveCondition(String questId) { this.questId = questId; }

    @Override
    public boolean test(Player player) {
        var qm = ServerCoreAPI.get().getQuestManager();
        return qm != null && qm.isActive(player.getUniqueId(), questId);
    }
}
```

```java
// QuestCompleteCondition.java -- all objectives met, awaiting turn-in
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
```

```java
// QuestFinishedCondition.java -- already turned in
package net.axther.serverCore.npc.dialogue.condition;

import net.axther.serverCore.api.ServerCoreAPI;
import org.bukkit.entity.Player;

public class QuestFinishedCondition implements DialogueCondition {
    private final String questId;
    public QuestFinishedCondition(String questId) { this.questId = questId; }

    @Override
    public boolean test(Player player) {
        var qm = ServerCoreAPI.get().getQuestManager();
        return qm != null && qm.hasCompleted(player.getUniqueId(), questId);
    }
}
```

**Step 2: Create quest actions**

```java
// AcceptQuestAction.java
package net.axther.serverCore.npc.dialogue.action;

import net.axther.serverCore.api.ServerCoreAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class AcceptQuestAction implements DialogueAction {
    private final String questId;
    public AcceptQuestAction(String questId) { this.questId = questId; }

    @Override
    public void execute(Player player) {
        var qm = ServerCoreAPI.get().getQuestManager();
        if (qm == null) return;

        if (qm.acceptQuest(player, questId)) {
            var quest = qm.getQuest(questId);
            String name = quest != null ? quest.getDisplayName() : questId;
            player.sendMessage(Component.text("Quest accepted: ", NamedTextColor.GREEN)
                    .append(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(name)));
        }
    }
}
```

```java
// CompleteQuestAction.java
package net.axther.serverCore.npc.dialogue.action;

import net.axther.serverCore.api.ServerCoreAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class CompleteQuestAction implements DialogueAction {
    private final String questId;
    public CompleteQuestAction(String questId) { this.questId = questId; }

    @Override
    public void execute(Player player) {
        var qm = ServerCoreAPI.get().getQuestManager();
        if (qm == null) return;

        if (qm.completeQuest(player, questId)) {
            var quest = qm.getQuest(questId);
            String name = quest != null ? quest.getDisplayName() : questId;
            player.sendMessage(Component.text("Quest completed: ", NamedTextColor.GOLD)
                    .append(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(name)));
        }
    }
}
```

**Step 3: Wire into DialogueCondition switch**

In `DialogueCondition.java:14-17`, add cases:
```java
            case "quest_available" -> new QuestAvailableCondition(value);
            case "quest_active" -> new QuestActiveCondition(value);
            case "quest_complete" -> new QuestCompleteCondition(value);
            case "quest_finished" -> new QuestFinishedCondition(value);
```

**Step 4: Wire into DialogueAction switch**

In `DialogueAction.java:14-19`, add cases:
```java
            case "accept_quest" -> new AcceptQuestAction(value);
            case "complete_quest" -> new CompleteQuestAction(value);
```

**Step 5: Commit**

```bash
git add src/main/java/net/axther/serverCore/npc/dialogue/condition/ src/main/java/net/axther/serverCore/npc/dialogue/action/
git commit -m "feat(quest): add dialogue conditions and actions for quests"
```

---

### Task 6: QuestListener -- kill tracking and talk tracking

**Files:**
- Create: `src/main/java/net/axther/serverCore/quest/listener/QuestListener.java`
- Modify: `src/main/java/net/axther/serverCore/npc/listener/NPCListener.java:31-41` -- add talk tracking call

**Step 1: Create QuestListener**

```java
package net.axther.serverCore.quest.listener;

import net.axther.serverCore.quest.QuestManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class QuestListener implements Listener {

    private final QuestManager manager;

    public QuestListener(QuestManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        String entityTypeName = event.getEntityType().name();
        manager.handleKill(killer.getUniqueId(), entityTypeName);
    }
}
```

**Step 2: Add talk tracking in NPCListener.handleInteraction**

In `NPCListener.handleInteraction()` (line 31), before the dialogue check, add:
```java
        // Track talk objectives for active quests
        var questManager = net.axther.serverCore.api.ServerCoreAPI.get().getQuestManager();
        if (questManager != null) {
            questManager.handleTalk(player.getUniqueId(), npc.getId());
        }
```

**Step 3: Commit**

```bash
git add src/main/java/net/axther/serverCore/quest/listener/ src/main/java/net/axther/serverCore/npc/listener/NPCListener.java
git commit -m "feat(quest): add kill and talk objective tracking"
```

---

### Task 7: QuestCommand

**Files:**
- Create: `src/main/java/net/axther/serverCore/quest/command/QuestCommand.java`

**Step 1: Create QuestCommand**

```java
package net.axther.serverCore.quest.command;

import net.axther.serverCore.quest.Quest;
import net.axther.serverCore.quest.QuestManager;
import net.axther.serverCore.quest.QuestObjective;
import net.axther.serverCore.quest.QuestProgress;
import net.axther.serverCore.quest.config.QuestConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

public class QuestCommand implements TabExecutor {

    private static final List<String> SUBCOMMANDS = List.of("active", "completed", "abandon", "reload");

    private final QuestManager manager;
    private final QuestConfig config;

    public QuestCommand(QuestManager manager, QuestConfig config) {
        this.manager = manager;
        this.config = config;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /quest <active|completed|abandon|reload>",
                    NamedTextColor.YELLOW));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "active" -> handleActive(player);
            case "completed" -> handleCompleted(player);
            case "abandon" -> handleAbandon(player, args);
            case "reload" -> handleReload(player);
            default -> player.sendMessage(Component.text(
                    "Unknown subcommand. Use: active, completed, abandon, reload", NamedTextColor.RED));
        }
        return true;
    }

    private void handleActive(Player player) {
        List<QuestProgress> active = manager.getActiveQuests(player.getUniqueId());
        if (active.isEmpty()) {
            player.sendMessage(Component.text("No active quests.", NamedTextColor.GRAY));
            return;
        }

        MiniMessage mm = MiniMessage.miniMessage();
        player.sendMessage(Component.text("--- Active Quests (" + active.size() + ") ---", NamedTextColor.GREEN));

        for (QuestProgress progress : active) {
            Quest quest = manager.getQuest(progress.getQuestId());
            if (quest == null) continue;

            player.sendMessage(Component.text(" ").append(mm.deserialize(quest.getDisplayName())));

            List<QuestObjective> objectives = quest.getObjectives();
            for (int i = 0; i < objectives.size(); i++) {
                QuestObjective obj = objectives.get(i);
                int current = progress.getProgress(i);

                // For fetch objectives, show inventory count dynamically
                if (obj.getType() == QuestObjective.Type.FETCH) {
                    current = manager.countMaterialPublic(player, obj.getTarget());
                }

                String desc = switch (obj.getType()) {
                    case FETCH -> "Collect " + obj.getAmount() + " " + obj.getTarget();
                    case KILL -> "Kill " + obj.getAmount() + " " + obj.getTarget();
                    case TALK -> "Talk to " + obj.getTarget();
                };

                boolean done = current >= obj.getAmount();
                player.sendMessage(Component.text("   " + (done ? "+" : "-") + " " + desc
                                + " (" + Math.min(current, obj.getAmount()) + "/" + obj.getAmount() + ")",
                        done ? NamedTextColor.GREEN : NamedTextColor.GRAY));
            }
        }
    }

    private void handleCompleted(Player player) {
        var completed = manager.getCompletedQuests(player.getUniqueId());
        if (completed.isEmpty()) {
            player.sendMessage(Component.text("No completed quests.", NamedTextColor.GRAY));
            return;
        }

        MiniMessage mm = MiniMessage.miniMessage();
        player.sendMessage(Component.text("--- Completed Quests (" + completed.size() + ") ---",
                NamedTextColor.GOLD));

        for (String questId : completed.keySet()) {
            Quest quest = manager.getQuest(questId);
            String name = quest != null ? quest.getDisplayName() : questId;
            player.sendMessage(Component.text(" ").append(mm.deserialize(name)));
        }
    }

    private void handleAbandon(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /quest abandon <id>", NamedTextColor.YELLOW));
            return;
        }

        String id = args[1].toLowerCase();
        if (!manager.isActive(player.getUniqueId(), id)) {
            player.sendMessage(Component.text("You don't have an active quest with ID '" + id + "'.",
                    NamedTextColor.RED));
            return;
        }

        manager.abandonQuest(player.getUniqueId(), id);
        player.sendMessage(Component.text("Abandoned quest '" + id + "'.", NamedTextColor.YELLOW));
    }

    private void handleReload(Player player) {
        manager.destroyAll();
        config.loadAll(manager);
        player.sendMessage(Component.text("Reloaded " + manager.getAllQuests().size() + " quests.",
                NamedTextColor.GREEN));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("abandon") && sender instanceof Player player) {
            List<String> activeIds = manager.getActiveQuests(player.getUniqueId()).stream()
                    .map(QuestProgress::getQuestId).toList();
            return filter(activeIds, args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String input) {
        String lower = input.toLowerCase();
        return options.stream().filter(s -> s.toLowerCase().startsWith(lower)).collect(Collectors.toList());
    }
}
```

Note: `QuestManager` needs a `countMaterialPublic` method exposed for the command. Add this public wrapper to QuestManager:
```java
    public int countMaterialPublic(Player player, String materialName) {
        return countMaterial(player, materialName);
    }
```
And change `countMaterial` from `private` to `private` (keep private, add the public wrapper).

**Step 2: Commit**

```bash
git add src/main/java/net/axther/serverCore/quest/command/
git commit -m "feat(quest): add /quest command"
```

---

### Task 8: API events

**Files:**
- Create: `src/main/java/net/axther/serverCore/api/event/QuestStartEvent.java`
- Create: `src/main/java/net/axther/serverCore/api/event/QuestCompleteEvent.java`

**Step 1: Create events**

Follow existing `DialogueStartEvent` pattern.

```java
// QuestStartEvent.java
package net.axther.serverCore.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class QuestStartEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final String questId;
    private boolean cancelled;

    public QuestStartEvent(Player player, String questId) {
        this.player = player;
        this.questId = questId;
    }

    public Player getPlayer() { return player; }
    public String getQuestId() { return questId; }
    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
```

```java
// QuestCompleteEvent.java -- same structure, not cancellable
package net.axther.serverCore.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class QuestCompleteEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final String questId;

    public QuestCompleteEvent(Player player, String questId) {
        this.player = player;
        this.questId = questId;
    }

    public Player getPlayer() { return player; }
    public String getQuestId() { return questId; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
```

**Step 2: Fire events from QuestManager**

In `QuestManager.acceptQuest()`, before adding progress, fire `QuestStartEvent` and check if cancelled.
In `QuestManager.completeQuest()`, after giving rewards, fire `QuestCompleteEvent`.

**Step 3: Commit**

```bash
git add src/main/java/net/axther/serverCore/api/event/Quest*
git commit -m "feat(quest): add QuestStartEvent and QuestCompleteEvent API events"
```

---

### Task 9: Wire into ServerCore, ServerCoreAPI, plugin.yml

**Files:**
- Modify: `src/main/java/net/axther/serverCore/ServerCore.java:206-234` -- add quest init inside NPC block
- Modify: `src/main/java/net/axther/serverCore/api/ServerCoreAPI.java` -- add questManager field + getter
- Modify: `src/main/resources/plugin.yml` -- add quest command

**Step 1: Update plugin.yml**

Add after the `npc` command block:
```yaml
  quest:
    description: Manage player quests
    usage: /quest <active|completed|abandon|reload>
    permission: servercore.quest
```

Add permission:
```yaml
  servercore.quest:
    description: Allows use of the quest command
    default: true
```

Note: quest permission defaults to `true` (all players) since players need `/quest active` to check progress. The `reload` subcommand should check for op internally.

**Step 2: Update ServerCoreAPI**

Add `QuestManager questManager` field, constructor param, getter, and update `init()` signature.

**Step 3: Update ServerCore.onEnable()**

Inside the NPC/PacketEvents block (after NPC system init, before the closing brace), add:
```java
                // --- Quest System (part of NPC system) ---
                questManager = new QuestManager();
                questConfig = new QuestConfig(this);
                questConfig.loadAll(questManager);

                questStore = new QuestStore(this);
                questManager.setStore(questStore);
                questStore.load(questManager);

                getServer().getPluginManager().registerEvents(new QuestListener(questManager), this);

                PluginCommand questCmd = getCommand("quest");
                if (questCmd != null) {
                    QuestCommand questCommand = new QuestCommand(questManager, questConfig);
                    questCmd.setExecutor(questCommand);
                    questCmd.setTabCompleter(questCommand);
                }
```

Also update `npcConfig.loadAll(npcManager)` to pass questManager: `npcConfig.loadAll(npcManager, questManager)`.

Add quest fields to the class: `private QuestManager questManager;`, `private QuestConfig questConfig;`, `private QuestStore questStore;`.

In `onDisable()`, add quest save:
```java
        if (questStore != null && questManager != null) {
            questStore.save(questManager);
        }
```

Update `ServerCoreAPI.init()` call to include `questManager`.

**Step 4: Commit**

```bash
git add src/main/java/net/axther/serverCore/ServerCore.java src/main/java/net/axther/serverCore/api/ServerCoreAPI.java src/main/resources/plugin.yml
git commit -m "feat(quest): wire quest system into ServerCore and API"
```

---

### Task 10: Build verification and final commit

**Step 1: Build**

```bash
./gradlew build
```

Expected: BUILD SUCCESSFUL, all 275 tests pass.

**Step 2: Fix any compilation errors**

Common issues to watch for:
- `NPCConfig.loadAll` signature change needs updating at all call sites
- `ServerCoreAPI.init()` parameter count change
- Import statements for new quest classes in ServerCore

**Step 3: Final commit if needed**

```bash
git add -A
git commit -m "feat(quest): quest system with fetch/kill/talk objectives and NPC dialogue integration"
```

---

## Verification Checklist

1. `./gradlew build` -- compiles, all tests pass
2. Server starts with NPC system enabled -- quest system initializes, log confirms quest count
3. Create `quests/test-quest.yml` with a fetch objective -- `/quest active` shows it after accepting via dialogue
4. NPC dialogue with `quest_available` condition shows "accept quest" choice only when quest is available
5. `accept_quest` action starts the quest, player sees confirmation
6. Kill objective increments when killing correct mob type
7. Talk objective completes when interacting with target NPC
8. Fetch objective checks inventory on turn-in
9. `complete_quest` action gives rewards, consumes fetch items
10. `/quest abandon` drops the quest
11. Repeatable quest with cooldown becomes available again after cooldown expires
12. Quest progress persists across server restart (`quest-data.yml`)
13. `QuestStartEvent` / `QuestCompleteEvent` fire correctly
