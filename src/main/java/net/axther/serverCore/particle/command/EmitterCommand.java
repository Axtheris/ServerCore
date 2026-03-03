package net.axther.serverCore.particle.command;

import net.axther.serverCore.particle.*;
import net.axther.serverCore.particle.config.EmitterConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EmitterCommand implements TabExecutor {

    private static final List<String> SUBCOMMANDS = List.of("create", "remove", "list", "edit", "info");
    private static final List<String> EDITABLE_PROPS = List.of("particle", "pattern", "radius", "height", "speed", "count", "interval", "color", "size", "block-material");

    // Curated list of the most visually interesting particles for tab completion (all 115 still work by name)
    private static final List<String> SUGGESTED_PARTICLES = List.of(
            // Fire & smoke
            "flame", "soul_fire_flame", "small_flame", "campfire_cosy_smoke", "campfire_signal_smoke",
            "smoke", "large_smoke", "white_smoke", "lava", "dripping_lava", "landing_lava",
            // Magic & enchantment
            "enchant", "enchanted_hit", "end_rod", "portal", "reverse_portal", "witch",
            "totem_of_undying", "nautilus", "sculk_soul", "sculk_charge_pop",
            // Nature
            "heart", "happy_villager", "angry_villager", "cherry_leaves", "pale_oak_leaves",
            "falling_nectar", "spore_blossom_air", "snowflake", "crimson_spore", "warped_spore",
            // Water
            "bubble", "bubble_pop", "bubble_column_up", "splash", "dripping_water", "rain",
            "fishing", "dolphin", "squid_ink", "underwater",
            // Combat & effects
            "crit", "damage_indicator", "sweep_attack", "explosion", "sonic_boom",
            // Dust (colored)
            "dust", "dust_color_transition",
            // Ambient
            "cloud", "poof", "flash", "glow", "note", "firework",
            "dragon_breath", "electric_spark", "wax_on", "wax_off"
    );

    private final EmitterManager manager;
    private final EmitterConfig config;

    public EmitterCommand(EmitterManager manager, EmitterConfig config) {
        this.manager = manager;
        this.config = config;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /emitter <create|remove|list|edit|info>", NamedTextColor.YELLOW));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(player, args);
            case "remove" -> handleRemove(player, args);
            case "list" -> handleList(player);
            case "edit" -> handleEdit(player, args);
            case "info" -> handleInfo(player);
            default -> player.sendMessage(Component.text("Unknown subcommand. Use: create, remove, list, edit, info", NamedTextColor.RED));
        }
        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /emitter create <id> <particle> [pattern]", NamedTextColor.YELLOW));
            return;
        }

        String id = args[1];
        if (manager.getEmitter(id) != null) {
            player.sendMessage(Component.text("An emitter with ID '" + id + "' already exists.", NamedTextColor.RED));
            return;
        }

        Particle particle;
        try {
            particle = Particle.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text("Unknown particle type: " + args[2], NamedTextColor.RED));
            return;
        }

        EmitterPattern pattern = EmitterPattern.POINT;
        if (args.length >= 4) {
            try {
                pattern = EmitterPattern.valueOf(args[3].toUpperCase());
            } catch (IllegalArgumentException e) {
                player.sendMessage(Component.text("Unknown pattern: " + args[3], NamedTextColor.RED));
                return;
            }
        }

        Block target = player.getTargetBlockExact(5);
        if (target == null) {
            player.sendMessage(Component.text("Look at a block to place the emitter above it.", NamedTextColor.RED));
            return;
        }

        Location placeLoc = target.getLocation().add(0, 1, 0);
        if (placeLoc.getBlock().getType().isSolid()) {
            player.sendMessage(Component.text("No space above the target block.", NamedTextColor.RED));
            return;
        }

        EmitterData data = new EmitterData(particle, pattern, 1.0, 2.0, 0.02, 2, 2, null, 1.0f, null);
        EmitterInstance instance = manager.createEmitter(id, placeLoc, data);

        if (instance == null) {
            player.sendMessage(Component.text("Failed to create emitter (ID may already exist).", NamedTextColor.RED));
            return;
        }

        config.saveAll(manager);
        player.sendMessage(Component.text("Created emitter '", NamedTextColor.GREEN)
                .append(Component.text(id, NamedTextColor.WHITE))
                .append(Component.text("' with ", NamedTextColor.GREEN))
                .append(Component.text(particle.name(), NamedTextColor.AQUA))
                .append(Component.text(" " + pattern.name(), NamedTextColor.AQUA)));
    }

    private void handleRemove(Player player, String[] args) {
        if (args.length >= 2) {
            String id = args[1];
            if (manager.removeEmitter(id)) {
                config.saveAll(manager);
                player.sendMessage(Component.text("Removed emitter '", NamedTextColor.YELLOW)
                        .append(Component.text(id, NamedTextColor.WHITE))
                        .append(Component.text("'", NamedTextColor.YELLOW)));
            } else {
                player.sendMessage(Component.text("No emitter found with ID '" + id + "'.", NamedTextColor.RED));
            }
            return;
        }

        Block target = player.getTargetBlockExact(5);
        if (target == null) {
            player.sendMessage(Component.text("Usage: /emitter remove [id] or look at an emitter block.", NamedTextColor.YELLOW));
            return;
        }

        EmitterInstance emitter = manager.getEmitterAt(target.getLocation());
        if (emitter == null) {
            player.sendMessage(Component.text("No emitter at target block.", NamedTextColor.RED));
            return;
        }

        manager.removeEmitter(emitter.getId());
        config.saveAll(manager);
        player.sendMessage(Component.text("Removed emitter '", NamedTextColor.YELLOW)
                .append(Component.text(emitter.getId(), NamedTextColor.WHITE))
                .append(Component.text("'", NamedTextColor.YELLOW)));
    }

    private void handleList(Player player) {
        var emitters = manager.getAllEmitters();
        if (emitters.isEmpty()) {
            player.sendMessage(Component.text("No emitters active.", NamedTextColor.GRAY));
            return;
        }

        player.sendMessage(Component.text("--- Emitters (" + emitters.size() + ") ---", NamedTextColor.GREEN));
        for (EmitterInstance inst : emitters) {
            player.sendMessage(Component.text(" " + inst.getId(), NamedTextColor.WHITE)
                    .append(Component.text(" @ " + inst.getWorldName() + " " + inst.getBlockX() + ", " + inst.getBlockY() + ", " + inst.getBlockZ(), NamedTextColor.GRAY))
                    .append(Component.text(" | " + inst.getData().particle().name() + " " + inst.getData().pattern().name(), NamedTextColor.AQUA)));
        }
    }

    private void handleEdit(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(Component.text("Usage: /emitter edit <id> <property> <value>", NamedTextColor.YELLOW));
            return;
        }

        String id = args[1];
        EmitterInstance instance = manager.getEmitter(id);
        if (instance == null) {
            player.sendMessage(Component.text("No emitter found with ID '" + id + "'.", NamedTextColor.RED));
            return;
        }

        String prop = args[2].toLowerCase();
        String value = args[3];
        EmitterData old = instance.getData();

        try {
            EmitterData newData = switch (prop) {
                case "particle" -> new EmitterData(Particle.valueOf(value.toUpperCase()), old.pattern(), old.radius(), old.height(), old.speed(), old.count(), old.interval(), old.color(), old.size(), old.blockMaterial());
                case "pattern" -> new EmitterData(old.particle(), EmitterPattern.valueOf(value.toUpperCase()), old.radius(), old.height(), old.speed(), old.count(), old.interval(), old.color(), old.size(), old.blockMaterial());
                case "radius" -> new EmitterData(old.particle(), old.pattern(), Double.parseDouble(value), old.height(), old.speed(), old.count(), old.interval(), old.color(), old.size(), old.blockMaterial());
                case "height" -> new EmitterData(old.particle(), old.pattern(), old.radius(), Double.parseDouble(value), old.speed(), old.count(), old.interval(), old.color(), old.size(), old.blockMaterial());
                case "speed" -> new EmitterData(old.particle(), old.pattern(), old.radius(), old.height(), Double.parseDouble(value), old.count(), old.interval(), old.color(), old.size(), old.blockMaterial());
                case "count" -> new EmitterData(old.particle(), old.pattern(), old.radius(), old.height(), old.speed(), Integer.parseInt(value), old.interval(), old.color(), old.size(), old.blockMaterial());
                case "interval" -> new EmitterData(old.particle(), old.pattern(), old.radius(), old.height(), old.speed(), old.count(), Integer.parseInt(value), old.color(), old.size(), old.blockMaterial());
                case "color" -> {
                    String[] parts = value.split(",");
                    if (parts.length != 3) throw new IllegalArgumentException("Color format: r,g,b (e.g. 255,0,128)");
                    Color color = Color.fromRGB(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()), Integer.parseInt(parts[2].trim()));
                    yield new EmitterData(old.particle(), old.pattern(), old.radius(), old.height(), old.speed(), old.count(), old.interval(), color, old.size(), old.blockMaterial());
                }
                case "size" -> new EmitterData(old.particle(), old.pattern(), old.radius(), old.height(), old.speed(), old.count(), old.interval(), old.color(), Float.parseFloat(value), old.blockMaterial());
                case "block-material" -> new EmitterData(old.particle(), old.pattern(), old.radius(), old.height(), old.speed(), old.count(), old.interval(), old.color(), old.size(), Material.valueOf(value.toUpperCase()));
                default -> {
                    player.sendMessage(Component.text("Unknown property: " + prop + ". Editable: " + String.join(", ", EDITABLE_PROPS), NamedTextColor.RED));
                    yield null;
                }
            };

            if (newData != null) {
                manager.replaceEmitterData(id, newData);
                config.saveAll(manager);
                player.sendMessage(Component.text("Updated ", NamedTextColor.GREEN)
                        .append(Component.text(prop, NamedTextColor.WHITE))
                        .append(Component.text(" to ", NamedTextColor.GREEN))
                        .append(Component.text(value, NamedTextColor.AQUA))
                        .append(Component.text(" for emitter '", NamedTextColor.GREEN))
                        .append(Component.text(id, NamedTextColor.WHITE))
                        .append(Component.text("'", NamedTextColor.GREEN)));
            }
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text("Invalid value: " + e.getMessage(), NamedTextColor.RED));
        }
    }

    private void handleInfo(Player player) {
        Block target = player.getTargetBlockExact(5);
        if (target == null) {
            player.sendMessage(Component.text("Look at an emitter block.", NamedTextColor.YELLOW));
            return;
        }

        EmitterInstance emitter = manager.getEmitterAt(target.getLocation());
        if (emitter == null) {
            player.sendMessage(Component.text("No emitter at target block.", NamedTextColor.RED));
            return;
        }

        EmitterData d = emitter.getData();
        player.sendMessage(Component.text("--- Emitter: " + emitter.getId() + " ---", NamedTextColor.GREEN));
        player.sendMessage(Component.text("  Location: " + emitter.getWorldName() + " " + emitter.getBlockX() + ", " + emitter.getBlockY() + ", " + emitter.getBlockZ(), NamedTextColor.GRAY));
        player.sendMessage(Component.text("  Particle: ", NamedTextColor.GRAY).append(Component.text(d.particle().name(), NamedTextColor.AQUA)));
        player.sendMessage(Component.text("  Pattern: ", NamedTextColor.GRAY).append(Component.text(d.pattern().name(), NamedTextColor.AQUA)));
        player.sendMessage(Component.text("  Radius: " + d.radius() + " | Height: " + d.height() + " | Speed: " + d.speed(), NamedTextColor.GRAY));
        player.sendMessage(Component.text("  Count: " + d.count() + " | Interval: " + d.interval() + " ticks", NamedTextColor.GRAY));
        if (d.color() != null) {
            player.sendMessage(Component.text("  Color: " + d.color().getRed() + "," + d.color().getGreen() + "," + d.color().getBlue() + " | Size: " + d.size(), NamedTextColor.GRAY));
        }
        if (d.blockMaterial() != null) {
            player.sendMessage(Component.text("  Block Material: " + d.blockMaterial().name(), NamedTextColor.GRAY));
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }

        String sub = args[0].toLowerCase();

        if (args.length == 2) {
            if (sub.equals("remove") || sub.equals("edit")) {
                return filter(manager.getAllEmitters().stream().map(EmitterInstance::getId).toList(), args[1]);
            }
            if (sub.equals("create")) {
                return List.of("<id>");
            }
        }

        if (sub.equals("create")) {
            if (args.length == 3) {
                // Show curated list first, but all particles match if user types a specific name
                List<String> suggestions = filter(SUGGESTED_PARTICLES, args[2]);
                if (suggestions.isEmpty()) suggestions = filter(particleNames(), args[2]);
                return suggestions;
            }
            if (args.length == 4) {
                return filter(patternNames(), args[3]);
            }
        }

        if (sub.equals("edit")) {
            if (args.length == 3) {
                return filter(EDITABLE_PROPS, args[2]);
            }
            if (args.length == 4) {
                String prop = args[2].toLowerCase();
                return switch (prop) {
                    case "particle" -> {
                        List<String> suggestions = filter(SUGGESTED_PARTICLES, args[3]);
                        if (suggestions.isEmpty()) suggestions = filter(particleNames(), args[3]);
                        yield suggestions;
                    }
                    case "pattern" -> filter(patternNames(), args[3]);
                    case "block-material" -> filter(Arrays.stream(Material.values()).filter(Material::isBlock).map(m -> m.name().toLowerCase()).toList(), args[3]);
                    default -> List.of();
                };
            }
        }

        return List.of();
    }

    private List<String> particleNames() {
        return Arrays.stream(Particle.values()).map(p -> p.name().toLowerCase()).toList();
    }

    private List<String> patternNames() {
        return Arrays.stream(EmitterPattern.values()).map(p -> p.name().toLowerCase()).toList();
    }

    private List<String> filter(List<String> options, String input) {
        String lower = input.toLowerCase();
        return options.stream().filter(s -> s.toLowerCase().startsWith(lower)).collect(Collectors.toList());
    }
}
