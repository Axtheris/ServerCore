package net.axther.serverCore.timeline.action;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;

/**
 * Simulates a camera shake effect by sending rapid empty title flashes
 * with very short stay times to create a screen jitter effect.
 * Config: type: camera-shake, duration: 10
 */
public class CameraShakeAction implements TimelineAction {

    private final int durationTicks;

    public CameraShakeAction(int durationTicks) {
        this.durationTicks = durationTicks;
    }

    @Override
    public void execute(Location origin, Collection<Player> audience) {
        if (origin.getWorld() == null) return;

        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(CameraShakeAction.class);

        new BukkitRunnable() {
            int remaining = durationTicks;

            @Override
            public void run() {
                if (remaining <= 0) {
                    // Send a clear title to reset
                    for (Player player : audience) {
                        if (player.isOnline()) {
                            player.clearTitle();
                        }
                    }
                    cancel();
                    return;
                }

                Title.Times times = Title.Times.times(
                        Duration.ZERO,
                        Duration.ofMillis(50),
                        Duration.ZERO
                );

                // Alternate between a barely visible title and empty to create flicker
                Component shakeText = remaining % 2 == 0
                        ? Component.text("\n\n\n")
                        : Component.empty();

                Title title = Title.title(shakeText, Component.empty(), times);

                for (Player player : audience) {
                    if (player.isOnline()) {
                        player.showTitle(title);
                    }
                }

                remaining--;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public static CameraShakeAction fromConfig(Map<?, ?> map) {
        int duration = map.containsKey("duration") ? ((Number) map.get("duration")).intValue() : 10;
        return new CameraShakeAction(duration);
    }
}
