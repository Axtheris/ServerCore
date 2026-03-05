package net.axther.serverCore.pet.listener;

import net.axther.serverCore.pet.PetInstance;
import net.axther.serverCore.pet.PetManager;
import net.axther.serverCore.pet.PetState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.inventory.ItemStack;

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
        PetInstance pet = manager.getPetByStand(event.getRightClicked().getUniqueId());
        if (pet == null) return;

        event.setCancelled(true);

        Player player = event.getPlayer();

        // Only the owner can interact with their pet
        if (!player.getUniqueId().equals(pet.getOwnerUuid())) return;

        // Shift + right-click: toggle passive mode
        if (player.isSneaking()) {
            boolean nowPassive = !pet.isPassive();
            pet.setPassive(nowPassive);
            String petName = pet.getProfile().getDisplayName();
            if (nowPassive) {
                player.sendMessage(Component.text(petName + " is now passive.", NamedTextColor.AQUA));
            } else {
                player.sendMessage(Component.text(petName + " will now attack enemies.", NamedTextColor.RED));
            }
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();

        // Right-click with food: feed the pet
        if (!hand.getType().isAir() && hand.getType().isEdible()) {
            if (pet.feed()) {
                // Consume one food item
                if (hand.getAmount() > 1) {
                    hand.setAmount(hand.getAmount() - 1);
                } else {
                    player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                }
                player.sendMessage(Component.text(pet.getProfile().getDisplayName() + " enjoyed the treat!", NamedTextColor.GREEN));
            } else {
                int seconds = pet.getRemainingFeedCooldownSeconds();
                player.sendMessage(Component.text(pet.getProfile().getDisplayName() + " is not hungry yet. (" + seconds + "s remaining)", NamedTextColor.GRAY));
            }
            return;
        }

        // Right-click with empty hand: toggle sit/follow
        if (hand.getType().isAir()) {
            if (pet.getState() == PetState.SITTING) {
                pet.setState(PetState.FOLLOWING);
                player.sendMessage(Component.text(pet.getProfile().getDisplayName() + " is now following you.", NamedTextColor.GREEN));
            } else {
                pet.setState(PetState.SITTING);
                player.sendMessage(Component.text(pet.getProfile().getDisplayName() + " is now sitting.", NamedTextColor.YELLOW));
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (manager.isPetStand(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
