package net.axther.serverCore.pet.listener;

import net.axther.serverCore.pet.PetManager;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;

public class PetLifecycleListener implements Listener {

    private final PetManager manager;

    public PetLifecycleListener(PetManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        manager.dismissAll(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        manager.dismissAll(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (manager.isPetStand(entity.getUniqueId())) {
                manager.removeStandFromIndex(entity.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        if (manager.isPetStand(event.getRightClicked().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (manager.isPetStand(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
