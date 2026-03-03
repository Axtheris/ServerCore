package net.axther.serverCore.timeline.action;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Spawns an entity at the origin with an optional offset.
 * Config: type: mob, entity-type: WARDEN, offset: [0, 1, 0]
 */
public class SpawnMobAction implements TimelineAction {

    private final EntityType entityType;
    private final double offsetX;
    private final double offsetY;
    private final double offsetZ;

    public SpawnMobAction(EntityType entityType, double offsetX, double offsetY, double offsetZ) {
        this.entityType = entityType;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    @Override
    public void execute(Location origin, Collection<Player> audience) {
        if (origin.getWorld() == null) return;

        Location spawnLoc = origin.clone().add(offsetX, offsetY, offsetZ);
        origin.getWorld().spawnEntity(spawnLoc, entityType);
    }

    public static SpawnMobAction fromConfig(Map<?, ?> map) {
        EntityType entityType;
        try {
            entityType = EntityType.valueOf(String.valueOf(map.get("entity-type")).toUpperCase());
        } catch (IllegalArgumentException e) {
            entityType = EntityType.ZOMBIE;
        }

        double ox = 0, oy = 0, oz = 0;
        Object offsetObj = map.get("offset");
        if (offsetObj instanceof List<?> offsetList && offsetList.size() >= 3) {
            ox = ((Number) offsetList.get(0)).doubleValue();
            oy = ((Number) offsetList.get(1)).doubleValue();
            oz = ((Number) offsetList.get(2)).doubleValue();
        }

        return new SpawnMobAction(entityType, ox, oy, oz);
    }
}
