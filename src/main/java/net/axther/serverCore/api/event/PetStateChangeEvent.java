package net.axther.serverCore.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PetStateChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final String petProfile;
    private final String oldState;
    private final String newState;

    public PetStateChangeEvent(Player player, String petProfile, String oldState, String newState) {
        this.player = player;
        this.petProfile = petProfile;
        this.oldState = oldState;
        this.newState = newState;
    }

    public Player getPlayer() { return player; }
    public String getPetProfile() { return petProfile; }
    public String getOldState() { return oldState; }
    public String getNewState() { return newState; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
