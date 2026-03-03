package net.axther.serverCore.npc.task;

import net.axther.serverCore.npc.NPCManager;
import net.axther.serverCore.npc.render.NPCViewTracker;
import org.bukkit.scheduler.BukkitRunnable;

public class NPCTickTask extends BukkitRunnable {

    private final NPCManager manager;
    private final NPCViewTracker viewTracker;
    private int tickCount = 0;

    public NPCTickTask(NPCManager manager, NPCViewTracker viewTracker) {
        this.manager = manager;
        this.viewTracker = viewTracker;
    }

    @Override
    public void run() {
        tickCount++;

        // Head rotation every tick
        manager.tickAll();

        // View distance checks every 5 ticks
        if (tickCount % 5 == 0) {
            viewTracker.updateAll();
        }
    }
}
