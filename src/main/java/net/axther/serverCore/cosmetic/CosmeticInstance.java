package net.axther.serverCore.cosmetic;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import java.lang.ref.WeakReference;
import java.util.UUID;

public class CosmeticInstance {

    private final UUID mobUuid;
    private final UUID standUuid;
    private final MobCosmeticProfile profile;
    private final ItemStack item;

    // Cached entity references — avoids Bukkit.getEntity(UUID) global lookup every tick
    private WeakReference<LivingEntity> cachedMob;
    private WeakReference<ArmorStand> cachedStand;

    // Reusable Location to avoid allocation per tick
    private final Location reusableTarget = new Location(null, 0, 0, 0);

    // Last known position — skip teleport if mob hasn't moved
    private double lastX, lastY, lastZ;
    private float lastYaw;

    public CosmeticInstance(UUID mobUuid, UUID standUuid, MobCosmeticProfile profile, ItemStack item) {
        this.mobUuid = mobUuid;
        this.standUuid = standUuid;
        this.profile = profile;
        this.item = item;
    }

    /**
     * Updates the armor stand position to follow the mob.
     * @return true if the instance is still valid, false if it should be removed
     */
    public boolean tick() {
        LivingEntity mob = getMob();
        ArmorStand stand = getStand();

        if (mob == null || mob.isDead() || stand == null || stand.isDead()) {
            destroy();
            return false;
        }

        // Read mob position directly — cheaper than getLocation() which allocates
        double mx = mob.getX();
        double my = mob.getY();
        double mz = mob.getZ();
        float myaw = mob.getYaw();

        // Skip teleport if the mob hasn't moved or turned
        if (mx == lastX && my == lastY && mz == lastZ && myaw == lastYaw) {
            return true;
        }
        lastX = mx;
        lastY = my;
        lastZ = mz;
        lastYaw = myaw;

        profile.computeStandLocation(mob, mx, my, mz, myaw, reusableTarget);
        stand.teleport(reusableTarget);
        return true;
    }

    public void destroy() {
        ArmorStand stand = getStand();
        if (stand != null && !stand.isDead()) {
            stand.remove();
        }
    }

    public UUID getMobUuid() {
        return mobUuid;
    }

    public UUID getStandUuid() {
        return standUuid;
    }

    public MobCosmeticProfile getProfile() {
        return profile;
    }

    public ItemStack getItem() {
        return item;
    }

    private LivingEntity getMob() {
        LivingEntity mob = cachedMob != null ? cachedMob.get() : null;
        if (mob != null && !mob.isDead()) return mob;
        var entity = Bukkit.getEntity(mobUuid);
        if (entity instanceof LivingEntity living) {
            cachedMob = new WeakReference<>(living);
            return living;
        }
        return null;
    }

    private ArmorStand getStand() {
        ArmorStand stand = cachedStand != null ? cachedStand.get() : null;
        if (stand != null && !stand.isDead()) return stand;
        var entity = Bukkit.getEntity(standUuid);
        if (entity instanceof ArmorStand as) {
            cachedStand = new WeakReference<>(as);
            return as;
        }
        return null;
    }
}
