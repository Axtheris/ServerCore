package net.axther.serverCore.npc.listener;

import net.axther.serverCore.npc.NPC;
import net.axther.serverCore.npc.NPCManager;
import net.axther.serverCore.npc.config.NPCConfig;
import net.axther.serverCore.npc.dialogue.DialogueSession;
import net.axther.serverCore.npc.dialogue.DialogueTree;
import net.axther.serverCore.npc.render.NPCViewTracker;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NPCListener implements Listener {

    private final JavaPlugin plugin;
    private final NPCManager manager;
    private final NPCConfig config;
    private final Map<UUID, DialogueSession> activeSessions = new HashMap<>();

    public NPCListener(JavaPlugin plugin, NPCManager manager, NPCConfig config) {
        this.plugin = plugin;
        this.manager = manager;
        this.config = config;
    }

    public void handleInteraction(Player player, NPC npc) {
        // Track talk objectives for active quests
        var questManager = net.axther.serverCore.api.ServerCoreAPI.get().getQuestManager();
        if (questManager != null) {
            questManager.handleTalk(player.getUniqueId(), npc.getId());
        }

        String dialogueId = npc.getDialogueId();
        if (dialogueId == null) return;

        DialogueTree tree = config.getDialogueTree(dialogueId);
        if (tree == null) return;

        DialogueSession session = new DialogueSession(player, npc, tree);
        activeSessions.put(player.getUniqueId(), session);
        session.start();

        // Spawn dialogue hologram above NPC head if enabled
        if (npc.isDialogueHologram()) {
            Location npcLoc = npc.getLocation().clone().add(0, 2.0, 0);
            net.axther.serverCore.hologram.DialogueHologram dialogueHolo =
                    new net.axther.serverCore.hologram.DialogueHologram(
                            plugin, player, npcLoc, npc.getDialogueHologramOffset());
            session.setDialogueHologram(dialogueHolo);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        NPCViewTracker viewTracker = manager.getViewTracker();
        if (viewTracker != null) {
            viewTracker.handlePlayerJoin(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        DialogueSession session = activeSessions.remove(event.getPlayer().getUniqueId());
        if (session != null) {
            session.end();
        }

        NPCViewTracker viewTracker = manager.getViewTracker();
        if (viewTracker != null) {
            viewTracker.handlePlayerQuit(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        NPCViewTracker viewTracker = manager.getViewTracker();
        if (viewTracker != null) {
            viewTracker.handlePlayerChangedWorld(event.getPlayer());
        }
    }

    public void handleDialogueChoice(Player player, int choiceIndex) {
        DialogueSession session = activeSessions.get(player.getUniqueId());
        if (session == null || !session.isActive()) return;

        session.selectChoice(choiceIndex);

        if (!session.isActive()) {
            activeSessions.remove(player.getUniqueId());
        }
    }

    public DialogueSession getSession(Player player) {
        return activeSessions.get(player.getUniqueId());
    }

    public void clearAllSessions() {
        for (DialogueSession session : activeSessions.values()) {
            session.end();
        }
        activeSessions.clear();
    }
}
