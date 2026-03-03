package net.axther.serverCore.gui;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Loads menu layouts from a YAML configuration file.
 * <p>
 * Each menu definition can specify a title, row count, and a map of slot positions
 * to display items. Actions are not loaded from config -- they must be bound in code
 * after loading the layout.
 * <p>
 * Example menus.yml:
 * <pre>
 * menus:
 *   main-menu:
 *     title: "<gold>Main Menu"
 *     rows: 3
 *     items:
 *       13:
 *         material: DIAMOND
 *         name: "<aqua>Shop"
 *         lore:
 *           - "<gray>Click to open the shop"
 * </pre>
 */
public class MenuConfig {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, MenuLayout> layouts = new HashMap<>();

    public MenuConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "menus.yml");
    }

    /**
     * Loads all menu layouts from menus.yml.
     */
    public void load() {
        layouts.clear();

        if (!file.exists()) return;

        Logger logger = plugin.getLogger();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection menusSection = yaml.getConfigurationSection("menus");
        if (menusSection == null) return;

        for (String menuId : menusSection.getKeys(false)) {
            ConfigurationSection menuSection = menusSection.getConfigurationSection(menuId);
            if (menuSection == null) continue;

            String title = menuSection.getString("title", menuId);
            int rows = menuSection.getInt("rows", 3);
            Map<Integer, ItemStack> slotItems = new HashMap<>();

            ConfigurationSection itemsSection = menuSection.getConfigurationSection("items");
            if (itemsSection != null) {
                for (String slotKey : itemsSection.getKeys(false)) {
                    int slot;
                    try {
                        slot = Integer.parseInt(slotKey);
                    } catch (NumberFormatException e) {
                        logger.warning("Invalid slot '" + slotKey + "' in menu '" + menuId + "'");
                        continue;
                    }

                    ConfigurationSection itemSection = itemsSection.getConfigurationSection(slotKey);
                    if (itemSection == null) continue;

                    ItemStack item = parseItem(itemSection);
                    if (item != null) {
                        slotItems.put(slot, item);
                    }
                }
            }

            layouts.put(menuId, new MenuLayout(title, rows, slotItems));
            logger.info("Loaded menu layout: " + menuId);
        }
    }

    /**
     * Returns a loaded layout by its config ID, or null if not found.
     */
    public MenuLayout getLayout(String menuId) {
        return layouts.get(menuId);
    }

    /**
     * Returns all loaded layout IDs.
     */
    public java.util.Set<String> getLayoutIds() {
        return layouts.keySet();
    }

    private ItemStack parseItem(ConfigurationSection section) {
        String materialName = section.getString("material");
        if (materialName == null) return null;

        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Unknown material: " + materialName);
            return null;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String name = section.getString("name");
        if (name != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize(name));
        }

        List<String> loreLines = section.getStringList("lore");
        if (!loreLines.isEmpty()) {
            meta.lore(loreLines.stream()
                    .map(line -> MiniMessage.miniMessage().deserialize(line))
                    .map(component -> (net.kyori.adventure.text.Component) component)
                    .toList());
        }

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Represents a loaded menu layout from configuration.
     */
    public record MenuLayout(String title, int rows, Map<Integer, ItemStack> items) {
    }
}
