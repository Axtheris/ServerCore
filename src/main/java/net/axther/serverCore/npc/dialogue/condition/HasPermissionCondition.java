package net.axther.serverCore.npc.dialogue.condition;

import org.bukkit.entity.Player;

public class HasPermissionCondition implements DialogueCondition {

    private final String permission;

    public HasPermissionCondition(String permission) {
        this.permission = permission;
    }

    @Override
    public boolean test(Player player) {
        return player.hasPermission(permission);
    }
}
