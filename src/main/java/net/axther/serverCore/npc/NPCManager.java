package net.axther.serverCore.npc;

import net.axther.serverCore.npc.render.NPCRenderer;
import net.axther.serverCore.npc.render.NPCViewTracker;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

public class NPCManager {

    private final Map<String, NPC> npcs = new LinkedHashMap<>();
    private final Map<Integer, NPC> entityIdIndex = new HashMap<>();

    private NPCRenderer renderer;
    private NPCViewTracker viewTracker;

    public void init(NPCRenderer renderer, NPCViewTracker viewTracker) {
        this.renderer = renderer;
        this.viewTracker = viewTracker;
    }

    public void register(NPC npc) {
        npcs.put(npc.getId(), npc);
        entityIdIndex.put(npc.getEntityId(), npc);
    }

    public void unregister(String id) {
        NPC npc = npcs.remove(id);
        if (npc != null) {
            entityIdIndex.remove(npc.getEntityId());
            if (viewTracker != null) {
                viewTracker.despawnForAllViewers(npc);
            }
        }
    }

    public NPC get(String id) {
        return npcs.get(id);
    }

    public NPC getByEntityId(int entityId) {
        return entityIdIndex.get(entityId);
    }

    public Collection<NPC> getAll() {
        return Collections.unmodifiableCollection(npcs.values());
    }

    public void destroyAll() {
        if (viewTracker != null) {
            viewTracker.despawnAll();
        }
        npcs.clear();
        entityIdIndex.clear();
    }

    public void tickAll() {
        if (renderer == null) return;

        for (NPC npc : npcs.values()) {
            if (!npc.isLookAtPlayer()) continue;

            Location npcLoc = npc.getLocation();
            if (npcLoc.getWorld() == null) continue;

            Set<UUID> viewers = viewTracker != null ? viewTracker.getViewers(npc.getId()) : Collections.emptySet();
            if (viewers.isEmpty()) continue;

            // Find nearest player among viewers within 10 blocks
            Player nearest = null;
            double nearestDistSq = 100.0; // 10 block radius

            for (UUID viewerId : viewers) {
                Player player = Bukkit.getPlayer(viewerId);
                if (player == null || !player.isOnline()) continue;
                if (!npcLoc.getWorld().equals(player.getWorld())) continue;

                double distSq = player.getLocation().distanceSquared(npcLoc);
                if (distSq < nearestDistSq) {
                    nearestDistSq = distSq;
                    nearest = player;
                }
            }

            if (nearest != null) {
                Location playerLoc = nearest.getEyeLocation();
                double dx = playerLoc.getX() - npcLoc.getX();
                double dy = playerLoc.getY() - npcLoc.getY();
                double dz = playerLoc.getZ() - npcLoc.getZ();
                float lookYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                double horizontalDist = Math.sqrt(dx * dx + dz * dz);
                float lookPitch = (float) Math.toDegrees(-Math.atan2(dy, horizontalDist));

                // Send rotation to all viewers
                for (UUID viewerId : viewers) {
                    Player viewer = Bukkit.getPlayer(viewerId);
                    if (viewer != null && viewer.isOnline()) {
                        renderer.sendHeadRotation(viewer, npc, lookYaw, lookPitch);
                    }
                }
            }
        }
    }

    public NPCRenderer getRenderer() { return renderer; }
    public NPCViewTracker getViewTracker() { return viewTracker; }
}
