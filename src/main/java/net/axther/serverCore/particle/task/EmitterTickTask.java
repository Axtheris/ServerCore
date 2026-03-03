package net.axther.serverCore.particle.task;

import net.axther.serverCore.particle.EmitterManager;
import org.bukkit.scheduler.BukkitRunnable;

public class EmitterTickTask extends BukkitRunnable {

    private final EmitterManager manager;

    public EmitterTickTask(EmitterManager manager) {
        this.manager = manager;
    }

    @Override
    public void run() {
        manager.tickAll();
    }
}
