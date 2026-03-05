package net.axther.serverCore.quest;

import net.axther.serverCore.api.event.QuestAbandonEvent;
import net.axther.serverCore.api.event.QuestCompleteEvent;
import net.axther.serverCore.api.event.QuestProgressEvent;
import net.axther.serverCore.api.event.QuestStartEvent;
import net.axther.serverCore.quest.data.QuestStore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;

import java.util.*;

public class QuestManager {

    private final Map<String, Quest> quests = new LinkedHashMap<>();
    private QuestStore store;
    private int maxActiveQuests = 0;
    private net.axther.serverCore.pet.PetManager petManager;

    public void setMaxActiveQuests(int max) { this.maxActiveQuests = max; }
    public void setPetManager(net.axther.serverCore.pet.PetManager petManager) { this.petManager = petManager; }

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

        // Check required permission
        if (quest.getRequiredPermission() != null && !player.hasPermission(quest.getRequiredPermission())) {
            return false;
        }

        // Check prerequisites
        for (String prereq : quest.getPrerequisites()) {
            if (!hasCompleted(player.getUniqueId(), prereq)) return false;
        }

        // Check max active quests
        if (maxActiveQuests > 0) {
            List<QuestProgress> active = activeQuests.get(player.getUniqueId());
            if (active != null && active.size() >= maxActiveQuests) return false;
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
            if (reward.getType() == QuestReward.Type.PET) {
                if (petManager != null) {
                    var profile = petManager.getProfile(reward.getValue());
                    if (profile != null) {
                        for (int i = 0; i < reward.getAmount(); i++) {
                            player.getInventory().addItem(profile.createItem());
                        }
                    } else {
                        Bukkit.getLogger().warning("Quest '" + questId + "' has PET reward for unknown pet: " + reward.getValue());
                    }
                } else {
                    Bukkit.getLogger().warning("Quest '" + questId + "' has PET reward but pet system is not enabled");
                }
            } else {
                reward.give(player);
            }
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
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            QuestAbandonEvent event = new QuestAbandonEvent(player, questId);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) return;
        }
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

    // --- Shared objective increment helper ---

    private void incrementObjective(UUID playerId, QuestObjective.Type type, String target, int amount) {
        List<QuestProgress> list = activeQuests.get(playerId);
        if (list == null) return;

        for (QuestProgress progress : list) {
            Quest quest = quests.get(progress.getQuestId());
            if (quest == null) continue;

            // Check time limit expiry
            if (quest.getTimeLimit() > 0 && progress.isExpired(quest.getTimeLimit())) continue;

            List<QuestObjective> objectives = quest.getObjectives();
            for (int i = 0; i < objectives.size(); i++) {
                QuestObjective obj = objectives.get(i);
                if (obj.getType() != type) continue;

                // Sequential: skip if not current objective
                if (quest.isSequentialObjectives() && i > getFirstIncompleteIndex(progress, objectives)) continue;

                boolean matches = type == QuestObjective.Type.FISH
                        ? (obj.getTarget().equalsIgnoreCase("ANY") || obj.getTarget().equalsIgnoreCase(target))
                        : obj.getTarget().equalsIgnoreCase(target);

                if (matches && progress.getProgress(i) < obj.getAmount()) {
                    for (int a = 0; a < amount && progress.getProgress(i) < obj.getAmount(); a++) {
                        progress.increment(i);
                    }
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null) {
                        Bukkit.getPluginManager().callEvent(
                                new QuestProgressEvent(player, progress.getQuestId(), i, progress.getProgress(i)));
                    }
                }
            }
        }
    }

    private int getFirstIncompleteIndex(QuestProgress progress, List<QuestObjective> objectives) {
        for (int i = 0; i < objectives.size(); i++) {
            if (progress.getProgress(i) < objectives.get(i).getAmount()) return i;
        }
        return objectives.size();
    }

    // --- Kill tracking (called from listener) ---

    public void handleKill(UUID playerId, String entityTypeName) {
        incrementObjective(playerId, QuestObjective.Type.KILL, entityTypeName, 1);
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
                    if (quest.isSequentialObjectives() && i > getFirstIncompleteIndex(progress, objectives)) continue;
                    progress.setProgress(i, 1);
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null) {
                        Bukkit.getPluginManager().callEvent(
                                new QuestProgressEvent(player, progress.getQuestId(), i, 1));
                    }
                }
            }
        }
    }

    // --- New handler methods ---

    public void handleCraft(UUID playerId, String materialName, int amount) {
        incrementObjective(playerId, QuestObjective.Type.CRAFT, materialName, amount);
    }

    public void handleMine(UUID playerId, String materialName) {
        incrementObjective(playerId, QuestObjective.Type.MINE, materialName, 1);
    }

    public void handlePlace(UUID playerId, String materialName) {
        incrementObjective(playerId, QuestObjective.Type.PLACE, materialName, 1);
    }

    public void handleFish(UUID playerId, String materialName) {
        incrementObjective(playerId, QuestObjective.Type.FISH, materialName, 1);
    }

    public void handleBreed(UUID playerId, String entityTypeName) {
        incrementObjective(playerId, QuestObjective.Type.BREED, entityTypeName, 1);
    }

    public void handleSmelt(UUID playerId, String materialName, int amount) {
        incrementObjective(playerId, QuestObjective.Type.SMELT, materialName, amount);
    }

    public void handleInteract(UUID playerId, String targetName) {
        incrementObjective(playerId, QuestObjective.Type.INTERACT, targetName, 1);
    }

    // --- Explore tracking (location-based) ---

    public void handleExplore(UUID playerId, Location location) {
        List<QuestProgress> list = activeQuests.get(playerId);
        if (list == null) return;

        for (QuestProgress progress : list) {
            Quest quest = quests.get(progress.getQuestId());
            if (quest == null) continue;

            List<QuestObjective> objectives = quest.getObjectives();
            for (int i = 0; i < objectives.size(); i++) {
                QuestObjective obj = objectives.get(i);
                if (obj.getType() != QuestObjective.Type.EXPLORE) continue;
                if (quest.isSequentialObjectives() && i > getFirstIncompleteIndex(progress, objectives)) continue;
                if (progress.getProgress(i) >= 1) continue;

                String[] parts = obj.getTarget().split(",");
                if (parts.length != 4) continue;
                String world = parts[0];
                if (location.getWorld() == null || !location.getWorld().getName().equalsIgnoreCase(world)) continue;

                try {
                    double tx = Double.parseDouble(parts[1]);
                    double ty = Double.parseDouble(parts[2]);
                    double tz = Double.parseDouble(parts[3]);
                    double distSq = Math.pow(location.getX() - tx, 2) + Math.pow(location.getY() - ty, 2) + Math.pow(location.getZ() - tz, 2);
                    if (distSq <= obj.getRadius() * obj.getRadius()) {
                        progress.setProgress(i, 1);
                        Player player = Bukkit.getPlayer(playerId);
                        if (player != null) {
                            Bukkit.getPluginManager().callEvent(
                                    new QuestProgressEvent(player, progress.getQuestId(), i, 1));
                        }
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    // --- Expired quest check ---

    public void checkExpiredQuests(Player player) {
        List<QuestProgress> list = activeQuests.get(player.getUniqueId());
        if (list == null) return;

        list.removeIf(progress -> {
            Quest quest = quests.get(progress.getQuestId());
            if (quest == null) return false;
            if (quest.getTimeLimit() > 0 && progress.isExpired(quest.getTimeLimit())) {
                player.sendMessage(Component.text(
                        "Quest '" + quest.getDisplayName() + "' has expired!",
                        NamedTextColor.RED));
                return true;
            }
            return false;
        });
        if (list.isEmpty()) activeQuests.remove(player.getUniqueId());
    }

    // --- Cleanup ---

    public void destroyAll() {
        quests.clear();
        activeQuests.clear();
    }

    public Map<UUID, List<QuestProgress>> getAllActiveQuests() { return activeQuests; }
    public Map<UUID, Map<String, Long>> getAllCompletedQuests() { return completedQuests; }

    // --- Inventory helpers ---

    public int countMaterial(Player player, String materialName) {
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
