package net.axther.serverCore.api.event;

import net.axther.serverCore.pet.PetProfile;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired before a pet is spawned for a player. Cancelling prevents the summon.
 */
public class PetSummonEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final PetProfile petProfile;
    private boolean cancelled;

    public PetSummonEvent(Player player, PetProfile petProfile) {
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
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
