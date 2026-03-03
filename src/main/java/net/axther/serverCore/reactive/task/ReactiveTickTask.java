package net.axther.serverCore.reactive.task;

import net.axther.serverCore.cosmetic.CosmeticManager;
import net.axther.serverCore.pet.PetManager;
import net.axther.serverCore.reactive.ReactiveManager;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * BukkitRunnable that evaluates all reactive rules every 20 ticks (1 second).
 * This keeps reactive effects responsive without excessive per-tick overhead.
 */
public class ReactiveTickTask extends BukkitRunnable {

    private final ReactiveManager reactiveManager;
    private final CosmeticManager cosmeticManager;
    private final PetManager petManager;

    public ReactiveTickTask(ReactiveManager reactiveManager, CosmeticManager cosmeticManager, PetManager petManager) {
        this.reactiveManager = reactiveManager;
        this.cosmeticManager = cosmeticManager;
        this.petManager = petManager;
    }

    @Override
    public void run() {
        reactiveManager.evaluate(cosmeticManager, petManager);
    }
}
