package net.axther.serverCore.npc.listener;

import net.axther.serverCore.npc.NPC;
import net.axther.serverCore.npc.NPCManager;
import net.axther.serverCore.npc.config.NPCConfig;
import net.axther.serverCore.npc.dialogue.DialogueSession;
import net.axther.serverCore.npc.dialogue.DialogueTree;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NPCListener implements Listener {

    private final NPCManager manager;
    private final NPCConfig config;
    private final Map<UUID, DialogueSession> activeSessions = new HashMap<>();

    public NPCListener(NPCManager manager, NPCConfig config) {
        this.manager = manager;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        Entity entity = event.getRightClicked();
        NPC npc = manager.getByEntityUuid(entity.getUniqueId());
        if (npc == null) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        String dialogueId = npc.getDialogueId();
        if (dialogueId == null) return;

        DialogueTree tree = config.getDialogueTree(dialogueId);
        if (tree == null) return;

        // Start or restart dialogue session
        DialogueSession session = new DialogueSession(player, npc, tree);
        activeSessions.put(player.getUniqueId(), session);
        session.start();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        DialogueSession session = activeSessions.remove(event.getPlayer().getUniqueId());
        if (session != null) {
            session.end();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        int chunkX = event.getChunk().getX();
        int chunkZ = event.getChunk().getZ();
        long chunkKey = ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);

        for (NPC npc : manager.getAll()) {
            if (npc.getChunkKey() == chunkKey && !npc.isSpawned()) {
                npc.spawn();
                // Update entity index after spawn
                if (npc.getEntityUuid() != null) {
                    manager.register(npc);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        int chunkX = event.getChunk().getX();
        int chunkZ = event.getChunk().getZ();
        long chunkKey = ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);

        for (NPC npc : manager.getAll()) {
            if (npc.getChunkKey() == chunkKey && npc.isSpawned()) {
                npc.despawn();
            }
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
