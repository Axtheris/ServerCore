package net.axther.serverCore.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A paginated extension of {@link Menu} for displaying large lists of items.
 * Content items fill rows 0-4 (slots 0-44), while row 5 (slots 45-53) holds navigation controls.
 */
public class PaginatedMenu extends Menu {

    private static final int NAV_ROW_START = 45;
    private static final int PREV_SLOT = 45;
    private static final int BACK_SLOT = 49;
    private static final int NEXT_SLOT = 53;

    private final List<MenuItem> contentItems;
    private final int itemsPerPage;
    private int currentPage;

    private PaginatedMenu(String title, List<MenuItem> contentItems, int itemsPerPage,
                          Menu parent, int currentPage) {
        super(title, 6, new HashMap<>(), parent, 0, null, null);
        this.contentItems = contentItems;
        this.itemsPerPage = itemsPerPage;
        this.currentPage = currentPage;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getTotalPages() {
        return Math.max(1, (int) Math.ceil((double) contentItems.size() / itemsPerPage));
    }

    @Override
    public void open(Player player) {
        // Fire MenuOpenEvent (matching Menu.open() behavior)
        var openEvent = new net.axther.serverCore.api.event.MenuOpenEvent(player, null);
        Bukkit.getPluginManager().callEvent(openEvent);
        if (openEvent.isCancelled()) return;

        Map<Integer, MenuItem> pageItems = buildPageItems(player);

        Inventory inventory = Bukkit.createInventory(null, 54,
                MiniMessage.miniMessage().deserialize(getTitle() + " <gray>(" + (currentPage + 1) + "/" + getTotalPages() + ")"));

        for (Map.Entry<Integer, MenuItem> entry : pageItems.entrySet()) {
            int slot = entry.getKey();
            MenuItem item = entry.getValue();
            if (slot < 0 || slot >= 54) continue;
            if (!item.isVisibleTo(player)) continue;
            inventory.setItem(slot, item.getDisplayItem());
        }

        player.openInventory(inventory);

        MenuManager manager = MenuManager.getInstance();
        if (manager != null) {
            manager.trackMenu(player, this);
        }
    }

    @Override
    public void handleClick(Player player, int slot, ClickType clickType) {
        // Handle navigation slots
        if (slot == PREV_SLOT && currentPage > 0) {
            currentPage--;
            open(player);
            return;
        }
        if (slot == NEXT_SLOT && currentPage < getTotalPages() - 1) {
            currentPage++;
            open(player);
            return;
        }
        if (slot == BACK_SLOT && getParent() != null) {
            getParent().open(player);
            return;
        }

        // Handle content item clicks
        Map<Integer, MenuItem> pageItems = buildPageItems(player);
        MenuItem item = pageItems.get(slot);
        if (item == null) return;
        if (!item.isVisibleTo(player)) return;

        if (clickType.isRightClick() && item.getOnRightClick() != null) {
            item.getOnRightClick().accept(player);
        } else if (item.getOnClick() != null) {
            item.getOnClick().accept(player);
        }
    }

    private Map<Integer, MenuItem> buildPageItems(Player player) {
        Map<Integer, MenuItem> pageItems = new HashMap<>();

        // Place content items for current page
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, contentItems.size());

        for (int i = startIndex; i < endIndex; i++) {
            MenuItem item = contentItems.get(i);
            if (item.isVisibleTo(player)) {
                pageItems.put(i - startIndex, item);
            }
        }

        // Navigation buttons
        if (currentPage > 0) {
            pageItems.put(PREV_SLOT, createNavItem(Material.ARROW, "<yellow>Previous Page"));
        }

        if (getParent() != null) {
            pageItems.put(BACK_SLOT, createNavItem(Material.BARRIER, "<red>Back"));
        }

        if (currentPage < getTotalPages() - 1) {
            pageItems.put(NEXT_SLOT, createNavItem(Material.ARROW, "<yellow>Next Page"));
        }

        return pageItems;
    }

    private MenuItem createNavItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize(name));
        item.setItemMeta(meta);
        return MenuItem.builder(item).build();
    }

    public static PaginatedBuilder paginatedBuilder(String title) {
        return new PaginatedBuilder(title);
    }

    public static class PaginatedBuilder {

        private final String title;
        private final List<MenuItem> contentItems = new ArrayList<>();
        private int itemsPerPage = 45; // 5 rows * 9 slots
        private Menu parent;

        private PaginatedBuilder(String title) {
            this.title = title;
        }

        public PaginatedBuilder contentItems(List<MenuItem> items) {
            this.contentItems.addAll(items);
            return this;
        }

        public PaginatedBuilder addContentItem(MenuItem item) {
            this.contentItems.add(item);
            return this;
        }

        public PaginatedBuilder itemsPerPage(int itemsPerPage) {
            this.itemsPerPage = Math.max(1, Math.min(45, itemsPerPage));
            return this;
        }

        public PaginatedBuilder parent(Menu parent) {
            this.parent = parent;
            return this;
        }

        public PaginatedMenu build() {
            return new PaginatedMenu(title, new ArrayList<>(contentItems), itemsPerPage, parent, 0);
        }
    }
}
