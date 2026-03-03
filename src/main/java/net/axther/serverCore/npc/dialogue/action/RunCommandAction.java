package net.axther.serverCore.npc.dialogue.action;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class RunCommandAction implements DialogueAction {

    private final String command;

    public RunCommandAction(String command) {
        this.command = command;
    }

    @Override
    public void execute(Player player) {
        String resolved = command.replace("%player%", player.getName());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
    }
}
