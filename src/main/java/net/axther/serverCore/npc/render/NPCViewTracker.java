package net.axther.serverCore.npc.render;

import net.axther.serverCore.npc.NPC;
import net.axther.serverCore.npc.NPCManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

public class NPCViewTracker {

    private final NPCManager manager;
    private final NPCRenderer renderer;
    private final double viewDistanceSq;

    // NPC id -> set of player UUIDs currently seeing it
    private final Map<String, Set<UUID>> npcViewers = new HashMap<>();
    // Player UUID -> set of NPC ids they can see
    private final Map<UUID, Set<String>> playerVisibleNpcs = new HashMap<>();

    public NPCViewTracker(NPCManager manager, NPCRenderer renderer, int viewDistance) {
        this.manager = manager;
        this.renderer = renderer;
        this.viewDistanceSq = (double) viewDistance * viewDistance;
    }

    public void updateAll() {
        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        Collection<NPC> allNpcs = manager.getAll();

        for (Player player : onlinePlayers) {
            UUID playerId = player.getUniqueId();
            Location playerLoc = player.getLocation();
            Set<String> currentlyVisible = playerVisibleNpcs.computeIfAbsent(playerId, k -> new HashSet<>());

            for (NPC npc : allNpcs) {
                String npcId = npc.getId();
                Location npcLoc = npc.getLocation();
                boolean inRange = npcLoc.getWorld() != null
                        && playerLoc.getWorld() != null
                        && npcLoc.getWorld().equals(playerLoc.getWorld())
                        && npcLoc.distanceSquared(playerLoc) <= viewDistanceSq;

                boolean wasVisible = currentlyVisible.contains(npcId);

                if (inRange && !wasVisible) {
                    // Spawn for this player
                    renderer.sendSpawn(player, npc);
                    currentlyVisible.add(npcId);
                    npcViewers.computeIfAbsent(npcId, k -> new HashSet<>()).add(playerId);
                } else if (!inRange && wasVisible) {
                    // Despawn for this player
                    renderer.sendDespawn(player, npc);
                    currentlyVisible.remove(npcId);
                    Set<UUID> viewers = npcViewers.get(npcId);
                    if (viewers != null) {
                        viewers.remove(playerId);
                    }
                }
            }
        }
    }

    public void spawnForAllViewers(NPC npc) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Location playerLoc = player.getLocation();
            Location npcLoc = npc.getLocation();

            if (npcLoc.getWorld() != null && playerLoc.getWorld() != null
                    && npcLoc.getWorld().equals(playerLoc.getWorld())
                    && npcLoc.distanceSquared(playerLoc) <= viewDistanceSq) {
                renderer.sendSpawn(player, npc);
                npcViewers.computeIfAbsent(npc.getId(), k -> new HashSet<>()).add(player.getUniqueId());
                playerVisibleNpcs.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(npc.getId());
            }
        }
    }

    public void despawnForAllViewers(NPC npc) {
        Set<UUID> viewers = npcViewers.remove(npc.getId());
        if (viewers == null) return;

        for (UUID playerId : viewers) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                renderer.sendDespawn(player, npc);
            }
            Set<String> visible = playerVisibleNpcs.get(playerId);
            if (visible != null) {
                visible.remove(npc.getId());
            }
        }
    }

    public void teleportForAllViewers(NPC npc) {
        Set<UUID> viewers = npcViewers.get(npc.getId());
        if (viewers == null) return;

        for (UUID playerId : new HashSet<>(viewers)) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                renderer.sendTeleport(player, npc);
            }
        }
    }

    public void handlePlayerJoin(Player player) {
        // Will be picked up on next updateAll() tick
        playerVisibleNpcs.put(player.getUniqueId(), new HashSet<>());
    }

    public void handlePlayerQuit(Player player) {
        UUID playerId = player.getUniqueId();
        Set<String> visible = playerVisibleNpcs.remove(playerId);
        if (visible != null) {
            for (String npcId : visible) {
                Set<UUID> viewers = npcViewers.get(npcId);
                if (viewers != null) {
                    viewers.remove(playerId);
                }
            }
        }
    }

    public void handlePlayerChangedWorld(Player player) {
        UUID playerId = player.getUniqueId();
        Set<String> visible = playerVisibleNpcs.get(playerId);
        if (visible != null) {
            // Despawn all currently visible NPCs -- updateAll() will re-spawn those in range
            for (String npcId : new HashSet<>(visible)) {
                NPC npc = manager.get(npcId);
                if (npc != null) {
                    renderer.sendDespawn(player, npc);
                }
                Set<UUID> viewers = npcViewers.get(npcId);
                if (viewers != null) {
                    viewers.remove(playerId);
                }
            }
            visible.clear();
        }
    }

    public int getViewerCount(String npcId) {
        Set<UUID> viewers = npcViewers.get(npcId);
        return viewers != null ? viewers.size() : 0;
    }

    public Set<UUID> getViewers(String npcId) {
        Set<UUID> viewers = npcViewers.get(npcId);
        return viewers != null ? Collections.unmodifiableSet(viewers) : Collections.emptySet();
    }

    public void despawnAll() {
        for (NPC npc : manager.getAll()) {
            despawnForAllViewers(npc);
        }
        npcViewers.clear();
        playerVisibleNpcs.clear();
    }
}
