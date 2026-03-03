package net.axther.serverCore.reactive.effect;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Toggles the glowing effect on an armor stand with a team-based glow color.
 *
 * Config format:
 * <pre>
 * type: glow
 * color: AQUA
 * </pre>
 */
public class GlowToggleEffect implements ReactiveEffect {

    private static final String TEAM_PREFIX = "reactive_glow_";
    private final ChatColor color;

    public GlowToggleEffect(ChatColor color) {
        this.color = color;
    }

    @Override
    public void apply(ArmorStand stand, Player owner) {
        stand.setGlowing(true);

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = TEAM_PREFIX + color.name().toLowerCase();
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
            team.setColor(color);
        }

        String entry = stand.getUniqueId().toString();
        team.addEntry(entry);
    }

    @Override
    public void remove(ArmorStand stand, Player owner) {
        stand.setGlowing(false);

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = TEAM_PREFIX + color.name().toLowerCase();
        Team team = scoreboard.getTeam(teamName);
        if (team != null) {
            String entry = stand.getUniqueId().toString();
            team.removeEntry(entry);
        }
    }

    public static GlowToggleEffect parse(ConfigurationSection section) {
        String colorName = section.getString("color", "AQUA").toUpperCase();
        ChatColor chatColor;
        try {
            chatColor = ChatColor.valueOf(colorName);
        } catch (IllegalArgumentException e) {
            chatColor = ChatColor.AQUA;
        }
        return new GlowToggleEffect(chatColor);
    }
}
