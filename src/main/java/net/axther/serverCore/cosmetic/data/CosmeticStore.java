package net.axther.serverCore.cosmetic.data;

import net.axther.serverCore.cosmetic.CosmeticInstance;
import net.axther.serverCore.cosmetic.CosmeticManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.logging.Level;

public class CosmeticStore {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, PendingCosmetic> pending = new HashMap<>();

    // PERS-01: Dirty flag — set on mutation, cleared before flush dispatch.
    private boolean dirty = false;
    // PERS-02: volatile so async write thread can clear it without a memory barrier issue.
    private volatile boolean saving = false;

    public CosmeticStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "cosmetic-data.yml");
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
     * Returns a fully-populated YamlConfiguration without touching the disk.
     */
    private YamlConfiguration buildSnapshot(CosmeticManager manager) {
        YamlConfiguration config = new YamlConfiguration();

        Map<UUID, List<CosmeticInstance>> active = manager.getActiveCosmetics();
        for (var entry : active.entrySet()) {
            UUID mobUuid = entry.getKey();
            List<CosmeticInstance> instances = entry.getValue();
            if (instances.isEmpty()) continue;

            String key = "cosmetics." + mobUuid.toString();

            // Bukkit.getEntity() must run on the main thread — stays here in buildSnapshot.
            Entity entity = Bukkit.getEntity(mobUuid);
            if (entity == null) continue;

            config.set(key + ".entity-type", entity.getType().name());
            config.set(key + ".world", entity.getWorld().getName());

            List<Map<String, Object>> serializedItems = new ArrayList<>();
            for (CosmeticInstance instance : instances) {
                serializedItems.add(instance.getItem().serialize());
            }
            config.set(key + ".items", serializedItems);
        }

        // Also save pending cosmetics so they survive multiple restarts
        for (var entry : pending.entrySet()) {
            UUID mobUuid = entry.getKey();
            if (active.containsKey(mobUuid)) continue;

            PendingCosmetic pc = entry.getValue();
            String key = "cosmetics." + mobUuid.toString();
            config.set(key + ".entity-type", pc.entityType().name());
            config.set(key + ".world", pc.worldName());

            List<Map<String, Object>> serializedItems = new ArrayList<>();
            for (ItemStack item : pc.items()) {
                serializedItems.add(item.serialize());
            }
            config.set(key + ".items", serializedItems);
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
            plugin.getLogger().log(Level.SEVERE, "Failed to save cosmetic data", e);
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
            plugin.getLogger().log(Level.SEVERE, "Failed to save cosmetic data", e);
        }
    }

    // -------------------------------------------------------------------------
    // Public persistence API
    // -------------------------------------------------------------------------

    /**
     * PERS-01: Flushes dirty state to disk via snapshot-then-async write.
     * No-op if store is clean or an async write is already in flight.
     */
    public void flushIfDirty(CosmeticManager manager) {
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
    public void saveSync(CosmeticManager manager) {
        dirty = false;
        YamlConfiguration snapshot = buildSnapshot(manager);
        writeSnapshotSync(snapshot);
    }

    // -------------------------------------------------------------------------
    // Load
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public void load(CosmeticManager manager) {
        // PERS-03: Clean up any .tmp file left by a crashed async write.
        Path tmp = file.toPath().resolveSibling(file.getName() + ".tmp");
        if (Files.exists(tmp)) {
            if (!file.exists()) {
                try {
                    Files.move(tmp, file.toPath());
                } catch (IOException e) {
                    plugin.getLogger().warning("Failed to recover cosmetic data from .tmp file: " + e.getMessage());
                }
            } else {
                try {
                    Files.deleteIfExists(tmp);
                } catch (IOException e) {
                    plugin.getLogger().warning("Failed to delete orphaned cosmetic .tmp file: " + e.getMessage());
                }
            }
        }

        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection cosmetics = config.getConfigurationSection("cosmetics");
        if (cosmetics == null) return;

        for (String uuidStr : cosmetics.getKeys(false)) {
            UUID mobUuid;
            try {
                mobUuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in cosmetic data: " + uuidStr);
                continue;
            }

            ConfigurationSection entry = cosmetics.getConfigurationSection(uuidStr);
            if (entry == null) continue;

            String entityTypeName = entry.getString("entity-type");
            String worldName = entry.getString("world");
            List<?> rawItems = entry.getList("items");

            if (entityTypeName == null || worldName == null || rawItems == null) continue;

            EntityType entityType;
            try {
                entityType = EntityType.valueOf(entityTypeName);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Unknown entity type in cosmetic data: " + entityTypeName);
                continue;
            }

            if (manager.getProfile(entityType) == null) {
                plugin.getLogger().warning("No cosmetic profile for entity type: " + entityTypeName + ", skipping");
                continue;
            }

            List<ItemStack> items = new ArrayList<>();
            for (Object rawItem : rawItems) {
                if (rawItem instanceof Map<?, ?> map) {
                    try {
                        ItemStack item = ItemStack.deserialize((Map<String, Object>) map);
                        items.add(item);
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "Failed to deserialize item in cosmetic data", e);
                    }
                }
            }

            if (items.isEmpty()) continue;

            Entity entity = Bukkit.getEntity(mobUuid);
            if (entity instanceof LivingEntity living && !living.isDead()) {
                for (ItemStack item : items) {
                    manager.applyCosmetic(living, item);
                }
            } else {
                pending.put(mobUuid, new PendingCosmetic(entityType, worldName, items));
            }
        }
    }

    public Map<UUID, PendingCosmetic> getPending() {
        return Collections.unmodifiableMap(pending);
    }

    public void removePending(UUID mobUuid) {
        pending.remove(mobUuid);
    }

    public record PendingCosmetic(EntityType entityType, String worldName, List<ItemStack> items) {
    }
}
