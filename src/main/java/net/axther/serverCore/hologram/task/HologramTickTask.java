package net.axther.serverCore.hologram.task;

import net.axther.serverCore.hologram.HologramManager;
import net.axther.serverCore.hologram.listener.HologramInteractListener;
import org.bukkit.scheduler.BukkitRunnable;

public class HologramTickTask extends BukkitRunnable {

    private final HologramManager manager;
    private final HologramInteractListener interactListener;
    private int tickCount;

    public HologramTickTask(HologramManager manager, HologramInteractListener interactListener) {
        this.manager = manager;
        this.interactListener = interactListener;
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
        // MEM-01: Sweep expired cooldown entries every 6000 ticks (~5 minutes).
        if (interactListener != null) {
            interactListener.sweepCooldowns(tickCount);
        }
    }
}
