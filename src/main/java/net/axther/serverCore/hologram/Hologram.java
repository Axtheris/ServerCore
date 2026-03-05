package net.axther.serverCore.hologram;

import net.axther.serverCore.hologram.action.HologramAction;
import net.axther.serverCore.hologram.condition.HologramCondition;
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

    private static final Color TRANSPARENT = Color.fromARGB(0, 0, 0, 0);

    private final String id;
    private Location location;
    private final List<String> lines;
    private HologramAnimation animation;
    private double bobAmplitude;
    private double bobFrequency;
    private UUID entityUuid;

    // Visual styling options
    private String billboard = "CENTER";
    private boolean textShadow = false;
    private String background = null;
    private int lineWidth = 200;
    private boolean seeThrough = false;
    private String textAlignment = "CENTER";
    private float viewRange = 1.0f;

    private int updateInterval = 20; // ticks between placeholder refreshes

    private final List<HologramCondition> conditions = new ArrayList<>();
    private final List<HologramAction> actions = new ArrayList<>();
    private int clickCooldown = 20;

    public Hologram(String id, Location location, List<String> lines, HologramAnimation animation,
                    double bobAmplitude, double bobFrequency) {
        this.id = id;
        this.location = location != null ? location.clone() : null;
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

            Display.Billboard billboardEnum;
            try {
                billboardEnum = Display.Billboard.valueOf(billboard);
            } catch (IllegalArgumentException e) {
                billboardEnum = Display.Billboard.CENTER;
            }
            entity.setBillboard(billboardEnum);

            if (background != null) {
                try {
                    long argb = Long.parseUnsignedLong(background.replace("#", ""), 16);
                    int a = (int) ((argb >> 24) & 0xFF);
                    int r = (int) ((argb >> 16) & 0xFF);
                    int g = (int) ((argb >> 8) & 0xFF);
                    int b = (int) (argb & 0xFF);
                    entity.setBackgroundColor(Color.fromARGB(a, r, g, b));
                } catch (NumberFormatException e) {
                    entity.setBackgroundColor(TRANSPARENT);
                }
            } else {
                entity.setBackgroundColor(TRANSPARENT);
            }

            entity.setPersistent(false);

            TextDisplay.TextAlignment alignmentEnum;
            try {
                alignmentEnum = TextDisplay.TextAlignment.valueOf(textAlignment);
            } catch (IllegalArgumentException e) {
                alignmentEnum = TextDisplay.TextAlignment.CENTER;
            }
            entity.setAlignment(alignmentEnum);

            entity.setShadowed(textShadow);
            entity.setLineWidth(lineWidth);
            entity.setSeeThrough(seeThrough);
            entity.setViewRange(viewRange);
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
            String line = lines.get(i);
            // Resolve PlaceholderAPI placeholders if present
            if (line.contains("%") && line.indexOf('%') != line.lastIndexOf('%')) {
                line = resolvePlaceholders(line);
            }
            combined = combined.append(mm.deserialize(line));
        }
        return combined;
    }

    private String resolvePlaceholders(String text) {
        try {
            Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            java.lang.reflect.Method setPlaceholders = papiClass.getMethod("setPlaceholders",
                    org.bukkit.OfflinePlayer.class, String.class);
            return (String) setPlaceholders.invoke(null, (org.bukkit.OfflinePlayer) null, text);
        } catch (Exception e) {
            return text; // PlaceholderAPI not present, return as-is
        }
    }

    // --- Getters and setters ---

    public String getId() { return id; }

    public Location getLocation() { return location != null ? location.clone() : null; }

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

    // --- Visual option getters and setters ---

    public String getBillboard() { return billboard; }

    public void setBillboard(String billboard) { this.billboard = billboard; }

    public boolean isTextShadow() { return textShadow; }

    public void setTextShadow(boolean textShadow) { this.textShadow = textShadow; }

    public String getBackground() { return background; }

    public void setBackground(String background) { this.background = background; }

    public int getLineWidth() { return lineWidth; }

    public void setLineWidth(int lineWidth) { this.lineWidth = lineWidth; }

    public boolean isSeeThrough() { return seeThrough; }

    public void setSeeThrough(boolean seeThrough) { this.seeThrough = seeThrough; }

    public String getTextAlignment() { return textAlignment; }

    public void setTextAlignment(String textAlignment) { this.textAlignment = textAlignment; }

    public float getViewRange() { return viewRange; }

    public void setViewRange(float viewRange) { this.viewRange = viewRange; }

    public int getUpdateInterval() { return updateInterval; }

    public void setUpdateInterval(int updateInterval) { this.updateInterval = updateInterval; }

    public List<HologramCondition> getConditions() { return conditions; }
    public boolean hasConditions() { return !conditions.isEmpty(); }

    public List<HologramAction> getActions() { return actions; }
    public int getClickCooldown() { return clickCooldown; }
    public void setClickCooldown(int clickCooldown) { this.clickCooldown = clickCooldown; }

    public boolean containsPlaceholders() {
        for (String line : lines) {
            if (line.contains("%") && line.indexOf('%') != line.lastIndexOf('%')) {
                return true;
            }
        }
        return false;
    }

    public void refreshPlaceholders() {
        if (!isSpawned() || !containsPlaceholders()) return;
        updateText();
    }

    public String getWorldName() {
        return location != null && location.getWorld() != null ? location.getWorld().getName() : "world";
    }

    public long getChunkKey() {
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }
}
