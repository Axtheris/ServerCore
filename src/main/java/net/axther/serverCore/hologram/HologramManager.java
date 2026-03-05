package net.axther.serverCore.hologram;

import net.axther.serverCore.hologram.visibility.HologramVisibilityTracker;
import org.bukkit.Location;

import java.util.*;

public class HologramManager {

    private final Map<String, Hologram> holograms = new LinkedHashMap<>();
    private HologramVisibilityTracker visibilityTracker;

    public void register(Hologram hologram) {
        holograms.put(hologram.getId(), hologram);
    }

    public void unregister(String id) {
        Hologram hologram = holograms.remove(id);
        if (hologram != null) {
            hologram.despawn();
        }
    }

    public Hologram get(String id) {
        return holograms.get(id);
    }

    public Collection<Hologram> getAll() {
        return Collections.unmodifiableCollection(holograms.values());
    }

    public void spawnAll() {
        for (Hologram hologram : holograms.values()) {
            if (!hologram.isSpawned()) {
                hologram.spawn();
            }
        }
    }

    public void despawnAll() {
        for (Hologram hologram : holograms.values()) {
            hologram.despawn();
        }
    }

    public void tickAll(int tickCount) {
        for (Hologram hologram : holograms.values()) {
            if (hologram.isSpawned()) {
                hologram.tick(tickCount);
            }
        }
    }

    public List<Hologram> getNearby(Location loc, double radius) {
        double radiusSq = radius * radius;
        List<Hologram> nearby = new ArrayList<>();
        String worldName = loc.getWorld() != null ? loc.getWorld().getName() : null;

        for (Hologram hologram : holograms.values()) {
            if (worldName != null && worldName.equals(hologram.getWorldName())) {
                if (hologram.getLocation().distanceSquared(loc) <= radiusSq) {
                    nearby.add(hologram);
                }
            }
        }
        return nearby;
    }

    public void refreshPlaceholders(int tickCount) {
        for (Hologram hologram : holograms.values()) {
            if (hologram.isSpawned() && hologram.containsPlaceholders()
                    && tickCount % hologram.getUpdateInterval() == 0) {
                hologram.refreshPlaceholders();
            }
        }
    }

    public Hologram getByEntityUuid(UUID entityUuid) {
        if (entityUuid == null) return null;
        for (Hologram hologram : holograms.values()) {
            if (entityUuid.equals(hologram.getEntityUuid())) {
                return hologram;
            }
        }
        return null;
    }

    public void setVisibilityTracker(HologramVisibilityTracker tracker) {
        this.visibilityTracker = tracker;
    }

    public HologramVisibilityTracker getVisibilityTracker() {
        return visibilityTracker;
    }

    public void destroyAll() {
        despawnAll();
        holograms.clear();
    }
}
