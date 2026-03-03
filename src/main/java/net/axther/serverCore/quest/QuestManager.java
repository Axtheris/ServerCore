package net.axther.serverCore.quest;

import net.axther.serverCore.api.event.QuestCompleteEvent;
import net.axther.serverCore.api.event.QuestStartEvent;
import net.axther.serverCore.quest.data.QuestStore;
import org.bukkit.Bukkit;
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

        // Fire event
        QuestStartEvent event = new QuestStartEvent(player, questId);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

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

        // Fire event
        Bukkit.getPluginManager().callEvent(new QuestCompleteEvent(player, questId));

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

    public int countMaterialPublic(Player player, String materialName) {
        return countMaterial(player, materialName);
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
