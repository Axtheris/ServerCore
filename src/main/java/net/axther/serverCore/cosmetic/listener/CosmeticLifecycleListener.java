package net.axther.serverCore.cosmetic.listener;

import net.axther.serverCore.cosmetic.CosmeticManager;
import net.axther.serverCore.cosmetic.data.CosmeticStore;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class CosmeticLifecycleListener implements Listener {

    private final CosmeticManager manager;
    private final CosmeticStore store;

    public CosmeticLifecycleListener(CosmeticManager manager, CosmeticStore store) {
        this.manager = manager;
        this.store = store;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (manager.hasCosmetics(entity.getUniqueId())) {
            manager.removeCosmetics(entity.getUniqueId());
        }
    }

    @EventHandler
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (manager.hasCosmetics(entity.getUniqueId())) {
                manager.removeCosmetics(entity.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        if (store == null) return;

        for (Entity entity : event.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;

            UUID mobUuid = living.getUniqueId();
            CosmeticStore.PendingCosmetic pending = store.getPending().get(mobUuid);
            if (pending == null) continue;

            // Verify a profile exists for this entity type
            if (manager.getProfile(living.getType()) == null) continue;

            List<ItemStack> items = pending.items();
            for (ItemStack item : items) {
                manager.applyCosmetic(living, item);
            }
            store.removePending(mobUuid);
        }
    }

    @EventHandler
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        if (manager.isCosmeticStand(event.getRightClicked().getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
