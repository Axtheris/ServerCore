package net.axther.serverCore.gui;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Loads menu layouts from a YAML configuration file.
 * <p>
 * Each menu definition can specify a title, row count, open sound, refresh interval,
 * and a map of slot positions to item definitions with actions, conditions, and
 * dynamic display support.
 * <p>
 * Example menus.yml:
 * <pre>
 * menus:
 *   main-menu:
 *     title: "&lt;gold&gt;Main Menu"
 *     rows: 3
 *     open-sound: BLOCK_CHEST_OPEN
 *     refresh-interval: 20
 *     items:
 *       13:
 *         material: DIAMOND
 *         name: "&lt;aqua&gt;Shop"
 *         lore:
 *           - "&lt;gray&gt;Click to open the shop"
 *         actions:
 *           - type: open_menu
 *             value: shop-menu
 *         right-actions:
 *           - type: message
 *             value: "&lt;yellow&gt;Right-clicked!"
 *         view-condition: "permission:shop.access"
 *         dynamic: false
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
            String openSound = menuSection.getString("open-sound");
            int refreshInterval = menuSection.getInt("refresh-interval", 0);
            Map<Integer, MenuItemDef> slotItems = new HashMap<>();

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
                    if (item == null) continue;

                    List<MenuAction> actions = parseActions(itemSection, "actions");
                    List<MenuAction> rightActions = parseActions(itemSection, "right-actions");
                    String viewCondition = itemSection.getString("view-condition");
                    boolean dynamic = itemSection.getBoolean("dynamic", false);

                    String nameTemplate = dynamic ? itemSection.getString("name") : null;
                    List<String> loreTemplates = dynamic ? itemSection.getStringList("lore") : List.of();

                    List<String> cycleMaterials = itemSection.getStringList("cycle");
                    int cycleInterval = itemSection.getInt("cycle-interval", 20);

                    slotItems.put(slot, new MenuItemDef(item, actions, rightActions,
                            viewCondition, dynamic, nameTemplate, loreTemplates,
                            cycleMaterials, cycleInterval));
                }
            }

            layouts.put(menuId, new MenuLayout(title, rows, slotItems, openSound, refreshInterval));
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
     * Parses a list of action maps from a configuration section.
     *
     * @param section the item configuration section
     * @param key     the key containing the action list (e.g. "actions" or "right-actions")
     * @return a list of parsed MenuAction instances
     */
    private List<MenuAction> parseActions(ConfigurationSection section, String key) {
        List<Map<?, ?>> actionMaps = section.getMapList(key);
        List<MenuAction> actions = new ArrayList<>();
        for (Map<?, ?> map : actionMaps) {
            String type = String.valueOf(map.get("type"));
            String value = map.containsKey("value") ? String.valueOf(map.get("value")) : "";
            actions.add(MenuAction.parse(type, value));
        }
        return actions;
    }

    /**
     * Parses a view condition string into a player predicate.
     * <p>
     * Supported formats:
     * <ul>
     *   <li>{@code permission:<node>} — checks if the player has the given permission</li>
     * </ul>
     * Unknown condition formats return null (always visible).
     *
     * @param condition the condition string, or null
     * @return a predicate, or null if always visible
     */
    private Predicate<Player> parseViewCondition(String condition) {
        if (condition == null) return null;
        if (condition.startsWith("permission:")) {
            String perm = condition.substring("permission:".length());
            return player -> player.hasPermission(perm);
        }
        return null;
    }

    /**
     * Builds a {@link Menu} from a loaded layout by its config ID.
     * Each item is fully wired with click actions, right-click actions,
     * view conditions, and dynamic/cycle properties.
     * Returns null if the layout is not found.
     *
     * @param menuId the menu identifier from menus.yml
     * @return the built Menu, or null
     */
    public Menu buildMenu(String menuId) {
        MenuLayout layout = layouts.get(menuId);
        if (layout == null) return null;

        MenuConfig self = this;
        Menu.Builder builder = Menu.builder(layout.title()).rows(layout.rows());

        for (var entry : layout.items().entrySet()) {
            MenuItemDef def = entry.getValue();

            // Convert cycle material names to ItemStacks
            List<ItemStack> cycleItems = new ArrayList<>();
            for (String matName : def.cycleMaterials()) {
                try {
                    Material mat = Material.valueOf(matName.toUpperCase());
                    cycleItems.add(new ItemStack(mat));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Unknown cycle material: " + matName);
                }
            }

            MenuItem.Builder itemBuilder = MenuItem.builder(def.displayItem())
                    .actions(def.actions())
                    .rightActions(def.rightActions())
                    .viewCondition(parseViewCondition(def.viewCondition()))
                    .dynamic(def.dynamic())
                    .cycleItems(cycleItems)
                    .cycleInterval(def.cycleInterval());

            if (def.nameTemplate() != null) {
                itemBuilder.nameTemplate(def.nameTemplate());
            }
            if (!def.loreTemplates().isEmpty()) {
                itemBuilder.loreTemplates(def.loreTemplates());
            }

            // Wire left-click actions
            if (!def.actions().isEmpty()) {
                List<MenuAction> leftActions = def.actions();
                itemBuilder.onClick(player -> {
                    Menu currentMenu = MenuManager.getInstance() != null
                            ? MenuManager.getInstance().getOpenMenu(player.getUniqueId()) : null;
                    for (MenuAction action : leftActions) {
                        action.execute(player, currentMenu, self);
                    }
                });
            }

            // Wire right-click actions
            if (!def.rightActions().isEmpty()) {
                List<MenuAction> rActions = def.rightActions();
                itemBuilder.onRightClick(player -> {
                    Menu currentMenu = MenuManager.getInstance() != null
                            ? MenuManager.getInstance().getOpenMenu(player.getUniqueId()) : null;
                    for (MenuAction action : rActions) {
                        action.execute(player, currentMenu, self);
                    }
                });
            }

            builder.item(entry.getKey(), itemBuilder.build());
        }

        return builder.build();
    }

    /**
     * Represents a single item definition loaded from menu configuration.
     *
     * @param displayItem    the base display ItemStack
     * @param actions        left-click actions
     * @param rightActions   right-click actions
     * @param viewCondition  condition string for visibility (e.g. "permission:vip.access")
     * @param dynamic        whether this item updates dynamically
     * @param nameTemplate   MiniMessage name template (if dynamic)
     * @param loreTemplates  MiniMessage lore templates (if dynamic)
     * @param cycleMaterials list of material names to cycle through
     * @param cycleInterval  ticks between material cycles
     */
    public record MenuItemDef(ItemStack displayItem, List<MenuAction> actions,
                               List<MenuAction> rightActions, String viewCondition,
                               boolean dynamic, String nameTemplate, List<String> loreTemplates,
                               List<String> cycleMaterials, int cycleInterval) {}

    /**
     * Represents a loaded menu layout from configuration.
     *
     * @param title           the menu title (MiniMessage format)
     * @param rows            the number of inventory rows (1-6)
     * @param items           slot-to-item-definition mapping
     * @param openSound       sound to play when the menu is opened (nullable)
     * @param refreshInterval ticks between automatic refreshes (0 = no refresh)
     */
    public record MenuLayout(String title, int rows, Map<Integer, MenuItemDef> items,
                              String openSound, int refreshInterval) {}
}
