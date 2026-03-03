package net.axther.serverCore.cosmetic.task;

import net.axther.serverCore.cosmetic.CosmeticManager;
import org.bukkit.scheduler.BukkitRunnable;

public class CosmeticTickTask extends BukkitRunnable {

    private final CosmeticManager manager;

    public CosmeticTickTask(CosmeticManager manager) {
        this.manager = manager;
    }

    @Override
    public void run() {
        manager.tickAll();
    }
}
