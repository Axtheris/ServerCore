package net.axther.serverCore.pet.command;

import net.axther.serverCore.pet.*;
import net.axther.serverCore.pet.config.PetConfig;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class PetCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("summon", "dismiss", "sit", "follow", "feed", "list", "give", "reload");

    private final PetManager manager;
    private final PetConfig config;
    private final Runnable reregisterJavaProfiles;

    public PetCommand(PetManager manager, PetConfig config, Runnable reregisterJavaProfiles) {
        this.manager = manager;
        this.config = config;
        this.reregisterJavaProfiles = reregisterJavaProfiles;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("Usage: /pet <summon|dismiss|sit|follow|feed|list|give|reload>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "summon" -> handleSummon(player, args);
            case "dismiss" -> handleDismiss(player);
            case "sit" -> handleSit(player);
            case "follow" -> handleFollow(player);
            case "feed" -> handleFeed(player);
            case "list" -> handleList(player);
            case "give" -> handleGive(player, args);
            case "reload" -> handleReload(player);
            default -> player.sendMessage("Usage: /pet <summon|dismiss|sit|follow|feed|list|give|reload>");
        }

        return true;
    }

    private void handleSummon(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("Usage: /pet summon <type>");
            return;
        }

        String petId = args[1].toLowerCase();
        PetProfile profile = manager.getProfile(petId);
        if (profile == null) {
            player.sendMessage("Unknown pet type: " + petId);
            return;
        }

        manager.summonPet(player, profile);
        player.sendMessage("Summoned " + profile.getDisplayName() + "!");
    }

    private void handleDismiss(Player player) {
        if (!manager.hasPets(player.getUniqueId())) {
            player.sendMessage("You have no active pets.");
            return;
        }

        manager.dismissPet(player);
        player.sendMessage("Pets dismissed.");
    }

    private void handleSit(Player player) {
        List<PetInstance> pets = manager.getPets(player.getUniqueId());
        if (pets.isEmpty()) {
            player.sendMessage("You have no active pets.");
            return;
        }

        for (PetInstance pet : pets) {
            pet.setState(PetState.SITTING);
        }
        player.sendMessage("Pets are now sitting.");
    }

    private void handleFollow(Player player) {
        List<PetInstance> pets = manager.getPets(player.getUniqueId());
        if (pets.isEmpty()) {
            player.sendMessage("You have no active pets.");
            return;
        }

        for (PetInstance pet : pets) {
            pet.setState(PetState.FOLLOWING);
        }
        player.sendMessage("Pets are now following you.");
    }

    private void handleFeed(Player player) {
        List<PetInstance> pets = manager.getPets(player.getUniqueId());
        if (pets.isEmpty()) {
            player.sendMessage("You have no active pets.");
            return;
        }

        for (PetInstance pet : pets) {
            if (!pet.feed()) {
                int seconds = pet.getRemainingFeedCooldownSeconds();
                player.sendMessage(pet.getProfile().getDisplayName() + " is not hungry yet. (" + seconds + "s remaining)");
            } else {
                player.sendMessage(pet.getProfile().getDisplayName() + " enjoyed the treat!");
            }
        }
    }

    private void handleList(Player player) {
        var ids = manager.getRegisteredPetIds();
        if (ids.isEmpty()) {
            player.sendMessage("No pet types registered.");
            return;
        }

        player.sendMessage("Available pets: " + String.join(", ", ids));
    }

    private void handleGive(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("Usage: /pet give <type> [player]");
            return;
        }

        String petId = args[1].toLowerCase();
        PetProfile profile = manager.getProfile(petId);
        if (profile == null) {
            player.sendMessage("Unknown pet type: " + petId);
            return;
        }

        Player target = player;
        if (args.length >= 3) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                player.sendMessage("Player not found: " + args[2]);
                return;
            }
        }

        target.getInventory().addItem(profile.createItem());
        if (target == player) {
            player.sendMessage("You received a " + profile.getDisplayName() + " pet item!");
        } else {
            player.sendMessage("Gave " + profile.getDisplayName() + " pet item to " + target.getName() + ".");
            target.sendMessage("You received a " + profile.getDisplayName() + " pet item!");
        }
    }

    private void handleReload(Player player) {
        // Re-register Java profiles first, then load config
        manager.clearProfiles();
        reregisterJavaProfiles.run();
        config.loadAndRegister(manager);
        player.sendMessage("Pet config reloaded. " + manager.getRegisteredPetIds().size() + " pet types loaded.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return SUBCOMMANDS.stream().filter(s -> s.startsWith(prefix)).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("summon")) {
            String prefix = args[1].toLowerCase();
            return manager.getRegisteredPetIds().stream().filter(s -> s.startsWith(prefix)).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            String prefix = args[1].toLowerCase();
            return manager.getRegisteredPetIds().stream().filter(s -> s.startsWith(prefix)).toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            String prefix = args[2].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(prefix))
                    .toList();
        }
        return List.of();
    }
}
