package net.axther.serverCore.particle.listener;

import net.axther.serverCore.particle.EmitterInstance;
import net.axther.serverCore.particle.EmitterManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class EmitterLifecycleListener implements Listener {

    private final EmitterManager manager;

    public EmitterLifecycleListener(EmitterManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.LIGHT) return;

        EmitterInstance emitter = manager.getEmitterAt(event.getBlock().getLocation());
        if (emitter == null) return;

        event.setCancelled(true);
        manager.removeEmitter(emitter.getId());
        event.getPlayer().sendMessage(Component.text("Removed emitter '", NamedTextColor.YELLOW)
                .append(Component.text(emitter.getId(), NamedTextColor.WHITE))
                .append(Component.text("'", NamedTextColor.YELLOW)));
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() != Material.LIGHT) return;

        EmitterInstance emitter = manager.getEmitterAt(event.getClickedBlock().getLocation());
        if (emitter == null) return;

        event.getPlayer().sendMessage(Component.text("Emitter: ", NamedTextColor.GREEN)
                .append(Component.text(emitter.getId(), NamedTextColor.WHITE))
                .append(Component.text(" | ", NamedTextColor.GRAY))
                .append(Component.text(emitter.getData().particle().name(), NamedTextColor.AQUA))
                .append(Component.text(" | ", NamedTextColor.GRAY))
                .append(Component.text(emitter.getData().pattern().name(), NamedTextColor.AQUA)));
    }
}
