package net.axther.serverCore.pet.task;

import net.axther.serverCore.pet.PetManager;
import org.bukkit.scheduler.BukkitRunnable;

public class PetTickTask extends BukkitRunnable {

    private final PetManager manager;

    public PetTickTask(PetManager manager) {
        this.manager = manager;
    }

    @Override
    public void run() {
        manager.tickAll();
    }
}
