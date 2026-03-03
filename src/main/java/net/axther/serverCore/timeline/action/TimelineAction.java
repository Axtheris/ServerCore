package net.axther.serverCore.timeline.action;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;

/**
 * Represents a single action that can be executed at a keyframe within a timeline.
 */
public interface TimelineAction {

    /**
     * Execute this action.
     *
     * @param origin   the location where the timeline was triggered
     * @param audience the players who should see/hear the action
     */
    void execute(Location origin, Collection<Player> audience);

    /**
     * Deserialize a TimelineAction from a configuration map.
     *
     * @param map the configuration map (from YAML list entry)
     * @return the parsed action, or null if the type is unknown
     */
    static TimelineAction fromConfig(Map<?, ?> map) {
        String type = String.valueOf(map.get("type"));
        return switch (type.toLowerCase()) {
            case "sound" -> PlaySoundAction.fromConfig(map);
            case "title" -> SendTitleAction.fromConfig(map);
            case "command" -> RunCommandAction.fromConfig(map);
            case "mob" -> SpawnMobAction.fromConfig(map);
            case "time" -> SetTimeAction.fromConfig(map);
            case "weather" -> SetWeatherAction.fromConfig(map);
            case "camera-shake" -> CameraShakeAction.fromConfig(map);
            default -> null;
        };
    }
}
