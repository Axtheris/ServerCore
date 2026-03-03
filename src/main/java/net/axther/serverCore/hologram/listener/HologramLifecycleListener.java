package net.axther.serverCore.hologram.listener;

import net.axther.serverCore.hologram.Hologram;
import net.axther.serverCore.hologram.HologramManager;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;

public class HologramLifecycleListener implements Listener {

    private final HologramManager manager;

    public HologramLifecycleListener(HologramManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        int chunkX = event.getChunk().getX();
        int chunkZ = event.getChunk().getZ();
        long chunkKey = ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);

        for (Hologram hologram : manager.getAll()) {
            if (hologram.getChunkKey() == chunkKey && !hologram.isSpawned()) {
                hologram.spawn();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        int chunkX = event.getChunk().getX();
        int chunkZ = event.getChunk().getZ();
        long chunkKey = ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);

        for (Hologram hologram : manager.getAll()) {
            if (hologram.getChunkKey() == chunkKey && hologram.isSpawned()) {
                hologram.despawn();
            }
        }
    }
}
