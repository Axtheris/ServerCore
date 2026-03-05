package net.axther.serverCore.npc;

import org.bukkit.Location;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class NPC {

    private static final AtomicInteger ENTITY_ID_COUNTER = new AtomicInteger(Integer.MAX_VALUE);

    private final String id;
    private final int entityId;
    private final UUID entityUuid;
    private String displayName;
    private Location location;
    private float yaw;
    private String skinTexture;
    private String skinSignature;
    private boolean lookAtPlayer;
    private String dialogueId;
    private boolean dialogueHologram = false;
    private double dialogueHologramOffset = 0.5;

    public NPC(String id, String displayName, Location location, float yaw,
               String skinTexture, String skinSignature,
               boolean lookAtPlayer, String dialogueId) {
        this.id = id;
        this.entityId = ENTITY_ID_COUNTER.getAndDecrement();
        this.entityUuid = UUID.nameUUIDFromBytes(("npc:" + id).getBytes(StandardCharsets.UTF_8));
        this.displayName = displayName;
        this.location = location.clone();
        this.yaw = yaw;
        this.skinTexture = skinTexture;
        this.skinSignature = skinSignature;
        this.lookAtPlayer = lookAtPlayer;
        this.dialogueId = dialogueId;
    }

    public void setLocation(Location location) {
        this.location = location.clone();
        this.yaw = location.getYaw();
    }

    // --- Getters and setters ---

    public String getId() { return id; }
    public int getEntityId() { return entityId; }
    public UUID getEntityUuid() { return entityUuid; }
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
    public boolean isDialogueHologram() { return dialogueHologram; }
    public void setDialogueHologram(boolean dialogueHologram) { this.dialogueHologram = dialogueHologram; }
    public double getDialogueHologramOffset() { return dialogueHologramOffset; }
    public void setDialogueHologramOffset(double offset) { this.dialogueHologramOffset = offset; }

    public String getWorldName() {
        return location.getWorld() != null ? location.getWorld().getName() : "world";
    }
}
