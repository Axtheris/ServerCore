package net.axther.serverCore.hologram.config;

import net.axther.serverCore.hologram.Hologram;
import net.axther.serverCore.hologram.HologramAnimation;
import net.axther.serverCore.hologram.HologramManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

public class HologramConfig {

    private final JavaPlugin plugin;
    private final File file;

    public HologramConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "holograms.yml");
    }

    public void loadAll(HologramManager manager) {
        if (!file.exists()) {
            saveDefault();
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("holograms");
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            ConfigurationSection sec = section.getConfigurationSection(id);
            if (sec == null) continue;

            try {
                String worldName = sec.getString("world", "world");
                double x = sec.getDouble("x");
                double y = sec.getDouble("y");
                double z = sec.getDouble("z");

                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    plugin.getLogger().warning("Hologram '" + id + "' references unknown world '" + worldName + "', skipping");
                    continue;
                }

                Location location = new Location(world, x, y, z);

                List<String> lines = sec.getStringList("lines");
                if (lines.isEmpty()) {
                    lines = List.of("<white>" + id + "</white>");
                }

                HologramAnimation animation = HologramAnimation.NONE;
                String animStr = sec.getString("animation");
                if (animStr != null) {
                    try {
                        animation = HologramAnimation.valueOf(animStr.toUpperCase());
                    } catch (IllegalArgumentException ignored) {}
                }

                double bobAmplitude = sec.getDouble("bob-amplitude", 0.1);
                double bobFrequency = sec.getDouble("bob-frequency", 0.08);

                Hologram hologram = new Hologram(id, location, lines, animation, bobAmplitude, bobFrequency);
                manager.register(hologram);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load hologram '" + id + "'", e);
            }
        }

        plugin.getLogger().info("Loaded " + manager.getAll().size() + " holograms");
    }

    public void saveAll(HologramManager manager) {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection section = yaml.createSection("holograms");

        for (Hologram hologram : manager.getAll()) {
            ConfigurationSection sec = section.createSection(hologram.getId());
            Location loc = hologram.getLocation();

            sec.set("world", hologram.getWorldName());
            sec.set("x", loc.getX());
            sec.set("y", loc.getY());
            sec.set("z", loc.getZ());
            sec.set("animation", hologram.getAnimation().name());
            sec.set("bob-amplitude", hologram.getBobAmplitude());
            sec.set("bob-frequency", hologram.getBobFrequency());
            sec.set("lines", hologram.getLines());
        }

        try {
            file.getParentFile().mkdirs();
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save holograms.yml", e);
        }
    }

    private void saveDefault() {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection section = yaml.createSection("holograms");

        // Write an example entry as a comment-like reference
        ConfigurationSection example = section.createSection("spawn-welcome");
        example.set("world", "world");
        example.set("x", 0.5);
        example.set("y", 68.0);
        example.set("z", 0.5);
        example.set("animation", "BOB");
        example.set("bob-amplitude", 0.1);
        example.set("bob-frequency", 0.08);
        example.set("lines", List.of(
                "<gradient:#FF6B6B:#FFE66D><bold>Welcome</bold></gradient>",
                "<gray>Type /help"
        ));

        try {
            file.getParentFile().mkdirs();
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save default holograms.yml", e);
        }
    }
}
