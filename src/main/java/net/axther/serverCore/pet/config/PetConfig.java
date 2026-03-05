package net.axther.serverCore.pet.config;

import net.axther.serverCore.pet.PetAnimationType;
import net.axther.serverCore.pet.PetManager;
import net.axther.serverCore.pet.PetProfile;
import net.axther.serverCore.pet.PetSound;
import net.axther.serverCore.pet.util.HeadUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class PetConfig {

    private final JavaPlugin plugin;

    public PetConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAndRegister(PetManager manager) {
        File petsDir = new File(plugin.getDataFolder(), "pets");

        // Create pets/ folder and save all default pet configs if it doesn't exist
        if (!petsDir.exists()) {
            petsDir.mkdirs();
            String[] defaultPets = {"rat", "dragon", "fox", "penguin", "ghost", "owl", "mushroom", "wisp", "robot", "skull"};
            for (String pet : defaultPets) {
                plugin.saveResource("pets/" + pet + ".yml", false);
            }
        }

        Logger logger = plugin.getLogger();
        File[] files = petsDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            logger.info("No pet config files found in pets/ folder");
            return;
        }

        for (File file : files) {
            String id = file.getName().replace(".yml", "").toLowerCase();

            // Skip if a Java profile is already registered
            if (manager.hasProfile(id)) {
                logger.info("Skipping config profile for pet '" + id + "' (Java profile already registered)");
                continue;
            }

            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            PetProfile profile = parsePetProfile(id, config, logger);
            if (profile != null) {
                manager.registerProfile(profile);
                logger.info("Loaded pet profile '" + id + "' from pets/" + file.getName());
            }
        }
    }

    private PetProfile parsePetProfile(String id, ConfigurationSection section, Logger logger) {
        // Build head item
        ItemStack headItem;
        if (section.contains("head-texture")) {
            headItem = HeadUtil.fromTexture(section.getString("head-texture"));
        } else if (section.contains("custom-model-data")) {
            String materialName = section.getString("head-material", "PLAYER_HEAD");
            Material material;
            try {
                material = Material.valueOf(materialName.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warning("Invalid head-material '" + materialName + "' for pet '" + id + "', using PLAYER_HEAD");
                material = Material.PLAYER_HEAD;
            }
            headItem = HeadUtil.fromCustomModelData(material, section.getInt("custom-model-data"));
        } else {
            logger.warning("Pet '" + id + "' has no head-texture or custom-model-data, skipping");
            return null;
        }

        String displayName = section.getString("display-name", id);
        String itemName = section.getString("item-name", "<white><bold>" + displayName + " Pet</bold></white>");
        List<String> itemLore = section.getStringList("item-lore");
        if (itemLore.isEmpty()) {
            itemLore = List.of("<gray>A pet companion.", "", "<yellow>Right-click <gray>to summon/dismiss");
        }
        double bobAmplitude = section.getDouble("bob-amplitude", 0.15);
        double bobFrequency = section.getDouble("bob-frequency", 0.1);
        double hoverHeight = section.getDouble("hover-height", 1.2);
        double followSpeed = section.getDouble("follow-speed", 0.25);
        double followStartDistance = section.getDouble("follow-start-distance", 3.0);
        double followStopDistance = section.getDouble("follow-stop-distance", 2.0);
        double teleportDistance = section.getDouble("teleport-distance", 16.0);
        boolean canAttack = section.getBoolean("can-attack", false);
        double attackRange = section.getDouble("attack-range", 6.0);
        double attackDamage = section.getDouble("attack-damage", 2.0);
        int attackCooldownTicks = section.getInt("attack-cooldown-ticks", 20);
        int feedCooldownTicks = section.getInt("feed-cooldown-ticks", 600);
        int heartParticleCount = section.getInt("heart-particle-count", 5);
        boolean useSmallStand = section.getBoolean("use-small-stand", true);
        boolean passiveByDefault = section.getBoolean("passive-by-default", false);

        PetAnimationType animationType = PetAnimationType.FLOAT;
        String animTypeStr = section.getString("animation-type");
        if (animTypeStr != null) {
            try {
                animationType = PetAnimationType.valueOf(animTypeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warning("Unknown animation-type '" + animTypeStr + "' for pet '" + id + "', using FLOAT");
            }
        }

        // Parse ambient sounds
        List<PetSound> ambientSounds = new ArrayList<>();
        ConfigurationSection soundsSection = section.getConfigurationSection("sounds");
        if (soundsSection != null) {
            for (String soundKey : soundsSection.getKeys(false)) {
                ConfigurationSection soundEntry = soundsSection.getConfigurationSection(soundKey);
                if (soundEntry == null) continue;

                String soundName = soundEntry.getString("sound");
                if (soundName == null) {
                    logger.warning("Sound entry '" + soundKey + "' for pet '" + id + "' has no sound field, skipping");
                    continue;
                }

                Sound sound;
                try {
                    sound = Sound.valueOf(soundName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    logger.warning("Unknown sound '" + soundName + "' for pet '" + id + "', skipping");
                    continue;
                }

                float volume = (float) soundEntry.getDouble("volume", 0.5);
                float pitch = (float) soundEntry.getDouble("pitch", 1.0);
                int chance = soundEntry.getInt("chance", 200);

                ambientSounds.add(new PetSound(sound, volume, pitch, chance));
            }
        }

        // Parse optional Model Engine fields
        String modelId = section.getString("model-id", null);
        Map<String, String> modelAnimations = new HashMap<>();
        ConfigurationSection modelAnimSection = section.getConfigurationSection("model-animations");
        if (modelAnimSection != null) {
            for (String key : modelAnimSection.getKeys(false)) {
                modelAnimations.put(key, modelAnimSection.getString(key));
            }
        }

        return new PetProfile(id, displayName, headItem,
                itemName, itemLore,
                bobAmplitude, bobFrequency, hoverHeight,
                followSpeed, followStartDistance, followStopDistance, teleportDistance,
                canAttack, attackRange, attackDamage, attackCooldownTicks,
                feedCooldownTicks, heartParticleCount, useSmallStand,
                passiveByDefault,
                animationType,
                ambientSounds,
                modelId, modelAnimations);
    }

    public void reload(PetManager manager) {
        manager.clearProfiles();
        loadAndRegister(manager);
    }
}
