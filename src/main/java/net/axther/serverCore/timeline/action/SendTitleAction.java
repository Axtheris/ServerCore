package net.axther.serverCore.timeline.action;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;

/**
 * Sends a title and subtitle to the audience using MiniMessage formatting.
 * Config: type: title, title: "...", subtitle: "...", fade-in: 10, stay: 40, fade-out: 10
 */
public class SendTitleAction implements TimelineAction {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final String titleText;
    private final String subtitleText;
    private final int fadeIn;
    private final int stay;
    private final int fadeOut;

    public SendTitleAction(String titleText, String subtitleText, int fadeIn, int stay, int fadeOut) {
        this.titleText = titleText;
        this.subtitleText = subtitleText;
        this.fadeIn = fadeIn;
        this.stay = stay;
        this.fadeOut = fadeOut;
    }

    @Override
    public void execute(Location origin, Collection<Player> audience) {
        Component title = titleText != null && !titleText.isEmpty()
                ? MINI.deserialize(titleText)
                : Component.empty();
        Component subtitle = subtitleText != null && !subtitleText.isEmpty()
                ? MINI.deserialize(subtitleText)
                : Component.empty();

        Title.Times times = Title.Times.times(
                Duration.ofMillis(fadeIn * 50L),
                Duration.ofMillis(stay * 50L),
                Duration.ofMillis(fadeOut * 50L)
        );

        Title titleObj = Title.title(title, subtitle, times);

        for (Player player : audience) {
            player.showTitle(titleObj);
        }
    }

    public static SendTitleAction fromConfig(Map<?, ?> map) {
        String title = map.containsKey("title") ? String.valueOf(map.get("title")) : "";
        String subtitle = map.containsKey("subtitle") ? String.valueOf(map.get("subtitle")) : "";
        int fadeIn = map.containsKey("fade-in") ? ((Number) map.get("fade-in")).intValue() : 10;
        int stay = map.containsKey("stay") ? ((Number) map.get("stay")).intValue() : 40;
        int fadeOut = map.containsKey("fade-out") ? ((Number) map.get("fade-out")).intValue() : 10;
        return new SendTitleAction(title, subtitle, fadeIn, stay, fadeOut);
    }
}
