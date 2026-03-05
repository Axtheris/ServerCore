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

        @Test
        void newProgressHasStartedAtTimestamp() {
            QuestProgress progress = new QuestProgress("q", 2);
            assertTrue(progress.getStartedAt() > 0);
            assertTrue(progress.getStartedAt() <= System.currentTimeMillis());
        }

        @Test
        void existingProgressPreservesStartedAt() {
            long timestamp = 1000000L;
            QuestProgress progress = new QuestProgress("q", new int[]{1, 2}, timestamp);
            assertEquals(timestamp, progress.getStartedAt());
        }

        @Test
        void isExpiredReturnsFalseWhenNoLimit() {
            QuestProgress progress = new QuestProgress("q", 2);
            assertFalse(progress.isExpired(0));
        }

        @Test
        void isExpiredReturnsFalseWhenWithinLimit() {
            QuestProgress progress = new QuestProgress("q", 2);
            assertFalse(progress.isExpired(3600));
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

        @Test
        void craftObjectiveStoresCorrectly() {
            QuestObjective obj = new QuestObjective(QuestObjective.Type.CRAFT, "IRON_INGOT", 10, "Smelt some iron");
            assertEquals(QuestObjective.Type.CRAFT, obj.getType());
            assertEquals("IRON_INGOT", obj.getTarget());
            assertEquals(10, obj.getAmount());
            assertEquals("Smelt some iron", obj.getDescription());
        }

        @Test
        void mineObjectiveStoresCorrectly() {
            QuestObjective obj = new QuestObjective(QuestObjective.Type.MINE, "DIAMOND_ORE", 5, null);
            assertEquals(QuestObjective.Type.MINE, obj.getType());
            assertEquals(5, obj.getAmount());
            assertNull(obj.getDescription());
        }

        @Test
        void placeObjectiveStoresCorrectly() {
            QuestObjective obj = new QuestObjective(QuestObjective.Type.PLACE, "STONE", 64, "Place some stone");
            assertEquals(QuestObjective.Type.PLACE, obj.getType());
            assertEquals("STONE", obj.getTarget());
            assertEquals(64, obj.getAmount());
            assertEquals("Place some stone", obj.getDescription());
        }

        @Test
        void fishObjectiveStoresCorrectly() {
            QuestObjective obj = new QuestObjective(QuestObjective.Type.FISH, "COD", 3, "Catch some fish");
            assertEquals(QuestObjective.Type.FISH, obj.getType());
            assertEquals("COD", obj.getTarget());
            assertEquals(3, obj.getAmount());
            assertEquals("Catch some fish", obj.getDescription());
        }

        @Test
        void breedObjectiveStoresCorrectly() {
            QuestObjective obj = new QuestObjective(QuestObjective.Type.BREED, "COW", 2, "Breed cows");
            assertEquals(QuestObjective.Type.BREED, obj.getType());
            assertEquals("COW", obj.getTarget());
            assertEquals(2, obj.getAmount());
            assertEquals("Breed cows", obj.getDescription());
        }

        @Test
        void smeltObjectiveStoresCorrectly() {
            QuestObjective obj = new QuestObjective(QuestObjective.Type.SMELT, "GOLD_INGOT", 8, null);
            assertEquals(QuestObjective.Type.SMELT, obj.getType());
            assertEquals("GOLD_INGOT", obj.getTarget());
            assertEquals(8, obj.getAmount());
            assertNull(obj.getDescription());
        }

        @Test
        void exploreObjectiveStoresCorrectly() {
            QuestObjective obj = new QuestObjective(QuestObjective.Type.EXPLORE, "world,100,64,200", 1, null);
            assertEquals(QuestObjective.Type.EXPLORE, obj.getType());
            assertEquals("world,100,64,200", obj.getTarget());
            assertEquals(50.0, obj.getRadius());
        }

        @Test
        void exploreObjectiveCustomRadius() {
            QuestObjective obj = new QuestObjective(QuestObjective.Type.EXPLORE, "world,0,70,0", 1, "Find the village", 25.0);
            assertEquals(QuestObjective.Type.EXPLORE, obj.getType());
            assertEquals("world,0,70,0", obj.getTarget());
            assertEquals(25.0, obj.getRadius());
            assertEquals("Find the village", obj.getDescription());
        }

        @Test
        void interactObjectiveStoresCorrectly() {
            QuestObjective obj = new QuestObjective(QuestObjective.Type.INTERACT, "LEVER", 3, "Pull some levers");
            assertEquals(QuestObjective.Type.INTERACT, obj.getType());
            assertEquals("LEVER", obj.getTarget());
            assertEquals(3, obj.getAmount());
            assertEquals("Pull some levers", obj.getDescription());
        }

        @Test
        void descriptionOverrideOnExistingType() {
            QuestObjective obj = new QuestObjective(QuestObjective.Type.KILL, "SKELETON", 10, "Hunt skeletons in the graveyard");
            assertEquals(QuestObjective.Type.KILL, obj.getType());
            assertEquals("Hunt skeletons in the graveyard", obj.getDescription());
        }

        @Test
        void nullDescriptionOnExistingType() {
            QuestObjective obj = new QuestObjective(QuestObjective.Type.FETCH, "WHEAT", 32, null);
            assertNull(obj.getDescription());
        }

        @Test
        void oldConstructorDefaultsDescriptionAndRadius() {
            QuestObjective obj = new QuestObjective(QuestObjective.Type.FETCH, "OAK_LOG", 16);
            assertNull(obj.getDescription());
            assertEquals(50.0, obj.getRadius());
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

        @Test
        void moneyRewardStoresCorrectly() {
            QuestReward reward = new QuestReward(QuestReward.Type.MONEY, "", 500);
            assertEquals(QuestReward.Type.MONEY, reward.getType());
            assertEquals(500, reward.getAmount());
        }

        @Test
        void permissionRewardStoresCorrectly() {
            QuestReward reward = new QuestReward(QuestReward.Type.PERMISSION, "vip.access", 3600);
            assertEquals(QuestReward.Type.PERMISSION, reward.getType());
            assertEquals("vip.access", reward.getValue());
            assertEquals(3600, reward.getAmount());
        }

        @Test
        void petRewardStoresCorrectly() {
            QuestReward reward = new QuestReward(QuestReward.Type.PET, "dragon", 1);
            assertEquals(QuestReward.Type.PET, reward.getType());
            assertEquals("dragon", reward.getValue());
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

        @Test
        void questWithStructureParameters() {
            Quest quest = new Quest("chain-2", "<gold>Chain Quest 2", "Second in chain",
                    "npc1", "npc1", List.of(), List.of(),
                    false, 0,
                    "quests.advanced", List.of("chain-1"), 3600, "combat", true);
            assertEquals("quests.advanced", quest.getRequiredPermission());
            assertEquals(List.of("chain-1"), quest.getPrerequisites());
            assertEquals(3600, quest.getTimeLimit());
            assertEquals("combat", quest.getCategory());
            assertTrue(quest.isSequentialObjectives());
        }

        @Test
        void questDefaultStructureParameters() {
            Quest quest = new Quest("simple", "Simple", "Desc",
                    "npc1", "npc1", List.of(), List.of(), false, 0);
            assertNull(quest.getRequiredPermission());
            assertTrue(quest.getPrerequisites().isEmpty());
            assertEquals(0, quest.getTimeLimit());
            assertEquals("general", quest.getCategory());
            assertFalse(quest.isSequentialObjectives());
        }
    }
}
