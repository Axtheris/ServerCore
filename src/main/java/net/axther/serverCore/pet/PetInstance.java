package net.axther.serverCore.pet;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;

import net.axther.serverCore.pet.model.ModelEngineHook;

import java.util.UUID;

public class PetInstance {

    private final UUID ownerUuid;
    private final UUID standUuid;
    private final PetProfile profile;

    private PetState state = PetState.FOLLOWING;
    private long tickCounter = 0;
    private int attackCooldown = 0;
    private int feedCooldown = 0;
    private boolean usingModelEngine = false;

    // Smooth movement tracking
    private double currentX;
    private double currentY;
    private double currentZ;
    private float currentYaw;
    private boolean positionInitialized = false;

    // Attack tracking
    private UUID attackTargetUuid;

    public PetInstance(UUID ownerUuid, UUID standUuid, PetProfile profile) {
        this.ownerUuid = ownerUuid;
        this.standUuid = standUuid;
        this.profile = profile;
    }

    /**
     * Main tick method. Returns false if the instance should be removed.
     */
    public boolean tick() {
        Player owner = getOwner();
        ArmorStand stand = getStand();

        if (owner == null || !owner.isOnline() || stand == null || stand.isDead()) {
            destroy();
            return false;
        }

        tickCounter++;
        if (attackCooldown > 0) attackCooldown--;
        if (feedCooldown > 0) feedCooldown--;

        // Initialize position on first tick
        if (!positionInitialized) {
            Location loc = owner.getLocation();
            currentX = loc.getX();
            currentY = loc.getY() + profile.getHoverHeight();
            currentZ = loc.getZ();
            currentYaw = loc.getYaw();
            positionInitialized = true;
        }

        switch (state) {
            case FOLLOWING -> tickFollowing(owner, stand);
            case SITTING -> tickSitting(stand);
            case ATTACKING -> tickAttacking(owner, stand);
        }

        // Call profile-specific tick hook
        profile.onTick(this);

        return true;
    }

    private void tickFollowing(Player owner, ArmorStand stand) {
        // Check for attack target if combat-enabled
        if (profile.canAttack()) {
            LivingEntity target = findNearestHostile(owner);
            if (target != null) {
                attackTargetUuid = target.getUniqueId();
                state = PetState.ATTACKING;
                tickAttacking(owner, stand);
                return;
            }
        }

        moveTowardOwner(owner, stand);
    }

    private void tickSitting(ArmorStand stand) {
        double[] offsets = computeAnimationOffsets();
        Location loc = stand.getLocation();
        loc.setX(currentX + offsets[0]);
        loc.setY(currentY + offsets[1]);
        loc.setZ(currentZ + offsets[2]);
        stand.teleport(loc);
    }

    private void tickAttacking(Player owner, ArmorStand stand) {
        if (attackTargetUuid == null) {
            state = PetState.FOLLOWING;
            return;
        }

        var entity = Bukkit.getEntity(attackTargetUuid);
        if (!(entity instanceof LivingEntity target) || target.isDead()) {
            attackTargetUuid = null;
            state = PetState.FOLLOWING;
            return;
        }

        // Disengage if target is too far from owner
        double distToOwner = target.getLocation().distance(owner.getLocation());
        if (distToOwner > profile.getAttackRange() * 2) {
            attackTargetUuid = null;
            state = PetState.FOLLOWING;
            moveTowardOwner(owner, stand);
            return;
        }

        // Move toward target at 1.5x speed
        Location targetLoc = target.getLocation().add(0, target.getHeight() / 2, 0);
        double dx = targetLoc.getX() - currentX;
        double dy = targetLoc.getY() - currentY;
        double dz = targetLoc.getZ() - currentZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        double speed = profile.getFollowSpeed() * 1.5;
        double lerpFactor = Math.min(speed / Math.max(distance, 0.01), 1.0);
        currentX += dx * lerpFactor;
        currentY += dy * 0.2;
        currentZ += dz * lerpFactor;

        double[] offsets = computeAnimationOffsets();

        // Attack if close enough and cooldown ready
        if (distance < 1.5 && attackCooldown <= 0) {
            target.damage(profile.getAttackDamage(), owner);
            // Crit particles
            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, target.getHeight() / 2, 0), 8, 0.3, 0.3, 0.3, 0.1);
            attackCooldown = profile.getAttackCooldownTicks();
        }

        Location standLoc = new Location(owner.getWorld(), currentX + offsets[0], currentY + offsets[1], currentZ + offsets[2], currentYaw, 0);
        stand.teleport(standLoc);
    }

    private void moveTowardOwner(Player owner, ArmorStand stand) {
        Location ownerLoc = owner.getLocation();

        // Target position: 2 blocks behind + 1 block right, rotated by yaw
        double yawRad = Math.toRadians(ownerLoc.getYaw());
        double targetX = ownerLoc.getX() + (-Math.sin(yawRad) * -2) + (-Math.cos(yawRad) * -1);
        double targetY = ownerLoc.getY() + profile.getHoverHeight();
        double targetZ = ownerLoc.getZ() + (Math.cos(yawRad) * -2) + (-Math.sin(yawRad) * -1);

        double dx = targetX - currentX;
        double dy = targetY - currentY;
        double dz = targetZ - currentZ;
        double distance = Math.sqrt(dx * dx + dz * dz);

        // Teleport if too far
        if (distance > profile.getTeleportDistance()) {
            currentX = targetX;
            currentY = targetY;
            currentZ = targetZ;
            currentYaw = ownerLoc.getYaw();
        } else if (distance > profile.getFollowStopDistance()) {
            // Lerp toward target (rubber-band movement)
            double lerpFactor = Math.min(profile.getFollowSpeed() / Math.max(distance, 0.01), 1.0);
            currentX += dx * lerpFactor;
            currentZ += dz * lerpFactor;
        }

        // Smooth vertical tracking
        currentY += dy * 0.15;

        // Smooth yaw toward player direction
        float targetYaw = ownerLoc.getYaw();
        float yawDiff = targetYaw - currentYaw;
        // Normalize to -180..180
        while (yawDiff > 180) yawDiff -= 360;
        while (yawDiff < -180) yawDiff += 360;
        currentYaw += yawDiff * 0.2f;

        double[] offsets = computeAnimationOffsets();

        Location standLoc = new Location(owner.getWorld(), currentX + offsets[0], currentY + offsets[1], currentZ + offsets[2], currentYaw, 0);
        stand.teleport(standLoc);
    }

    private double[] computeAnimationOffsets() {
        double amplitude = profile.getBobAmplitude();
        double frequency = profile.getBobFrequency();
        double offsetX = 0, offsetY = 0, offsetZ = 0;

        switch (profile.getAnimationType()) {
            case FLOAT -> offsetY = amplitude * Math.sin(frequency * tickCounter);
            case FLY -> {
                offsetY = amplitude * Math.sin(frequency * tickCounter);
                offsetX = (amplitude * 0.5) * Math.sin(frequency * 0.7 * tickCounter);
                offsetZ = (amplitude * 0.3) * Math.cos(frequency * 0.5 * tickCounter);
            }
            case HOP -> {
                int hopInterval = 40;
                int hopDuration = 12;
                int cyclePos = (int) (tickCounter % hopInterval);
                if (cyclePos < hopDuration) {
                    double progress = (double) cyclePos / hopDuration;
                    offsetY = amplitude * 2.5 * Math.sin(Math.PI * progress);
                }
            }
            case ORBIT -> {
                double orbitAngle = frequency * 0.5 * tickCounter;
                offsetX = amplitude * 3.0 * Math.sin(orbitAngle);
                offsetZ = amplitude * 3.0 * Math.cos(orbitAngle);
                offsetY = amplitude * 0.5 * Math.sin(frequency * 2 * tickCounter);
            }
            case BOUNCE -> offsetY = amplitude * Math.abs(Math.sin(frequency * 1.5 * tickCounter));
        }

        return new double[]{offsetX, offsetY, offsetZ};
    }

    private LivingEntity findNearestHostile(Player owner) {
        double rangeSq = profile.getAttackRange() * profile.getAttackRange();
        LivingEntity nearest = null;
        double nearestDistSq = rangeSq;

        for (var entity : owner.getNearbyEntities(profile.getAttackRange(), profile.getAttackRange(), profile.getAttackRange())) {
            if (entity instanceof Monster monster && !monster.isDead()) {
                double distSq = monster.getLocation().distanceSquared(owner.getLocation());
                if (distSq < nearestDistSq) {
                    nearestDistSq = distSq;
                    nearest = monster;
                }
            }
        }

        return nearest;
    }

    /**
     * Feed the pet with heart particles.
     * @return true if fed successfully, false if still on cooldown
     */
    public boolean feed() {
        if (feedCooldown > 0) return false;

        ArmorStand stand = getStand();
        if (stand == null) return false;

        Location loc = stand.getLocation().add(0, 0.5, 0);
        stand.getWorld().spawnParticle(Particle.HEART, loc, profile.getHeartParticleCount(), 0.4, 0.3, 0.4, 0.02);

        feedCooldown = profile.getFeedCooldownTicks();
        return true;
    }

    public int getRemainingFeedCooldownSeconds() {
        return feedCooldown / 20;
    }

    public void setState(PetState state) {
        if (state == PetState.FOLLOWING) {
            attackTargetUuid = null;
        }
        this.state = state;

        if (usingModelEngine) {
            String stateKey = switch (state) {
                case FOLLOWING -> "walk";
                case SITTING -> "sit";
                case ATTACKING -> "attack";
            };
            String animName = profile.getModelAnimations().get(stateKey);
            if (animName != null) {
                ArmorStand stand = getStand();
                if (stand != null) {
                    ModelEngineHook.playAnimation(stand, animName);
                }
            }
        }
    }

    public PetState getState() { return state; }

    public void destroy() {
        ArmorStand stand = getStand();
        if (stand != null && !stand.isDead()) {
            if (usingModelEngine) {
                ModelEngineHook.removeModel(stand);
            }
            stand.remove();
        }
    }

    public void setUsingModelEngine(boolean usingModelEngine) {
        this.usingModelEngine = usingModelEngine;
    }

    public boolean isUsingModelEngine() {
        return usingModelEngine;
    }

    public UUID getOwnerUuid() { return ownerUuid; }
    public UUID getStandUuid() { return standUuid; }
    public PetProfile getProfile() { return profile; }
    public long getTickCounter() { return tickCounter; }

    public Player getOwner() {
        return Bukkit.getPlayer(ownerUuid);
    }

    public ArmorStand getStand() {
        var entity = Bukkit.getEntity(standUuid);
        return entity instanceof ArmorStand stand ? stand : null;
    }
}
