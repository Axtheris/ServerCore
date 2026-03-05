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

                // Visual options
                hologram.setBillboard(sec.getString("billboard", "CENTER"));
                hologram.setTextShadow(sec.getBoolean("text-shadow", false));
                hologram.setBackground(sec.getString("background", null));
                hologram.setLineWidth(sec.getInt("line-width", 200));
                hologram.setSeeThrough(sec.getBoolean("see-through", false));
                hologram.setTextAlignment(sec.getString("alignment", "CENTER"));
                hologram.setViewRange((float) sec.getDouble("view-range", 1.0));

                // Dynamic content
                hologram.setUpdateInterval(sec.getInt("update-interval", 20));

                // Click cooldown
                hologram.setClickCooldown(sec.getInt("click-cooldown", 20));

                // Conditions
                List<?> condList = sec.getList("conditions");
                if (condList != null) {
                    for (Object obj : condList) {
                        if (obj instanceof java.util.Map<?, ?> map) {
                            String condType = map.get("type") != null ? String.valueOf(map.get("type")) : "";
                            String condValue = map.get("value") != null ? String.valueOf(map.get("value")) : "";
                            String condEquals = map.containsKey("equals") ? String.valueOf(map.get("equals")) : null;
                            String condMin = map.containsKey("min") ? String.valueOf(map.get("min")) : null;
                            hologram.getConditions().add(
                                    net.axther.serverCore.hologram.condition.HologramCondition.parse(
                                            condType, condValue, condEquals, condMin));
                        }
                    }
                }

                // Actions
                List<?> actList = sec.getList("actions");
                if (actList != null) {
                    for (Object obj : actList) {
                        if (obj instanceof java.util.Map<?, ?> map) {
                            String actType = map.get("type") != null ? String.valueOf(map.get("type")) : "";
                            String actValue = map.get("value") != null ? String.valueOf(map.get("value")) : "";
                            hologram.getActions().add(
                                    net.axther.serverCore.hologram.action.HologramAction.parse(actType, actValue));
                        }
                    }
                }

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

            // Visual options
            sec.set("billboard", hologram.getBillboard());
            sec.set("text-shadow", hologram.isTextShadow());
            if (hologram.getBackground() != null) {
                sec.set("background", hologram.getBackground());
            }
            sec.set("line-width", hologram.getLineWidth());
            sec.set("see-through", hologram.isSeeThrough());
            sec.set("alignment", hologram.getTextAlignment());
            sec.set("view-range", (double) hologram.getViewRange());
            sec.set("update-interval", hologram.getUpdateInterval());
            sec.set("click-cooldown", hologram.getClickCooldown());

            // Conditions
            if (!hologram.getConditions().isEmpty()) {
                List<java.util.Map<String, Object>> condMaps = new java.util.ArrayList<>();
                for (var cond : hologram.getConditions()) {
                    java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("type", cond.getType());
                    m.put("value", cond.getValue());
                    if (cond.getEquals() != null) m.put("equals", cond.getEquals());
                    if (cond.getMin() != null) m.put("min", cond.getMin());
                    condMaps.add(m);
                }
                sec.set("conditions", condMaps);
            }

            // Actions
            if (!hologram.getActions().isEmpty()) {
                List<java.util.Map<String, Object>> actMaps = new java.util.ArrayList<>();
                for (var act : hologram.getActions()) {
                    java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("type", act.getType());
                    m.put("value", act.getValue());
                    actMaps.add(m);
                }
                sec.set("actions", actMaps);
            }
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

        ConfigurationSection advanced = section.createSection("vip-portal");
        advanced.set("world", "world");
        advanced.set("x", 10.5);
        advanced.set("y", 70.0);
        advanced.set("z", 10.5);
        advanced.set("animation", "PULSE");
        advanced.set("lines", List.of(
                "<rainbow>VIP Portal</rainbow>",
                "<gray>Click to teleport"
        ));
        advanced.set("billboard", "CENTER");
        advanced.set("text-shadow", true);
        advanced.set("background", "#80000000");
        advanced.set("update-interval", 40);
        advanced.set("click-cooldown", 40);
        advanced.set("conditions", List.of(
                java.util.Map.of("type", "permission", "value", "vip.access")
        ));
        advanced.set("actions", List.of(
                java.util.Map.of("type", "command", "value", "warp vip %player%"),
                java.util.Map.of("type", "sound", "value", "ENTITY_ENDERMAN_TELEPORT")
        ));

        try {
            file.getParentFile().mkdirs();
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save default holograms.yml", e);
        }
    }
}
