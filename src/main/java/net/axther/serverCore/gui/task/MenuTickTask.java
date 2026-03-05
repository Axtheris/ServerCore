package net.axther.serverCore.gui.task;

import net.axther.serverCore.gui.Menu;
import net.axther.serverCore.gui.MenuManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;

/**
 * Ticks all open menus every server tick, refreshing dynamic and cycle items
 * at each menu's configured refresh interval.
 */
public class MenuTickTask extends BukkitRunnable {

    private final MenuManager menuManager;
    private int tickCount = 0;

    public MenuTickTask(MenuManager menuManager) {
        this.menuManager = menuManager;
    }

    @Override
    public void run() {
        tickCount++;
        for (Map.Entry<UUID, Menu> entry : Map.copyOf(menuManager.getOpenMenus()).entrySet()) {
            Menu menu = entry.getValue();
            if (menu.getRefreshInterval() <= 0) continue;
            if (tickCount % menu.getRefreshInterval() != 0) continue;

            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                menu.refresh(player, tickCount);
            }
        }
    }
}
