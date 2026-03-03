package net.axther.serverCore.timeline;

import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

/**
 * Defines a timeline -- a sequence of keyframes that play out over a duration.
 * Timelines are loaded from YAML and can be played at any location.
 */
public class Timeline {

    private final String id;
    private final boolean loop;
    private final String audience;
    private final double radius;
    private final Map<Integer, Keyframe> keyframes;
    private final int duration;

    public Timeline(String id, boolean loop, String audience, double radius,
                    Map<Integer, Keyframe> keyframes, int duration) {
        this.id = id;
        this.loop = loop;
        this.audience = audience;
        this.radius = radius;
        this.keyframes = Collections.unmodifiableMap(keyframes);
        this.duration = duration;
    }

    public String getId() {
        return id;
    }

    public boolean isLoop() {
        return loop;
    }

    public String getAudience() {
        return audience;
    }

    public double getRadius() {
        return radius;
    }

    public Map<Integer, Keyframe> getKeyframes() {
        return keyframes;
    }

    public int getDuration() {
        return duration;
    }

    /**
     * Parse a Timeline from a YAML configuration section.
     *
     * @param id      the timeline identifier
     * @param section the YAML section containing timeline data
     * @return the parsed Timeline
     */
    @SuppressWarnings("unchecked")
    public static Timeline fromConfig(String id, ConfigurationSection section) {
        boolean loop = section.getBoolean("loop", false);
        String audience = section.getString("audience", "nearby");
        double radius = section.getDouble("radius", 50.0);

        Map<Integer, Keyframe> keyframes = new LinkedHashMap<>();
        int maxTick = 0;

        ConfigurationSection kfSection = section.getConfigurationSection("keyframes");
        if (kfSection != null) {
            for (String tickKey : kfSection.getKeys(false)) {
                int tick;
                try {
                    tick = Integer.parseInt(tickKey);
                } catch (NumberFormatException e) {
                    continue;
                }

                List<Map<?, ?>> actionConfigs = kfSection.getMapList(tickKey);
                if (!actionConfigs.isEmpty()) {
                    keyframes.put(tick, Keyframe.fromConfig(tick, actionConfigs));
                    if (tick > maxTick) {
                        maxTick = tick;
                    }
                }
            }
        }

        // Duration is the last keyframe tick + 1 so that the final keyframe executes
        int duration = section.getInt("duration", maxTick + 1);

        return new Timeline(id, loop, audience, radius, keyframes, duration);
    }
}
