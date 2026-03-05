package net.axther.serverCore.quest;

public class QuestProgress {

    private final String questId;
    private final int[] objectiveProgress;
    private final long startedAt;

    public QuestProgress(String questId, int objectiveCount) {
        this.questId = questId;
        this.objectiveProgress = new int[objectiveCount];
        this.startedAt = System.currentTimeMillis();
    }

    public QuestProgress(String questId, int[] objectiveProgress) {
        this(questId, objectiveProgress, System.currentTimeMillis());
    }

    public QuestProgress(String questId, int[] objectiveProgress, long startedAt) {
        this.questId = questId;
        this.objectiveProgress = objectiveProgress;
        this.startedAt = startedAt;
    }

    public String getQuestId() { return questId; }
    public int[] getObjectiveProgress() { return objectiveProgress; }
    public long getStartedAt() { return startedAt; }

    public boolean isExpired(int timeLimitSeconds) {
        if (timeLimitSeconds <= 0) return false;
        long elapsed = (System.currentTimeMillis() - startedAt) / 1000;
        return elapsed >= timeLimitSeconds;
    }

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
