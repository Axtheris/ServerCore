package net.axther.serverCore.quest;

/**
 * Represents a reward granted upon quest completion.
 */
public class QuestReward {

    public enum Type {
        ITEM,
        XP,
        COMMAND
    }

    private final Type type;
    private final String value;
    private final int amount;

    /**
     * @param type   the reward type
     * @param value  contextual value (material name, command string, etc.)
     * @param amount the quantity (item count, XP amount, or 1 for commands)
     */
    public QuestReward(Type type, String value, int amount) {
        this.type = type;
        this.value = value;
        this.amount = amount;
    }

    public Type getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public int getAmount() {
        return amount;
    }
}
