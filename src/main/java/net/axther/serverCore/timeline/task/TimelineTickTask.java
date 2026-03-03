package net.axther.serverCore.timeline.task;

import net.axther.serverCore.timeline.TimelineManager;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Tick task that advances all active timeline instances every server tick.
 */
public class TimelineTickTask extends BukkitRunnable {

    private final TimelineManager manager;

    public TimelineTickTask(TimelineManager manager) {
        this.manager = manager;
    }

    @Override
    public void run() {
        manager.tickAll();
    }
}
