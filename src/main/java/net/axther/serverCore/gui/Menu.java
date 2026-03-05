package net.axther.serverCore.gui;

import net.axther.serverCore.api.event.MenuOpenEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A chest inventory GUI backed by Bukkit inventories.
 * Construct via {@link Builder} and open for players with {@link #open(Player)}.
 */
public class Menu {

    private final String title;
    private final int rows;
    private final Map<Integer, MenuItem> items;
    private final Menu parent;
    private final int refreshInterval; // 0 = no refresh
    private final String menuId;      // null for code-built menus
    private final String openSound;   // null for no sound

    protected Menu(String title, int rows, Map<Integer, MenuItem> items, Menu parent,
                   int refreshInterval, String menuId, String openSound) {
        this.title = title;
        this.rows = Math.max(1, Math.min(6, rows));
        this.items = items;
        this.parent = parent;
        this.refreshInterval = refreshInterval;
        this.menuId = menuId;
        this.openSound = openSound;
    }

    public String getTitle() {
        return title;
    }

    public int getRows() {
        return rows;
    }

    public Map<Integer, MenuItem> getItems() {
        return items;
    }

    public Menu getParent() {
        return parent;
    }

    public int getRefreshInterval() {
        return refreshInterval;
    }

    public String getMenuId() {
        return menuId;
    }

    public String getOpenSound() {
        return openSound;
    }

    /**
     * Creates a Bukkit inventory, fills visible items, and opens it for the player.
     * Registers the menu with the global {@link MenuManager} if one is available.
     */
    public void open(Player player) {
        MenuOpenEvent openEvent = new MenuOpenEvent(player, menuId);
        Bukkit.getPluginManager().callEvent(openEvent);
        if (openEvent.isCancelled()) return;

        Inventory inventory = Bukkit.createInventory(null, rows * 9,
                MiniMessage.miniMessage().deserialize(title));

        for (Map.Entry<Integer, MenuItem> entry : items.entrySet()) {
            int slot = entry.getKey();
            MenuItem item = entry.getValue();

            if (slot < 0 || slot >= rows * 9) continue;
            if (!item.isVisibleTo(player)) continue;

            inventory.setItem(slot, item.getDisplayItem());
        }

        player.openInventory(inventory);

        if (openSound != null) {
            try {
                Sound sound = Sound.valueOf(openSound.toUpperCase());
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException ignored) {}
        }

        MenuManager manager = MenuManager.getInstance();
        if (manager != null) {
            manager.trackMenu(player, this);
        }
    }

    /**
     * Dispatches a click event to the appropriate {@link MenuItem} handler.
     */
    public void handleClick(Player player, int slot, ClickType clickType) {
        MenuItem item = items.get(slot);
        if (item == null) return;
        if (!item.isVisibleTo(player)) return;

        if (clickType.isRightClick() && item.getOnRightClick() != null) {
            item.getOnRightClick().accept(player);
        } else if (item.getOnClick() != null) {
            item.getOnClick().accept(player);
        }
    }

    /**
     * Re-renders dynamic and cycle items in the player's currently open inventory
     * without closing and reopening the menu.
     *
     * @param player    the player whose inventory to refresh
     * @param tickCount global tick counter used for cycle item rotation
     */
    public void refresh(Player player, int tickCount) {
        Inventory topInventory = player.getOpenInventory().getTopInventory();
        if (topInventory == null) return;

        for (Map.Entry<Integer, MenuItem> entry : items.entrySet()) {
            int slot = entry.getKey();
            MenuItem item = entry.getValue();
            if (slot < 0 || slot >= rows * 9) continue;

            // Handle cycle items
            List<ItemStack> cycleItems = item.getCycleItems();
            if (cycleItems != null && !cycleItems.isEmpty()) {
                int interval = Math.max(1, item.getCycleInterval());
                int index = (tickCount / interval) % cycleItems.size();
                topInventory.setItem(slot, cycleItems.get(index));
                continue;
            }

            // Handle dynamic items (placeholder for future PAPI resolution)
            if (item.isDynamic()) {
                if (!item.isVisibleTo(player)) {
                    topInventory.setItem(slot, null);
                } else {
                    topInventory.setItem(slot, item.getDisplayItem());
                }
            }
        }
    }

    public static Builder builder(String title) {
        return new Builder(title);
    }

    public static class Builder {

        protected String title;
        protected int rows = 3;
        protected final Map<Integer, MenuItem> items = new HashMap<>();
        protected Menu parent;
        protected int refreshInterval = 0;
        protected String menuId = null;
        protected String openSound = null;

        protected Builder(String title) {
            this.title = title;
        }

        public Builder rows(int rows) {
            this.rows = rows;
            return this;
        }

        public Builder item(int slot, MenuItem item) {
            items.put(slot, item);
            return this;
        }

        public Builder item(int slot, ItemStack displayItem, java.util.function.Consumer<Player> onClick) {
            items.put(slot, MenuItem.builder(displayItem).onClick(onClick).build());
            return this;
        }

        public Builder parent(Menu parent) {
            this.parent = parent;
            return this;
        }

        public Builder refreshInterval(int refreshInterval) {
            this.refreshInterval = refreshInterval;
            return this;
        }

        public Builder menuId(String menuId) {
            this.menuId = menuId;
            return this;
        }

        public Builder openSound(String openSound) {
            this.openSound = openSound;
            return this;
        }

        public Builder fillBorder(ItemStack filler) {
            int size = rows * 9;
            MenuItem fillerItem = MenuItem.builder(filler).build();
            for (int i = 0; i < 9; i++) {
                items.putIfAbsent(i, fillerItem);
                items.putIfAbsent(size - 9 + i, fillerItem);
            }
            for (int i = 9; i < size - 9; i += 9) {
                items.putIfAbsent(i, fillerItem);
                items.putIfAbsent(i + 8, fillerItem);
            }
            return this;
        }

        public Menu build() {
            return new Menu(title, rows, new HashMap<>(items), parent,
                    refreshInterval, menuId, openSound);
        }
    }
}
