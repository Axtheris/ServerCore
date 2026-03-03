package net.axther.serverCore.particle.config;

import net.axther.serverCore.particle.*;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class EmitterConfig {

    private final JavaPlugin plugin;
    private final File file;

    public EmitterConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "emitters.yml");
    }

    public void loadAll(EmitterManager manager) {
        if (!file.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection emitters = yaml.getConfigurationSection("emitters");
        if (emitters == null) return;

        for (String id : emitters.getKeys(false)) {
            ConfigurationSection sec = emitters.getConfigurationSection(id);
            if (sec == null) continue;

            try {
                String worldName = sec.getString("world", "world");
                int x = sec.getInt("x");
                int y = sec.getInt("y");
                int z = sec.getInt("z");

                Particle particle = Particle.valueOf(sec.getString("particle", "FLAME"));
                EmitterPattern pattern = EmitterPattern.valueOf(sec.getString("pattern", "POINT"));
                double radius = sec.getDouble("radius", 1.0);
                double height = sec.getDouble("height", 2.0);
                double speed = sec.getDouble("speed", 0.02);
                int count = sec.getInt("count", 2);
                int interval = sec.getInt("interval", 2);
                float size = (float) sec.getDouble("size", 1.0);

                Color color = null;
                ConfigurationSection colorSec = sec.getConfigurationSection("color");
                if (colorSec != null) {
                    color = Color.fromRGB(colorSec.getInt("r", 255), colorSec.getInt("g", 255), colorSec.getInt("b", 255));
                }

                Material blockMaterial = null;
                String matStr = sec.getString("block-material");
                if (matStr != null && !matStr.equals("null")) {
                    try {
                        blockMaterial = Material.valueOf(matStr);
                    } catch (IllegalArgumentException ignored) {}
                }

                EmitterData data = new EmitterData(particle, pattern, radius, height, speed, count, interval, color, size, blockMaterial);
                EmitterInstance instance = new EmitterInstance(id, worldName, x, y, z, data);
                manager.registerExisting(instance);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load emitter '" + id + "'", e);
            }
        }

        plugin.getLogger().info("Loaded " + manager.getAllEmitters().size() + " particle emitters");
    }

    public void saveAll(EmitterManager manager) {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection emitters = yaml.createSection("emitters");

        for (EmitterInstance instance : manager.getAllEmitters()) {
            ConfigurationSection sec = emitters.createSection(instance.getId());
            EmitterData data = instance.getData();

            sec.set("world", instance.getWorldName());
            sec.set("x", instance.getBlockX());
            sec.set("y", instance.getBlockY());
            sec.set("z", instance.getBlockZ());
            sec.set("particle", data.particle().name());
            sec.set("pattern", data.pattern().name());
            sec.set("radius", data.radius());
            sec.set("height", data.height());
            sec.set("speed", data.speed());
            sec.set("count", data.count());
            sec.set("interval", data.interval());
            sec.set("size", data.size());

            if (data.color() != null) {
                ConfigurationSection colorSec = sec.createSection("color");
                colorSec.set("r", data.color().getRed());
                colorSec.set("g", data.color().getGreen());
                colorSec.set("b", data.color().getBlue());
            } else {
                sec.set("color", null);
            }

            sec.set("block-material", data.blockMaterial() != null ? data.blockMaterial().name() : null);
        }

        try {
            file.getParentFile().mkdirs();
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save emitters.yml", e);
        }
    }
}
