package net.axther.serverCore.task;

import net.axther.serverCore.cosmetic.CosmeticManager;
import net.axther.serverCore.cosmetic.data.CosmeticStore;
import net.axther.serverCore.pet.data.PetStore;
import net.axther.serverCore.quest.QuestManager;
import net.axther.serverCore.quest.data.QuestStore;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * PERS-01: Periodic save flush task. Runs every 6000 ticks (~5 minutes).
 * Checks each store's dirty flag and triggers snapshot-then-async write if dirty.
 * If the store is clean, the flush is a no-op (no disk I/O).
 */
public class SaveFlushTask extends BukkitRunnable {

    private final CosmeticStore cosmeticStore;
    private final CosmeticManager cosmeticManager;
    private final PetStore petStore;
    private final QuestStore questStore;
    private final QuestManager questManager;

    public SaveFlushTask(CosmeticStore cosmeticStore, CosmeticManager cosmeticManager,
                         PetStore petStore,
                         QuestStore questStore, QuestManager questManager) {
        this.cosmeticStore = cosmeticStore;
        this.cosmeticManager = cosmeticManager;
        this.petStore = petStore;
        this.questStore = questStore;
        this.questManager = questManager;
    }

    @Override
    public void run() {
        if (cosmeticStore != null && cosmeticManager != null) {
            cosmeticStore.flushIfDirty(cosmeticManager);
        }
        if (petStore != null) {
            petStore.flushIfDirty();
        }
        if (questStore != null && questManager != null) {
            questStore.flushIfDirty(questManager);
        }
    }
}
