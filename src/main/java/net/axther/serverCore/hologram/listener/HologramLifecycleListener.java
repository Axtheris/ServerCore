package net.axther.serverCore.hologram.listener;

import net.axther.serverCore.hologram.Hologram;
import net.axther.serverCore.hologram.HologramManager;
import net.axther.serverCore.hologram.visibility.HologramVisibilityTracker;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;

public class HologramLifecycleListener implements Listener {

    private final HologramManager manager;
    private final HologramVisibilityTracker visibilityTracker;

    public HologramLifecycleListener(HologramManager manager, HologramVisibilityTracker visibilityTracker) {
        this.manager = manager;
        this.visibilityTracker = visibilityTracker;
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

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        if (visibilityTracker != null) {
            visibilityTracker.handlePlayerQuit(event.getPlayer().getUniqueId());
        }
    }
}
