package net.axther.serverCore.cosmetic;

import net.axther.serverCore.api.event.CosmeticApplyEvent;
import net.axther.serverCore.api.event.CosmeticRemoveEvent;
import net.axther.serverCore.cosmetic.data.CosmeticStore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.logging.Logger;

public class CosmeticManager {

    private final Map<EntityType, MobCosmeticProfile> profiles = new EnumMap<>(EntityType.class);
    private final Map<UUID, List<CosmeticInstance>> activeCosmetics = new HashMap<>();
    private final Map<UUID, CosmeticInstance> standIndex = new HashMap<>();
    private CosmeticStore store;
    private final Logger logger;
    private int sweepCounter = 0;

    public CosmeticManager(Logger logger) {
        this.logger = logger;
    }

    public void registerProfile(EntityType type, MobCosmeticProfile profile) {
        profiles.put(type, profile);
    }

    public MobCosmeticProfile getProfile(EntityType type) {
        return profiles.get(type);
    }

    public void setStore(CosmeticStore store) {
        this.store = store;
    }

    public boolean applyCosmetic(LivingEntity mob, ItemStack item) {
        MobCosmeticProfile profile = profiles.get(mob.getType());
        if (profile == null) {
            return false;
        }

        // CORR-01 VERIFIED: Event is fired BEFORE stand spawn. If cancelled, we return false
        // without spawning any ArmorStand — no orphan entities can leak on cancellation.
        // DO NOT reorder: stand spawn must remain AFTER the isCancelled() check.
        CosmeticApplyEvent event = new CosmeticApplyEvent(mob, item);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        Location standLoc = profile.computeStandLocation(mob);
        ArmorStand stand = mob.getWorld().spawn(standLoc, ArmorStand.class, s -> {
            s.setVisible(false);
            s.setMarker(true);
            s.setGravity(false);
            s.setSilent(true);
            s.setPersistent(false);
            s.setCanPickupItems(false);
            s.setSmall(profile.useSmallStand());
            s.setItem(EquipmentSlot.HEAD, item.clone());
            // Lock all equipment slots to prevent interaction
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                s.addEquipmentLock(slot, ArmorStand.LockType.REMOVING_OR_CHANGING);
            }
        });

        CosmeticInstance instance = new CosmeticInstance(mob.getUniqueId(), stand.getUniqueId(), profile, item.clone());

        activeCosmetics.computeIfAbsent(mob.getUniqueId(), k -> new ArrayList<>()).add(instance);
        standIndex.put(stand.getUniqueId(), instance);

        if (store != null) {
            store.markDirty();
        }

        return true;
    }

    public void removeCosmetics(UUID mobUuid) {
        List<CosmeticInstance> instances = activeCosmetics.remove(mobUuid);
        if (instances == null) return;

        Bukkit.getPluginManager().callEvent(new CosmeticRemoveEvent(mobUuid));

        for (CosmeticInstance instance : instances) {
            standIndex.remove(instance.getStandUuid());
            instance.destroy();
        }

        if (store != null) {
            store.markDirty();
        }
    }

    public void tickAll() {
        var iterator = activeCosmetics.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            var instances = entry.getValue();
            instances.removeIf(instance -> {
                try {
                    if (!instance.tick()) {
                        standIndex.remove(instance.getStandUuid());
                        return true;
                    }
                    return false;
                } catch (Exception e) {
                    logger.warning("[ServerCore] CosmeticInstance tick failed for mob UUID "
                            + instance.getMobUuid() + ": " + e.getMessage());
                    standIndex.remove(instance.getStandUuid());
                    try { instance.destroy(); } catch (Exception ignored) {}
                    return true;
                }
            });
            if (instances.isEmpty()) {
                iterator.remove();
            }
        }

        // MEM-02: Safety-net audit sweep — runs every 10 minutes (12000 ticks).
        // Catches stale standIndex entries not removed by normal cleanup paths.
        // Non-persistent armor stands are removed on chunk unload, so getEntity()==null
        // means the entity is truly gone. Logged at FINE to avoid INFO noise.
        if (++sweepCounter >= 12000) {
            sweepCounter = 0;
            standIndex.entrySet().removeIf(e -> {
                if (Bukkit.getEntity(e.getKey()) == null) {
                    logger.fine("[ServerCore] CosmeticManager audit: removed stale standIndex entry " + e.getKey());
                    return true;
                }
                return false;
            });
        }
    }

    public void destroyAll() {
        for (List<CosmeticInstance> instances : activeCosmetics.values()) {
            for (CosmeticInstance instance : instances) {
                instance.destroy();
            }
        }
        activeCosmetics.clear();
        standIndex.clear();
    }

    public boolean isCosmeticStand(UUID standUuid) {
        return standIndex.containsKey(standUuid);
    }

    public boolean hasCosmetics(UUID mobUuid) {
        return activeCosmetics.containsKey(mobUuid);
    }

    public Set<EntityType> getSupportedTypes() {
        return Collections.unmodifiableSet(profiles.keySet());
    }

    public Map<UUID, List<CosmeticInstance>> getActiveCosmetics() {
        return Collections.unmodifiableMap(activeCosmetics);
    }

    public List<CosmeticInstance> getCosmetics(UUID mobUuid) {
        List<CosmeticInstance> instances = activeCosmetics.get(mobUuid);
        return instances != null ? Collections.unmodifiableList(instances) : Collections.emptyList();
    }
}
