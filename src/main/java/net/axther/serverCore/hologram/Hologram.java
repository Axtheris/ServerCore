package net.axther.serverCore.hologram;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Hologram {

    private final String id;
    private Location location;
    private final List<String> lines;
    private HologramAnimation animation;
    private double bobAmplitude;
    private double bobFrequency;
    private UUID entityUuid;

    public Hologram(String id, Location location, List<String> lines, HologramAnimation animation,
                    double bobAmplitude, double bobFrequency) {
        this.id = id;
        this.location = location.clone();
        this.lines = new ArrayList<>(lines);
        this.animation = animation;
        this.bobAmplitude = bobAmplitude;
        this.bobFrequency = bobFrequency;
    }

    public Hologram(String id, Location location, List<String> lines) {
        this(id, location, lines, HologramAnimation.NONE, 0.1, 0.08);
    }

    public void spawn() {
        if (isSpawned()) return;

        World world = location.getWorld();
        if (world == null) return;
        if (!world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) return;

        TextDisplay display = world.spawn(location, TextDisplay.class, entity -> {
            entity.text(buildText());
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            entity.setPersistent(false);
            entity.setAlignment(TextDisplay.TextAlignment.CENTER);
            entity.setShadowed(true);
        });

        this.entityUuid = display.getUniqueId();
    }

    public void despawn() {
        if (entityUuid == null) return;

        Entity entity = Bukkit.getEntity(entityUuid);
        if (entity != null) {
            entity.remove();
        }
        entityUuid = null;
    }

    public void tick(int tickCount) {
        if (!isSpawned()) return;

        Entity entity = Bukkit.getEntity(entityUuid);
        if (entity == null || !(entity instanceof TextDisplay display)) {
            entityUuid = null;
            return;
        }

        switch (animation) {
            case BOB -> {
                double offsetY = Math.sin(tickCount * bobFrequency) * bobAmplitude;
                Location bobLoc = location.clone().add(0, offsetY, 0);
                display.teleport(bobLoc);
            }
            case ROTATE -> {
                float angle = (tickCount * 2.0f) % 360.0f;
                float radians = (float) Math.toRadians(angle);
                Quaternionf rotation = new Quaternionf().rotateY(radians);
                display.setTransformation(new org.bukkit.util.Transformation(
                        new org.joml.Vector3f(0, 0, 0),
                        rotation,
                        new org.joml.Vector3f(1, 1, 1),
                        new Quaternionf()
                ));
                display.setInterpolationDuration(1);
                display.setInterpolationDelay(0);
            }
            case PULSE -> {
                float scale = 1.0f + 0.15f * (float) Math.sin(tickCount * bobFrequency);
                display.setTransformation(new org.bukkit.util.Transformation(
                        new org.joml.Vector3f(0, 0, 0),
                        new Quaternionf(),
                        new org.joml.Vector3f(scale, scale, scale),
                        new Quaternionf()
                ));
                display.setInterpolationDuration(1);
                display.setInterpolationDelay(0);
            }
            case NONE -> {}
        }
    }

    public void updateText() {
        if (!isSpawned()) return;

        Entity entity = Bukkit.getEntity(entityUuid);
        if (entity instanceof TextDisplay display) {
            display.text(buildText());
        }
    }

    public boolean isSpawned() {
        if (entityUuid == null) return false;
        Entity entity = Bukkit.getEntity(entityUuid);
        return entity != null && !entity.isDead();
    }

    private Component buildText() {
        MiniMessage mm = MiniMessage.miniMessage();
        Component combined = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) combined = combined.append(Component.newline());
            combined = combined.append(mm.deserialize(lines.get(i)));
        }
        return combined;
    }

    // --- Getters and setters ---

    public String getId() { return id; }

    public Location getLocation() { return location.clone(); }

    public void setLocation(Location location) {
        this.location = location.clone();
        if (isSpawned()) {
            Entity entity = Bukkit.getEntity(entityUuid);
            if (entity != null) {
                entity.teleport(location);
            }
        }
    }

    public List<String> getLines() { return lines; }

    public HologramAnimation getAnimation() { return animation; }

    public void setAnimation(HologramAnimation animation) { this.animation = animation; }

    public double getBobAmplitude() { return bobAmplitude; }

    public void setBobAmplitude(double bobAmplitude) { this.bobAmplitude = bobAmplitude; }

    public double getBobFrequency() { return bobFrequency; }

    public void setBobFrequency(double bobFrequency) { this.bobFrequency = bobFrequency; }

    public UUID getEntityUuid() { return entityUuid; }

    public String getWorldName() {
        return location.getWorld() != null ? location.getWorld().getName() : "world";
    }

    public long getChunkKey() {
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }
}
