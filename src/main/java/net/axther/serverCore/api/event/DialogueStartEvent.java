package net.axther.serverCore.api.event;

import net.axther.serverCore.npc.dialogue.DialogueTree;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired before a dialogue sequence starts with an NPC. Cancelling prevents the dialogue.
 */
public class DialogueStartEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String npcId;
    private final DialogueTree dialogueTree;
    private boolean cancelled;

    public DialogueStartEvent(Player player, String npcId, DialogueTree dialogueTree) {
        this.player = player;
        this.npcId = npcId;
        this.dialogueTree = dialogueTree;
    }

    public Player getPlayer() {
        return player;
    }

    public String getNpcId() {
        return npcId;
    }

    public DialogueTree getDialogueTree() {
        return dialogueTree;
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
