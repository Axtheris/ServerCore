package net.axther.serverCore.gui;

import net.axther.serverCore.api.event.MenuCloseEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Bukkit listener that intercepts inventory interactions for tracked {@link Menu} instances.
 * Cancels all clicks/drags within menus and dispatches to the menu's click handler.
 */
public class MenuListener implements Listener {

    private final MenuManager menuManager;

    public MenuListener(MenuManager menuManager) {
        this.menuManager = menuManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Menu menu = menuManager.getOpenMenu(player.getUniqueId());
        if (menu == null) return;

        event.setCancelled(true);

        // Only handle clicks in the top inventory
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;

        menu.handleClick(player, event.getSlot(), event.getClick());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Menu menu = menuManager.getOpenMenu(player.getUniqueId());
        if (menu == null) return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        Menu menu = menuManager.getOpenMenu(player.getUniqueId());
        menuManager.closeMenu(player);

        if (menu != null) {
            Bukkit.getPluginManager().callEvent(new MenuCloseEvent(player, menu.getMenuId()));
        }
    }
}
