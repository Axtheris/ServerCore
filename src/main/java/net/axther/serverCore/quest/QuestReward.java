package net.axther.serverCore.quest;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class QuestReward {

    public enum Type { ITEM, XP, COMMAND }

    private final Type type;
    private final String value;  // material name, xp amount, or command string
    private final int amount;

    public QuestReward(Type type, String value, int amount) {
        this.type = type;
        this.value = value;
        this.amount = amount;
    }

    public static QuestReward fromConfig(ConfigurationSection section) {
        String typeStr = section.getString("type", "item");
        return switch (typeStr.toLowerCase()) {
            case "xp" -> new QuestReward(Type.XP, "", section.getInt("amount", 0));
            case "command" -> new QuestReward(Type.COMMAND, section.getString("value", ""), 1);
            default -> new QuestReward(Type.ITEM,
                    section.getString("material", "DIRT"), section.getInt("amount", 1));
        };
    }

    public void give(Player player) {
        switch (type) {
            case ITEM -> {
                Material material = Material.matchMaterial(value);
                if (material != null) {
                    player.getInventory().addItem(new ItemStack(material, amount));
                }
            }
            case XP -> player.giveExp(amount);
            case COMMAND -> {
                String cmd = value.replace("%player%", player.getName());
                player.getServer().dispatchCommand(player.getServer().getConsoleSender(), cmd);
            }
        }
    }

    public Type getType() { return type; }
    public String getValue() { return value; }
    public int getAmount() { return amount; }
}
