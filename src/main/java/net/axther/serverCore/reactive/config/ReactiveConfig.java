package net.axther.serverCore.reactive.config;

import net.axther.serverCore.reactive.ReactiveManager;
import net.axther.serverCore.reactive.ReactiveRule;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Loads reactive rules from reactive.yml.
 * Creates a default configuration file on first run.
 */
public class ReactiveConfig {

    private final JavaPlugin plugin;
    private final File configFile;

    public ReactiveConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "reactive.yml");
    }

    /**
     * Loads all rules from reactive.yml and registers them with the manager.
     * Creates the default config if it does not exist.
     */
    public void loadAll(ReactiveManager manager) {
        if (!configFile.exists()) {
            createDefaults();
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        Logger logger = plugin.getLogger();

        ConfigurationSection rulesSection = config.getConfigurationSection("rules");
        if (rulesSection == null) {
            logger.info("No reactive rules found in reactive.yml");
            return;
        }

        int count = 0;
        for (String ruleId : rulesSection.getKeys(false)) {
            ConfigurationSection ruleSection = rulesSection.getConfigurationSection(ruleId);
            if (ruleSection == null) continue;

            ReactiveRule rule = ReactiveRule.fromConfig(ruleId, ruleSection, logger);
            if (rule != null) {
                manager.register(rule);
                count++;
            }
        }

        logger.info("Loaded " + count + " reactive rule(s) from reactive.yml");
    }

    /**
     * Creates the default reactive.yml with example rules.
     */
    private void createDefaults() {
        plugin.getDataFolder().mkdirs();

        YamlConfiguration config = new YamlConfiguration();

        // Night glow rule
        ConfigurationSection rules = config.createSection("rules");

        ConfigurationSection nightGlow = rules.createSection("night-glow");
        nightGlow.set("conditions", java.util.List.of(
                java.util.Map.of("type", "time-of-day", "range", java.util.List.of(13000, 23000))
        ));
        nightGlow.set("targets", java.util.List.of("pets", "cosmetics"));
        nightGlow.set("effects", java.util.List.of(
                java.util.Map.of("type", "glow", "color", "AQUA"),
                java.util.Map.of("type", "particle", "particle", "END_ROD", "count", 2)
        ));

        // Rain umbrella rule
        ConfigurationSection rainUmbrella = rules.createSection("rain-umbrella");
        rainUmbrella.set("conditions", java.util.List.of(
                java.util.Map.of("type", "weather", "value", "rain")
        ));
        rainUmbrella.set("targets", java.util.List.of("pets"));
        rainUmbrella.set("effects", java.util.List.of(
                java.util.Map.of("type", "swap-item", "slot", "head", "material", "RED_MUSHROOM")
        ));

        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create default reactive.yml: " + e.getMessage());
        }
    }
}
