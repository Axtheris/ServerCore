package net.axther.serverCore.quest.data;

import net.axther.serverCore.quest.QuestManager;
import net.axther.serverCore.quest.QuestProgress;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.logging.Level;

public class QuestStore {

    private final JavaPlugin plugin;
    private final File file;

    // PERS-01: Dirty flag — set on mutation, cleared before flush dispatch.
    private boolean dirty = false;
    // PERS-02: volatile so async write thread can clear it without a memory barrier issue.
    private volatile boolean saving = false;

    public QuestStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "quest-data.yml");
    }

    // -------------------------------------------------------------------------
    // Dirty-flag API
    // -------------------------------------------------------------------------

    public void markDirty() {
        dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    // -------------------------------------------------------------------------
    // Snapshot helpers
    // -------------------------------------------------------------------------

    /**
     * PERS-02: snapshot is built on main thread (safe to access Bukkit API and live collections).
     * CRITICAL: int[] objectiveProgress is mutable — deep-copy via Arrays.copyOf before dispatch.
     */
    private YamlConfiguration buildSnapshot(QuestManager manager) {
        YamlConfiguration config = new YamlConfiguration();

        // Save active quests
        for (var entry : manager.getAllActiveQuests().entrySet()) {
            String path = "players." + entry.getKey().toString();
            for (QuestProgress progress : entry.getValue()) {
                // Deep-copy the mutable int[] so the async thread reads a stable snapshot.
                int[] progressCopy = Arrays.copyOf(
                        progress.getObjectiveProgress(),
                        progress.getObjectiveProgress().length);
                List<Integer> progressList = new ArrayList<>(progressCopy.length);
                for (int val : progressCopy) {
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

        return config;
    }

    // -------------------------------------------------------------------------
    // Write helpers
    // -------------------------------------------------------------------------

    /**
     * PERS-02: writeSnapshot runs on async thread — only touches the detached YamlConfiguration.
     * YamlConfiguration is read-only after buildSnapshot() returns — no concurrent access.
     * Uses atomic file swap for crash safety (PERS-03).
     */
    private void writeSnapshot(YamlConfiguration snapshot) {
        try {
            Path tmp = file.toPath().resolveSibling(file.getName() + ".tmp");
            snapshot.save(tmp.toFile());
            try {
                Files.move(tmp, file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save quest data", e);
        } finally {
            saving = false;
        }
    }

    /**
     * Synchronous write — same atomic swap logic as writeSnapshot but without the saving flag
     * management. Only called from onDisable on the main thread when no async write is in flight.
     */
    private void writeSnapshotSync(YamlConfiguration snapshot) {
        try {
            Path tmp = file.toPath().resolveSibling(file.getName() + ".tmp");
            snapshot.save(tmp.toFile());
            try {
                Files.move(tmp, file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save quest data", e);
        }
    }

    // -------------------------------------------------------------------------
    // Public persistence API
    // -------------------------------------------------------------------------

    /**
     * PERS-01: Flushes dirty state to disk via snapshot-then-async write.
     * No-op if store is clean or an async write is already in flight.
     */
    public void flushIfDirty(QuestManager manager) {
        if (!dirty) return;
        if (saving) return;  // async write in flight, skip (PERS-02)
        saving = true;
        dirty = false;  // clear BEFORE dispatch — mutations after snapshot set dirty again (correct)
        YamlConfiguration snapshot = buildSnapshot(manager);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> writeSnapshot(snapshot));
    }

    /**
     * PERS-01 / onDisable: Synchronous save — guaranteed to complete before process exits.
     */
    public void saveSync(QuestManager manager) {
        dirty = false;
        YamlConfiguration snapshot = buildSnapshot(manager);
        writeSnapshotSync(snapshot);
    }

    // -------------------------------------------------------------------------
    // Load
    // -------------------------------------------------------------------------

    public void load(QuestManager manager) {
        // PERS-03: Clean up any .tmp file left by a crashed async write.
        Path tmp = file.toPath().resolveSibling(file.getName() + ".tmp");
        if (Files.exists(tmp)) {
            if (!file.exists()) {
                try {
                    Files.move(tmp, file.toPath());
                } catch (IOException e) {
                    plugin.getLogger().warning("Failed to recover quest data from .tmp file: " + e.getMessage());
                }
            } else {
                try {
                    Files.deleteIfExists(tmp);
                } catch (IOException e) {
                    plugin.getLogger().warning("Failed to delete orphaned quest .tmp file: " + e.getMessage());
                }
            }
        }

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
}
