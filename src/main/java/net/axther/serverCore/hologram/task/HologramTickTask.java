package net.axther.serverCore.hologram.task;

import net.axther.serverCore.hologram.HologramManager;
import org.bukkit.scheduler.BukkitRunnable;

public class HologramTickTask extends BukkitRunnable {

    private final HologramManager manager;
    private int tickCount;

    public HologramTickTask(HologramManager manager) {
        this.manager = manager;
        this.tickCount = 0;
    }

    @Override
    public void run() {
        tickCount++;
        manager.tickAll(tickCount);
        manager.refreshPlaceholders(tickCount);
        if (manager.getVisibilityTracker() != null) {
            manager.getVisibilityTracker().update(tickCount);
        }
    }
}
