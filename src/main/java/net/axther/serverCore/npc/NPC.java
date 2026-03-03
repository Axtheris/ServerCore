package net.axther.serverCore.npc;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

public class NPC {

    private final String id;
    private String displayName;
    private Location location;
    private float yaw;
    private String skinTexture;
    private String skinSignature;
    private boolean lookAtPlayer;
    private String dialogueId;
    private UUID entityUuid;

    public NPC(String id, String displayName, Location location, float yaw,
               String skinTexture, String skinSignature,
               boolean lookAtPlayer, String dialogueId) {
        this.id = id;
        this.displayName = displayName;
        this.location = location.clone();
        this.yaw = yaw;
        this.skinTexture = skinTexture;
        this.skinSignature = skinSignature;
        this.lookAtPlayer = lookAtPlayer;
        this.dialogueId = dialogueId;
    }

    public void spawn() {
        if (isSpawned()) return;

        World world = location.getWorld();
        if (world == null) return;
        if (!world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) return;

        Location spawnLoc = location.clone();
        spawnLoc.setYaw(yaw);

        ArmorStand stand = world.spawn(spawnLoc, ArmorStand.class, entity -> {
            entity.setVisible(false);
            entity.setCustomNameVisible(true);
            entity.customName(MiniMessage.miniMessage().deserialize(displayName));
            entity.setMarker(true);
            entity.setGravity(false);
            entity.setPersistent(false);
            entity.setCanPickupItems(false);
            entity.setBasePlate(false);
            entity.setArms(false);

            // Lock all equipment slots
            for (org.bukkit.inventory.EquipmentSlot slot : org.bukkit.inventory.EquipmentSlot.values()) {
                entity.addEquipmentLock(slot, ArmorStand.LockType.REMOVING_OR_CHANGING);
            }

            // Apply player head with skin if provided
            if (skinTexture != null && !skinTexture.isBlank()) {
                ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) skull.getItemMeta();
                if (meta != null) {
                    PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), "NPC_" + id);
                    profile.setProperty(new ProfileProperty("textures", skinTexture,
                            skinSignature != null ? skinSignature : ""));
                    meta.setPlayerProfile(profile);
                    skull.setItemMeta(meta);
                }
                entity.getEquipment().setHelmet(skull);
            }
        });

        this.entityUuid = stand.getUniqueId();
    }

    public void despawn() {
        if (entityUuid == null) return;

        Entity entity = Bukkit.getEntity(entityUuid);
        if (entity != null) {
            entity.remove();
        }
        entityUuid = null;
    }

    public boolean isSpawned() {
        if (entityUuid == null) return false;
        Entity entity = Bukkit.getEntity(entityUuid);
        return entity != null && !entity.isDead();
    }

    public long getChunkKey() {
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    public void setLocation(Location location) {
        this.location = location.clone();
        this.yaw = location.getYaw();
        if (isSpawned()) {
            Entity entity = Bukkit.getEntity(entityUuid);
            if (entity != null) {
                Location teleportLoc = location.clone();
                teleportLoc.setYaw(yaw);
                entity.teleport(teleportLoc);
            }
        }
    }

    // --- Getters and setters ---

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public Location getLocation() { return location.clone(); }
    public float getYaw() { return yaw; }
    public void setYaw(float yaw) { this.yaw = yaw; }
    public String getSkinTexture() { return skinTexture; }
    public void setSkinTexture(String skinTexture) { this.skinTexture = skinTexture; }
    public String getSkinSignature() { return skinSignature; }
    public void setSkinSignature(String skinSignature) { this.skinSignature = skinSignature; }
    public boolean isLookAtPlayer() { return lookAtPlayer; }
    public void setLookAtPlayer(boolean lookAtPlayer) { this.lookAtPlayer = lookAtPlayer; }
    public String getDialogueId() { return dialogueId; }
    public void setDialogueId(String dialogueId) { this.dialogueId = dialogueId; }
    public UUID getEntityUuid() { return entityUuid; }

    public String getWorldName() {
        return location.getWorld() != null ? location.getWorld().getName() : "world";
    }
}
