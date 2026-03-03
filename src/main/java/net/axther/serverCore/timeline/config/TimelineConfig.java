package net.axther.serverCore.timeline.config;

import net.axther.serverCore.timeline.Timeline;
import net.axther.serverCore.timeline.TimelineManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Loads timeline definitions from the timelines/ directory.
 * Each YAML file in the directory represents one timeline.
 */
public class TimelineConfig {

    private final JavaPlugin plugin;
    private final File timelinesDir;

    public TimelineConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.timelinesDir = new File(plugin.getDataFolder(), "timelines");
    }

    /**
     * Load all timeline files from the timelines/ directory and register
     * them with the manager.
     */
    public void loadAll(TimelineManager manager) {
        if (!timelinesDir.exists()) {
            timelinesDir.mkdirs();
            saveDefaultExample();
            // After saving default, load it
        }

        File[] files = timelinesDir.listFiles((dir, name) ->
                name.endsWith(".yml") || name.endsWith(".yaml"));

        if (files == null || files.length == 0) {
            plugin.getLogger().info("No timeline files found in timelines/ directory");
            return;
        }

        int count = 0;
        for (File file : files) {
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

                String id = yaml.getString("id");
                if (id == null || id.isEmpty()) {
                    // Derive ID from filename
                    String name = file.getName();
                    id = name.substring(0, name.lastIndexOf('.'));
                }

                Timeline timeline = Timeline.fromConfig(id, yaml);
                manager.register(timeline);
                count++;
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                        "Failed to load timeline from " + file.getName(), e);
            }
        }

        plugin.getLogger().info("Loaded " + count + " timeline(s) from timelines/ directory");
    }

    /**
     * Save a default example timeline file for first-run setup.
     */
    private void saveDefaultExample() {
        File exampleFile = new File(timelinesDir, "boss-intro.yml");
        if (exampleFile.exists()) return;

        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("id", "boss-intro");
        yaml.set("loop", false);
        yaml.set("audience", "nearby");
        yaml.set("radius", 50);

        // Keyframe 0: title + sound
        yaml.set("keyframes.0", List.of(
                Map.of(
                        "type", "title",
                        "title", "<red><bold>THE WARDEN AWAKENS",
                        "subtitle", "<dark_gray>Prepare yourself...",
                        "fade-in", 10,
                        "stay", 40,
                        "fade-out", 10
                ),
                Map.of(
                        "type", "sound",
                        "sound", "ENTITY_WITHER_SPAWN",
                        "volume", 1.0,
                        "pitch", 0.5
                )
        ));

        // Keyframe 40: command
        yaml.set("keyframes.40", List.of(
                Map.of(
                        "type", "command",
                        "value", "summon minecraft:warden %x% %y% %z%"
                )
        ));

        // Keyframe 60: camera shake
        yaml.set("keyframes.60", List.of(
                Map.of(
                        "type", "camera-shake",
                        "duration", 10
                )
        ));

        try {
            yaml.save(exampleFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save default timeline example", e);
        }
    }
}
