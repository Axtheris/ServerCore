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
        this.renderHeight = useSmallStand ? SMALL_RENDER_HEIGHT : NORMAL_RENDER_HEIGHT;
    }

    // Pre-computed render height — avoids branch every tick
    private final double renderHeight;

    /**
     * Computes the armor stand location so the helmet item sits at the mob's head.
     * Allocates a new Location — use for one-off calls like initial spawn placement.
     */
    public Location computeStandLocation(LivingEntity mob) {
        Location loc = mob.getLocation();
        Location out = new Location(loc.getWorld(), 0, 0, 0);
        computeStandLocation(mob, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), out);
        return out;
    }

    /**
     * Writes the computed stand position into {@code out}, avoiding allocation.
     * This is the hot-path variant called every tick.
     */
    public void computeStandLocation(LivingEntity mob, double mobX, double mobY, double mobZ, float headYawDeg, Location out) {
        double headYawRad = Math.toRadians(headYawDeg);

        double sinYaw = Math.sin(headYawRad);
        double cosYaw = Math.cos(headYawRad);

        double offsetX = -headForwardZ * sinYaw + headSideX * cosYaw;
        double offsetZ = headForwardZ * cosYaw + headSideX * sinYaw;

        double standY = mobY + headY - renderHeight;

        out.setWorld(mob.getWorld());
        out.setX(mobX + offsetX);
        out.setY(standY);
        out.setZ(mobZ + offsetZ);
        out.setYaw(headYawDeg);
        out.setPitch(0);
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
