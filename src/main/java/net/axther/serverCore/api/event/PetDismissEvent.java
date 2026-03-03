package net.axther.serverCore.api.event;

import net.axther.serverCore.pet.PetProfile;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired before a pet is dismissed. Informational only (not cancellable).
 */
public class PetDismissEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final PetProfile petProfile;

    public PetDismissEvent(Player player, PetProfile petProfile) {
        this.player = player;
        this.petProfile = petProfile;
    }

    public Player getPlayer() {
        return player;
    }

    public PetProfile getPetProfile() {
        return petProfile;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
