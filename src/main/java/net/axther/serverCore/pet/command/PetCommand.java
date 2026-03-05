package net.axther.serverCore.pet.command;

import net.axther.serverCore.gui.Menu;
import net.axther.serverCore.gui.MenuItem;
import net.axther.serverCore.gui.PaginatedMenu;
import net.axther.serverCore.pet.*;
import net.axther.serverCore.pet.config.PetConfig;
import net.axther.serverCore.pet.data.PetStore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PetCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("summon", "dismiss", "sit", "follow", "feed", "list", "give", "reload", "gui");

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
            player.sendMessage("Usage: /pet <summon|dismiss|sit|follow|feed|list|give|reload|gui>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "summon" -> {
                if (!player.hasPermission("servercore.pet.summon")) {
                    player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    return true;
                }
                handleSummon(player, args);
            }
            case "dismiss" -> {
                if (!player.hasPermission("servercore.pet.dismiss")) {
                    player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    return true;
                }
                handleDismiss(player);
            }
            case "sit" -> {
                if (!player.hasPermission("servercore.pet.sit")) {
                    player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    return true;
                }
                handleSit(player);
            }
            case "follow" -> {
                if (!player.hasPermission("servercore.pet.follow")) {
                    player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    return true;
                }
                handleFollow(player);
            }
            case "feed" -> {
                if (!player.hasPermission("servercore.pet.feed")) {
                    player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    return true;
                }
                handleFeed(player);
            }
            case "list" -> {
                if (!player.hasPermission("servercore.pet.list")) {
                    player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    return true;
                }
                handleList(player);
            }
            case "give" -> {
                if (!player.hasPermission("servercore.pet.give")) {
                    player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    return true;
                }
                handleGive(player, args);
            }
            case "reload" -> {
                if (!player.hasPermission("servercore.pet.reload")) {
                    player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    return true;
                }
                handleReload(player);
            }
            case "gui" -> {
                if (!player.hasPermission("servercore.pet.gui")) {
                    player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    return true;
                }
                handleGui(player);
            }
            default -> player.sendMessage("Usage: /pet <summon|dismiss|sit|follow|feed|list|give|reload|gui>");
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

        PetStore store = manager.getStore();
        if (store != null && !store.ownsPet(player.getUniqueId(), petId)) {
            player.sendMessage("You don't own this pet type.");
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
        PetStore store = manager.getStore();
        if (store != null) {
            var owned = store.getOwnedPets(player.getUniqueId());
            if (owned.isEmpty()) {
                player.sendMessage("You don't own any pets.");
                return;
            }
            player.sendMessage("Your pets: " + String.join(", ", owned));
        } else {
            var ids = manager.getRegisteredPetIds();
            if (ids.isEmpty()) {
                player.sendMessage("No pet types registered.");
                return;
            }
            player.sendMessage("Available pets: " + String.join(", ", ids));
        }
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

        PetStore giveStore = manager.getStore();
        if (giveStore != null) {
            giveStore.addPet(target.getUniqueId(), petId);
        }

        if (target == player) {
            player.sendMessage("You received a " + profile.getDisplayName() + " pet item!");
        } else {
            player.sendMessage("Gave " + profile.getDisplayName() + " pet item to " + target.getName() + ".");
            target.sendMessage("You received a " + profile.getDisplayName() + " pet item!");
        }
    }

    private void handleGui(Player player) {
        MiniMessage mm = MiniMessage.miniMessage();
        PetStore store = manager.getStore();

        // Determine which pets to show: owned pets if store exists, otherwise all registered
        Set<String> petIds;
        if (store != null) {
            petIds = store.getOwnedPets(player.getUniqueId());
        } else {
            petIds = manager.getRegisteredPetIds();
        }

        List<MenuItem> items = new ArrayList<>();
        for (String petId : petIds) {
            PetProfile profile = manager.getProfile(petId);
            if (profile == null) continue;

            ItemStack display = profile.getHeadItem().clone();
            ItemMeta meta = display.getItemMeta();

            meta.displayName(mm.deserialize("<gold>" + profile.getDisplayName()));

            List<Component> lore = new ArrayList<>();
            lore.add(mm.deserialize("<gray>ID: " + profile.getId()));

            // Show status if currently summoned
            boolean isActive = manager.hasPetType(player.getUniqueId(), petId);
            if (isActive) {
                List<PetInstance> pets = manager.getPets(player.getUniqueId());
                for (PetInstance pet : pets) {
                    if (pet.getProfile().getId().equalsIgnoreCase(petId)) {
                        String stateLabel = switch (pet.getState()) {
                            case FOLLOWING -> "<green>Following";
                            case SITTING -> "<yellow>Sitting";
                            case ATTACKING -> "<red>Attacking";
                        };
                        lore.add(mm.deserialize("<gray>Status: " + stateLabel));
                        break;
                    }
                }
                lore.add(Component.empty());
                lore.add(mm.deserialize("<red>Click to dismiss"));
            } else {
                lore.add(Component.empty());
                lore.add(mm.deserialize("<yellow>Click to summon"));
            }

            meta.lore(lore);
            display.setItemMeta(meta);

            final String finalPetId = petId;
            items.add(MenuItem.builder(display)
                    .onClick(p -> {
                        if (manager.hasPetType(p.getUniqueId(), finalPetId)) {
                            manager.dismissPetType(p.getUniqueId(), finalPetId);
                            p.sendMessage("Dismissed " + profile.getDisplayName() + ".");
                        } else {
                            manager.summonPet(p, profile);
                            p.sendMessage("Summoned " + profile.getDisplayName() + "!");
                        }
                        p.closeInventory();
                    })
                    .build());
        }

        if (items.isEmpty()) {
            player.sendMessage("You don't own any pets.");
            return;
        }

        PaginatedMenu menu = PaginatedMenu.paginatedBuilder("<gradient:#5B86E5:#36D1DC>Pet Collection</gradient>")
                .contentItems(items)
                .build();
        menu.open(player);
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
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(prefix))
                    .filter(s -> !(sender instanceof Player p) || p.hasPermission("servercore.pet." + s))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("summon")) {
            String prefix = args[1].toLowerCase();
            PetStore tabStore = manager.getStore();
            if (tabStore != null && sender instanceof Player tabPlayer) {
                return tabStore.getOwnedPets(tabPlayer.getUniqueId()).stream()
                        .filter(s -> s.startsWith(prefix))
                        .filter(s -> manager.hasProfile(s))
                        .toList();
            }
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
