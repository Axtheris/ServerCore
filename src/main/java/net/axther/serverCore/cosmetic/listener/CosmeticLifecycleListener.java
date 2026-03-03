package net.axther.serverCore.cosmetic.listener;

import net.axther.serverCore.cosmetic.CosmeticManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;

public class CosmeticLifecycleListener implements Listener {

    private final CosmeticManager manager;

    public CosmeticLifecycleListener(CosmeticManager manager) {
        this.manager = manager;
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
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        if (manager.isCosmeticStand(event.getRightClicked().getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
