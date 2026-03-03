package net.axther.serverCore.npc.task;

import net.axther.serverCore.npc.NPCManager;
import org.bukkit.scheduler.BukkitRunnable;

public class NPCTickTask extends BukkitRunnable {

    private final NPCManager manager;

    public NPCTickTask(NPCManager manager) {
        this.manager = manager;
    }

    @Override
    public void run() {
        manager.tickAll();
    }
}
