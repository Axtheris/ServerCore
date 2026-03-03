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
import java.util.*;
import java.util.logging.Level;

public class CosmeticStore {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, PendingCosmetic> pending = new HashMap<>();

    public CosmeticStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "cosmetic-data.yml");
    }

    public void save(CosmeticManager manager) {
        YamlConfiguration config = new YamlConfiguration();

        Map<UUID, List<CosmeticInstance>> active = manager.getActiveCosmetics();
        for (var entry : active.entrySet()) {
            UUID mobUuid = entry.getKey();
            List<CosmeticInstance> instances = entry.getValue();
            if (instances.isEmpty()) continue;

            String key = "cosmetics." + mobUuid.toString();

            // Determine entity type and world from the mob
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

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save cosmetic data", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void load(CosmeticManager manager) {
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
