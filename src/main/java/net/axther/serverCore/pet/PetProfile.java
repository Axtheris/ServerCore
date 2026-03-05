package net.axther.serverCore.pet;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PetProfile {

    public static final NamespacedKey PET_ID_KEY = new NamespacedKey("servercore", "pet_id");

    private final String id;
    private final String displayName;
    private final ItemStack headItem;

    // Item representation
    private final String itemName;
    private final List<String> itemLore;

    // Animation
    private final double bobAmplitude;
    private final double bobFrequency;
    private final double hoverHeight;

    // Movement
    private final double followSpeed;
    private final double followStartDistance;
    private final double followStopDistance;
    private final double teleportDistance;

    // Combat
    private final boolean canAttack;
    private final double attackRange;
    private final double attackDamage;
    private final int attackCooldownTicks;

    // Feed
    private final int feedCooldownTicks;
    private final int heartParticleCount;

    // Display
    private final boolean useSmallStand;

    // Behavior
    private final boolean passiveByDefault;

    // Animation type
    private final PetAnimationType animationType;

    // Sounds
    private final List<PetSound> ambientSounds;

    // Model Engine (optional)
    private final String modelId;
    private final Map<String, String> modelAnimations;

    public PetProfile(String id, String displayName, ItemStack headItem,
                      String itemName, List<String> itemLore,
                      double bobAmplitude, double bobFrequency, double hoverHeight,
                      double followSpeed, double followStartDistance, double followStopDistance, double teleportDistance,
                      boolean canAttack, double attackRange, double attackDamage, int attackCooldownTicks,
                      int feedCooldownTicks, int heartParticleCount, boolean useSmallStand,
                      boolean passiveByDefault,
                      PetAnimationType animationType,
                      List<PetSound> ambientSounds,
                      String modelId, Map<String, String> modelAnimations) {
        this.id = id;
        this.displayName = displayName;
        this.headItem = headItem;
        this.itemName = itemName;
        this.itemLore = itemLore;
        this.bobAmplitude = bobAmplitude;
        this.bobFrequency = bobFrequency;
        this.hoverHeight = hoverHeight;
        this.followSpeed = followSpeed;
        this.followStartDistance = followStartDistance;
        this.followStopDistance = followStopDistance;
        this.teleportDistance = teleportDistance;
        this.canAttack = canAttack;
        this.attackRange = attackRange;
        this.attackDamage = attackDamage;
        this.attackCooldownTicks = attackCooldownTicks;
        this.feedCooldownTicks = feedCooldownTicks;
        this.heartParticleCount = heartParticleCount;
        this.useSmallStand = useSmallStand;
        this.passiveByDefault = passiveByDefault;
        this.animationType = animationType;
        this.ambientSounds = ambientSounds;
        this.modelId = modelId;
        this.modelAnimations = modelAnimations != null ? modelAnimations : Collections.emptyMap();
    }

    /** Creates the giveable pet item with name, lore, and PDC pet ID tag. */
    public ItemStack createItem() {
        ItemStack item = headItem.clone();
        ItemMeta meta = item.getItemMeta();
        MiniMessage mm = MiniMessage.miniMessage();

        meta.displayName(mm.deserialize(itemName));
        meta.lore(itemLore.stream().map(mm::deserialize).toList());
        meta.getPersistentDataContainer().set(PET_ID_KEY, PersistentDataType.STRING, id);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Called every tick for custom behavior. Base implementation plays configured ambient sounds.
     * Override in Java profiles for additional behavior (call super to keep ambient sounds).
     */
    public void onTick(PetInstance instance) {
        if (ambientSounds.isEmpty()) return;
        ArmorStand stand = instance.getStand();
        if (stand == null) return;
        for (PetSound sound : ambientSounds) {
            sound.tryPlay(stand, instance.getTickCounter());
        }
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public ItemStack getHeadItem() { return headItem; }
    public String getItemName() { return itemName; }
    public List<String> getItemLore() { return itemLore; }
    public double getBobAmplitude() { return bobAmplitude; }
    public double getBobFrequency() { return bobFrequency; }
    public double getHoverHeight() { return hoverHeight; }
    public double getFollowSpeed() { return followSpeed; }
    public double getFollowStartDistance() { return followStartDistance; }
    public double getFollowStopDistance() { return followStopDistance; }
    public double getTeleportDistance() { return teleportDistance; }
    public boolean canAttack() { return canAttack; }
    public double getAttackRange() { return attackRange; }
    public double getAttackDamage() { return attackDamage; }
    public int getAttackCooldownTicks() { return attackCooldownTicks; }
    public int getFeedCooldownTicks() { return feedCooldownTicks; }
    public int getHeartParticleCount() { return heartParticleCount; }
    public boolean useSmallStand() { return useSmallStand; }
    public boolean isPassiveByDefault() { return passiveByDefault; }
    public PetAnimationType getAnimationType() { return animationType; }
    public List<PetSound> getAmbientSounds() { return ambientSounds; }
    public String getModelId() { return modelId; }
    public Map<String, String> getModelAnimations() { return modelAnimations; }
}
