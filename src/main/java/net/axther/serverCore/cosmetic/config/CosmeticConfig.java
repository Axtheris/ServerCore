package net.axther.serverCore.cosmetic.config;

import net.axther.serverCore.cosmetic.CosmeticManager;
import net.axther.serverCore.cosmetic.MobCosmeticProfile;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public class CosmeticConfig {

    private final JavaPlugin plugin;

    public CosmeticConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public void saveProfile(EntityType type, MobCosmeticProfile profile) {
        File file = new File(plugin.getDataFolder(), "cosmetics.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String key = "mobs." + type.name().toLowerCase();
        config.set(key + ".head-y", profile.getHeadY());
        config.set(key + ".head-forward-z", profile.getHeadForwardZ());
        config.set(key + ".head-side-x", profile.getHeadSideX());
        config.set(key + ".use-small-stand", profile.useSmallStand());
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save cosmetics.yml: " + e.getMessage());
        }
    }

    public void loadAndRegister(CosmeticManager manager) {
        File file = new File(plugin.getDataFolder(), "cosmetics.yml");
        if (!file.exists()) {
            plugin.saveResource("cosmetics.yml", false);
        }

        Logger logger = plugin.getLogger();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection mobsSection = config.getConfigurationSection("mobs");
        if (mobsSection == null) {
            logger.info("No mob cosmetic profiles found in cosmetics.yml");
            return;
        }

        for (String key : mobsSection.getKeys(false)) {
            ConfigurationSection mobSection = mobsSection.getConfigurationSection(key);
            if (mobSection == null) continue;

            EntityType type;
            try {
                type = EntityType.valueOf(key.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warning("Unknown entity type in cosmetics.yml: " + key);
                continue;
            }

            // Skip if a Java profile is already registered (Java profiles take priority)
            if (manager.getProfile(type) != null) {
                logger.info("Skipping config profile for " + key + " (Java profile already registered)");
                continue;
            }

            double headY = mobSection.getDouble("head-y", 1.0);
            double headForwardZ = mobSection.getDouble("head-forward-z", 0.0);
            double headSideX = mobSection.getDouble("head-side-x", 0.0);
            boolean useSmallStand = mobSection.getBoolean("use-small-stand", false);

            manager.registerProfile(type, new MobCosmeticProfile(headY, headForwardZ, headSideX, useSmallStand));
            logger.info("Loaded cosmetic profile for " + key + " from config");
        }
    }
}
