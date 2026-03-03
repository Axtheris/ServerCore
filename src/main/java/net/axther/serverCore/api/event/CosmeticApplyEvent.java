package net.axther.serverCore.api.event;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/**
 * Fired before a cosmetic is applied to a mob. Cancelling prevents the application.
 */
public class CosmeticApplyEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final LivingEntity mob;
    private final ItemStack itemStack;
    private boolean cancelled;

    public CosmeticApplyEvent(LivingEntity mob, ItemStack itemStack) {
        this.mob = mob;
        this.itemStack = itemStack;
    }

    public LivingEntity getMob() {
        return mob;
    }

    public ItemStack getItemStack() {
        return itemStack;
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
