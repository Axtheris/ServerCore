package net.axther.serverCore.quest;

public class QuestProgress {

    private final String questId;
    private final int[] objectiveProgress;

    public QuestProgress(String questId, int objectiveCount) {
        this.questId = questId;
        this.objectiveProgress = new int[objectiveCount];
    }

    public QuestProgress(String questId, int[] objectiveProgress) {
        this.questId = questId;
        this.objectiveProgress = objectiveProgress;
    }

    public String getQuestId() { return questId; }
    public int[] getObjectiveProgress() { return objectiveProgress; }

    public int getProgress(int index) {
        return index >= 0 && index < objectiveProgress.length ? objectiveProgress[index] : 0;
    }

    public void setProgress(int index, int value) {
        if (index >= 0 && index < objectiveProgress.length) {
            objectiveProgress[index] = value;
        }
    }

    public void increment(int index) {
        if (index >= 0 && index < objectiveProgress.length) {
            objectiveProgress[index]++;
        }
    }
}
