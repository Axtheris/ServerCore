package net.axther.serverCore.quest;

/**
 * Defines a single objective within a quest (e.g. kill 10 zombies, fetch 16 oak logs).
 */
public class QuestObjective {

    public enum Type {
        KILL,
        FETCH,
        TALK
    }

    private final Type type;
    private final String target;
    private final int amount;

    /**
     * @param type   the objective type
     * @param target the target identifier (entity type, material name, or NPC id)
     * @param amount the required count to complete this objective
     */
    public QuestObjective(Type type, String target, int amount) {
        this.type = type;
        this.target = target;
        this.amount = amount;
    }

    public Type getType() {
        return type;
    }

    public String getTarget() {
        return target;
    }

    public int getAmount() {
        return amount;
    }
}
