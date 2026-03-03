package net.axther.serverCore.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ServerCoreConfig {

    private final JavaPlugin plugin;
    private FileConfiguration config;

    public ServerCoreConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
    }

    /**
     * Check whether a system is enabled in config.yml.
     *
     * @param systemName the system key (e.g. "cosmetics", "pets", "gui")
     * @return true if the system is enabled or if the key is missing (enabled by default)
     */
    public boolean isSystemEnabled(String systemName) {
        return config.getBoolean("systems." + systemName + ".enabled", true);
    }

    /**
     * Reload config.yml from disk.
     */
    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }
}
