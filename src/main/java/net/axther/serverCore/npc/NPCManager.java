package net.axther.serverCore.npc;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;

public class NPCManager {

    private final Map<String, NPC> npcs = new LinkedHashMap<>();
    private final Map<UUID, NPC> entityIndex = new HashMap<>();

    public void register(NPC npc) {
        npcs.put(npc.getId(), npc);
        if (npc.getEntityUuid() != null) {
            entityIndex.put(npc.getEntityUuid(), npc);
        }
    }

    public void unregister(String id) {
        NPC npc = npcs.remove(id);
        if (npc != null) {
            if (npc.getEntityUuid() != null) {
                entityIndex.remove(npc.getEntityUuid());
            }
            npc.despawn();
        }
    }

    public NPC get(String id) {
        return npcs.get(id);
    }

    public NPC getByEntityUuid(UUID uuid) {
        return entityIndex.get(uuid);
    }

    public Collection<NPC> getAll() {
        return Collections.unmodifiableCollection(npcs.values());
    }

    public void spawnAll() {
        for (NPC npc : npcs.values()) {
            if (!npc.isSpawned()) {
                npc.spawn();
                if (npc.getEntityUuid() != null) {
                    entityIndex.put(npc.getEntityUuid(), npc);
                }
            }
        }
    }

    public void despawnAll() {
        for (NPC npc : npcs.values()) {
            if (npc.getEntityUuid() != null) {
                entityIndex.remove(npc.getEntityUuid());
            }
            npc.despawn();
        }
    }

    public void destroyAll() {
        despawnAll();
        npcs.clear();
        entityIndex.clear();
    }

    public void tickAll() {
        for (NPC npc : npcs.values()) {
            if (!npc.isSpawned() || !npc.isLookAtPlayer()) continue;

            Entity entity = Bukkit.getEntity(npc.getEntityUuid());
            if (entity == null) continue;

            Location npcLoc = entity.getLocation();
            Player nearest = null;
            double nearestDistSq = 100.0; // 10 block radius

            for (Player player : npcLoc.getWorld().getPlayers()) {
                double distSq = player.getLocation().distanceSquared(npcLoc);
                if (distSq < nearestDistSq) {
                    nearestDistSq = distSq;
                    nearest = player;
                }
            }

            if (nearest != null) {
                Location playerLoc = nearest.getEyeLocation();
                double dx = playerLoc.getX() - npcLoc.getX();
                double dz = playerLoc.getZ() - npcLoc.getZ();
                float lookYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));

                Location newLoc = npcLoc.clone();
                newLoc.setYaw(lookYaw);
                entity.teleport(newLoc);
            }

            // Rebuild entity index if UUID changed (shouldn't normally happen)
            if (npc.getEntityUuid() != null && !entityIndex.containsKey(npc.getEntityUuid())) {
                entityIndex.put(npc.getEntityUuid(), npc);
            }
        }
    }
}
