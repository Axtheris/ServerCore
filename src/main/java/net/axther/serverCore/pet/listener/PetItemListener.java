package net.axther.serverCore.pet.listener;

import net.axther.serverCore.pet.PetManager;
import net.axther.serverCore.pet.PetProfile;
import net.axther.serverCore.pet.data.PetStore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class PetItemListener implements Listener {

    private final PetManager manager;

    public PetItemListener(PetManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        String petId = meta.getPersistentDataContainer().get(PetProfile.PET_ID_KEY, PersistentDataType.STRING);
        if (petId == null) return;

        // Cancel the interaction so the item isn't placed/used
        event.setCancelled(true);

        PetProfile profile = manager.getProfile(petId);
        if (profile == null) {
            player.sendMessage("This pet type no longer exists.");
            return;
        }

        // Toggle: if already summoned, dismiss; otherwise summon
        if (manager.hasPetType(player.getUniqueId(), petId)) {
            manager.dismissPetType(player.getUniqueId(), petId);
            player.sendMessage(profile.getDisplayName() + " dismissed!");
        } else {
            PetStore store = manager.getStore();
            if (store != null) {
                store.addPet(player.getUniqueId(), petId);
            }
            manager.summonPet(player, profile);
            player.sendMessage(profile.getDisplayName() + " summoned!");
        }
    }
}
