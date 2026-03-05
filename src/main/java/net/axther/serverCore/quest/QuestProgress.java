package net.axther.serverCore.quest;

/**
 * Tracks a player's progress toward completing a quest's objectives.
 * Each index in the progress array corresponds to one {@link QuestObjective}.
 */
public class QuestProgress {

    private final String questId;
    private final int[] objectiveProgress;

    /**
     * Creates a new zeroed-out progress tracker.
     *
     * @param questId        the quest identifier
     * @param objectiveCount number of objectives in the quest
     */
    public QuestProgress(String questId, int objectiveCount) {
        this.questId = questId;
        this.objectiveProgress = new int[objectiveCount];
    }

    /**
     * Creates a progress tracker from an existing array (e.g. loaded from storage).
     *
     * @param questId  the quest identifier
     * @param progress pre-existing progress values
     */
    public QuestProgress(String questId, int[] progress) {
        this.questId = questId;
        this.objectiveProgress = progress.clone();
    }

    public String getQuestId() {
        return questId;
    }

    public int[] getObjectiveProgress() {
        return objectiveProgress;
    }

    /**
     * Returns the progress value for the given objective index,
     * or 0 if the index is out of bounds.
     */
    public int getProgress(int index) {
        if (index < 0 || index >= objectiveProgress.length) {
            return 0;
        }
        return objectiveProgress[index];
    }

    /**
     * Sets the progress value for the given objective index.
     * Out-of-bounds indices are silently ignored.
     */
    public void setProgress(int index, int value) {
        if (index >= 0 && index < objectiveProgress.length) {
            objectiveProgress[index] = value;
        }
    }

    /**
     * Increments the progress value at the given index by one.
     * Out-of-bounds indices are silently ignored.
     */
    public void increment(int index) {
        if (index >= 0 && index < objectiveProgress.length) {
            objectiveProgress[index]++;
        }
    }
}
