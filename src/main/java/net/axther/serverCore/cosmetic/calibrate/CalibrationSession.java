package net.axther.serverCore.cosmetic.calibrate;

import net.axther.serverCore.cosmetic.CosmeticManager;
import net.axther.serverCore.cosmetic.MobCosmeticProfile;
import net.axther.serverCore.cosmetic.config.CosmeticConfig;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CalibrationSession {

    private final Player player;
    private final LivingEntity mob;
    private final EntityType entityType;
    private final CosmeticManager manager;
    private final CosmeticConfig config;
    private final ItemStack previewItem;

    private double headY;
    private double headForwardZ;
    private double headSideX;
    private boolean useSmallStand;
    private double step = 0.05;

    private final MobCosmeticProfile originalProfile;
    private final boolean originalAI;
    private boolean active = true;

    public CalibrationSession(Player player, LivingEntity mob, CosmeticManager manager, CosmeticConfig config, ItemStack previewItem) {
        this.player = player;
        this.mob = mob;
        this.entityType = mob.getType();
        this.manager = manager;
        this.config = config;
        this.previewItem = previewItem.clone();

        // Copy offsets from existing profile if one exists
        this.originalProfile = manager.getProfile(entityType);
        if (originalProfile != null) {
            this.headY = originalProfile.getHeadY();
            this.headForwardZ = originalProfile.getHeadForwardZ();
            this.headSideX = originalProfile.getHeadSideX();
            this.useSmallStand = originalProfile.useSmallStand();
        } else {
            this.headY = 1.0;
            this.headForwardZ = 0.0;
            this.headSideX = 0.0;
            this.useSmallStand = false;
        }

        // Store original AI state, then disable AI
        if (mob instanceof Mob m) {
            this.originalAI = m.isAware();
            m.setAware(false);
        } else {
            this.originalAI = true;
        }

        // Apply initial preview
        rebuildPreview();
        sendFeedback();

        player.sendMessage("Calibration started for " + entityType.name().toLowerCase() + ".");
        player.sendMessage("Use /cosmetic calibrate <y|fwd|side> <+|-> to adjust offsets.");
        player.sendMessage("Use /cosmetic calibrate small to toggle small stand.");
        player.sendMessage("Use /cosmetic calibrate step <value> to change step size.");
        player.sendMessage("Use /cosmetic calibrate save to save, or cancel to discard.");
    }

    public void adjustY(double delta) {
        headY += delta;
        rebuildPreview();
        sendFeedback();
    }

    public void adjustForward(double delta) {
        headForwardZ += delta;
        rebuildPreview();
        sendFeedback();
    }

    public void adjustSide(double delta) {
        headSideX += delta;
        rebuildPreview();
        sendFeedback();
    }

    public void toggleSmallStand() {
        useSmallStand = !useSmallStand;
        rebuildPreview();
        sendFeedback();
    }

    public void setStep(double step) {
        this.step = step;
        sendFeedback();
    }

    private void rebuildPreview() {
        manager.removeCosmetics(mob.getUniqueId());
        manager.registerProfile(entityType, new MobCosmeticProfile(headY, headForwardZ, headSideX, useSmallStand));
        manager.applyCosmetic(mob, previewItem);
    }

    private void sendFeedback() {
        String msg = String.format("Y: %.2f | Fwd: %.2f | Side: %.2f | Small: %s | Step: %.2f",
                headY, headForwardZ, headSideX, useSmallStand ? "yes" : "no", step);
        player.sendActionBar(Component.text(msg));
    }

    public void save() {
        MobCosmeticProfile finalProfile = new MobCosmeticProfile(headY, headForwardZ, headSideX, useSmallStand);
        manager.registerProfile(entityType, finalProfile);
        config.saveProfile(entityType, finalProfile);
        player.sendMessage("Calibration saved for " + entityType.name().toLowerCase() + ".");
        end();
    }

    public void cancel() {
        // Restore original profile or remove if none existed
        if (originalProfile != null) {
            manager.registerProfile(entityType, originalProfile);
        }
        player.sendMessage("Calibration cancelled.");
        end();
    }

    private void end() {
        if (!active) return;
        active = false;

        // Remove preview cosmetic
        manager.removeCosmetics(mob.getUniqueId());

        // Restore AI
        if (mob instanceof Mob m && !mob.isDead()) {
            m.setAware(originalAI);
        }
    }

    public boolean isActive() {
        return active;
    }

    public Player getPlayer() {
        return player;
    }

    public LivingEntity getMob() {
        return mob;
    }

    public double getStep() {
        return step;
    }
}
