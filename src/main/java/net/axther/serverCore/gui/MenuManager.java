package net.axther.serverCore.gui;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks which {@link Menu} each player currently has open.
 * A single instance is held as a static singleton so that {@link Menu#open(Player)}
 * can register itself without requiring the manager to be passed around.
 */
public class MenuManager {

    private static MenuManager instance;

    private final Map<UUID, Menu> openMenus = new HashMap<>();

    public MenuManager() {
        instance = this;
    }

    static MenuManager getInstance() {
        return instance;
    }

    public void trackMenu(Player player, Menu menu) {
        openMenus.put(player.getUniqueId(), menu);
    }

    public void closeMenu(Player player) {
        openMenus.remove(player.getUniqueId());
    }

    public Menu getOpenMenu(UUID playerUuid) {
        return openMenus.get(playerUuid);
    }
}
