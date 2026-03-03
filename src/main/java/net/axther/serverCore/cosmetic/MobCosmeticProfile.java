package net.axther.serverCore.cosmetic;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public class MobCosmeticProfile {

    private final double headY;
    private final double headForwardZ;
    private final double headSideX;
    private final boolean useSmallStand;

    private static final double NORMAL_RENDER_HEIGHT = 1.7;
    private static final double SMALL_RENDER_HEIGHT = 0.85;

    public MobCosmeticProfile(double headY, double headForwardZ, double headSideX, boolean useSmallStand) {
        this.headY = headY;
        this.headForwardZ = headForwardZ;
        this.headSideX = headSideX;
        this.useSmallStand = useSmallStand;
    }

    /**
     * Computes the armor stand location so the helmet item sits at the mob's head.
     * Uses the mob's look yaw (getLocation().getYaw()) for both the forward offset
     * and the stand's facing direction, so the cosmetic follows where the head looks.
     */
    public Location computeStandLocation(LivingEntity mob) {
        Location loc = mob.getLocation();
        // getLocation().getYaw() returns where the entity is looking (head yaw for mobs)
        float headYawDeg = loc.getYaw();
        double headYawRad = Math.toRadians(headYawDeg);

        double sinYaw = Math.sin(headYawRad);
        double cosYaw = Math.cos(headYawRad);

        // Rotate the forward (Z) and side (X) offsets by the mob's head yaw
        double offsetX = -headForwardZ * sinYaw + headSideX * cosYaw;
        double offsetZ = headForwardZ * cosYaw + headSideX * sinYaw;

        double renderHeight = useSmallStand ? SMALL_RENDER_HEIGHT : NORMAL_RENDER_HEIGHT;
        double standY = loc.getY() + headY - renderHeight;

        return new Location(loc.getWorld(), loc.getX() + offsetX, standY, loc.getZ() + offsetZ, headYawDeg, 0);
    }

    public boolean useSmallStand() {
        return useSmallStand;
    }

    public double getHeadY() {
        return headY;
    }

    public double getHeadForwardZ() {
        return headForwardZ;
    }

    public double getHeadSideX() {
        return headSideX;
    }
}
