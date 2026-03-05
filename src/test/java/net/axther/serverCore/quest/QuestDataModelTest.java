package net.axther.serverCore.quest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuestDataModelTest {

    // --- QuestProgress tests ---

    @Nested
    class QuestProgressTests {
        @Test
        void constructorCreatesZeroedProgressArray() {
            QuestProgress progress = new QuestProgress("test-quest", 3);
            assertEquals("test-quest", progress.getQuestId());
            assertEquals(3, progress.getObjectiveProgress().length);
            for (int val : progress.getObjectiveProgress()) {
                assertEquals(0, val);
            }
        }

        @Test
        void constructorWithExistingArray() {
            int[] existing = {5, 10, 0};
            QuestProgress progress = new QuestProgress("test-quest", existing);
            assertArrayEquals(existing, progress.getObjectiveProgress());
        }

        @Test
        void getProgressReturnsZeroForOutOfBoundsIndex() {
            QuestProgress progress = new QuestProgress("q", 2);
            assertEquals(0, progress.getProgress(-1));
            assertEquals(0, progress.getProgress(5));
        }

        @Test
        void setProgressUpdatesCorrectly() {
            QuestProgress progress = new QuestProgress("q", 3);
            progress.setProgress(1, 42);
            assertEquals(42, progress.getProgress(1));
            assertEquals(0, progress.getProgress(0));
        }

        @Test
        void setProgressIgnoresOutOfBounds() {
            QuestProgress progress = new QuestProgress("q", 2);
            progress.setProgress(-1, 10);
            progress.setProgress(5, 10);
            // Should not throw; array unchanged
            assertEquals(0, progress.getProgress(0));
            assertEquals(0, progress.getProgress(1));
        }

        @Test
        void incrementIncrementsCorrectIndex() {
            QuestProgress progress = new QuestProgress("q", 3);
            progress.increment(0);
            progress.increment(0);
            progress.increment(2);
            assertEquals(2, progress.getProgress(0));
            assertEquals(0, progress.getProgress(1));
            assertEquals(1, progress.getProgress(2));
        }

        @Test
        void incrementIgnoresOutOfBounds() {
            QuestProgress progress = new QuestProgress("q", 1);
            progress.increment(-1);
            progress.increment(99);
            assertEquals(0, progress.getProgress(0));
        }
    }

    // --- QuestObjective tests ---

    @Nested
    class QuestObjectiveTests {
        @Test
        void fetchObjectiveStoresCorrectly() {
            QuestObjective obj = new QuestObjective(QuestObjective.Type.FETCH, "OAK_LOG", 16);
            assertEquals(QuestObjective.Type.FETCH, obj.getType());
            assertEquals("OAK_LOG", obj.getTarget());
            assertEquals(16, obj.getAmount());
        }

        @Test
        void killObjectiveStoresCorrectly() {
            QuestObjective obj = new QuestObjective(QuestObjective.Type.KILL, "ZOMBIE", 10);
            assertEquals(QuestObjective.Type.KILL, obj.getType());
            assertEquals("ZOMBIE", obj.getTarget());
            assertEquals(10, obj.getAmount());
        }

        @Test
        void talkObjectiveUsesAmountOne() {
            QuestObjective obj = new QuestObjective(QuestObjective.Type.TALK, "merchant", 1);
            assertEquals(QuestObjective.Type.TALK, obj.getType());
            assertEquals("merchant", obj.getTarget());
            assertEquals(1, obj.getAmount());
        }
    }

    // --- QuestReward tests ---

    @Nested
    class QuestRewardTests {
        @Test
        void itemRewardStoresCorrectly() {
            QuestReward reward = new QuestReward(QuestReward.Type.ITEM, "DIAMOND", 3);
            assertEquals(QuestReward.Type.ITEM, reward.getType());
            assertEquals("DIAMOND", reward.getValue());
            assertEquals(3, reward.getAmount());
        }

        @Test
        void xpRewardStoresCorrectly() {
            QuestReward reward = new QuestReward(QuestReward.Type.XP, "", 100);
            assertEquals(QuestReward.Type.XP, reward.getType());
            assertEquals(100, reward.getAmount());
        }

        @Test
        void commandRewardStoresCorrectly() {
            QuestReward reward = new QuestReward(QuestReward.Type.COMMAND, "eco give %player% 500", 1);
            assertEquals(QuestReward.Type.COMMAND, reward.getType());
            assertEquals("eco give %player% 500", reward.getValue());
        }
    }

    // --- Quest tests ---

    @Nested
    class QuestTests {
        @Test
        void questStoresAllFields() {
            List<QuestObjective> objectives = List.of(
                new QuestObjective(QuestObjective.Type.FETCH, "OAK_LOG", 16),
                new QuestObjective(QuestObjective.Type.KILL, "ZOMBIE", 5)
            );
            List<QuestReward> rewards = List.of(
                new QuestReward(QuestReward.Type.ITEM, "DIAMOND", 3),
                new QuestReward(QuestReward.Type.XP, "", 100)
            );

            Quest quest = new Quest("gather-wood", "<gold>Lumberjack", "Gather logs.",
                    "merchant", "merchant", objectives, rewards, true, 3600);

            assertEquals("gather-wood", quest.getId());
            assertEquals("<gold>Lumberjack", quest.getDisplayName());
            assertEquals("Gather logs.", quest.getDescription());
            assertEquals("merchant", quest.getAcceptNpc());
            assertEquals("merchant", quest.getTurnInNpc());
            assertEquals(2, quest.getObjectives().size());
            assertEquals(2, quest.getRewards().size());
            assertTrue(quest.isRepeatable());
            assertEquals(3600, quest.getCooldownSeconds());
        }

        @Test
        void nonRepeatableQuestDefaults() {
            Quest quest = new Quest("one-time", "Quest", "Desc",
                    "npc1", "npc1", List.of(), List.of(), false, 0);

            assertFalse(quest.isRepeatable());
            assertEquals(0, quest.getCooldownSeconds());
        }
    }
}
