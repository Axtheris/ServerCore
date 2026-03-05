# Quest Parameters Expansion Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Expand the quest system with 8 new objective types, 3 new reward types (with soft Vault dependency), quest structure parameters (prerequisites, permissions, time limits, categories, sequential objectives, max-active cap), and action bar progress notifications.

**Architecture:** Extend the existing enum-driven `QuestObjective.Type` and `QuestReward.Type` enums. Add new fields to `Quest` with backwards-compatible defaults. New event listeners in `QuestListener`. Vault integration via isolated `VaultHook` class (same classloading pattern as `ModelEngineHook`). Time-limit expiry checked in listener handlers.

**Tech Stack:** Paper 1.21 API, Vault API (compileOnly), JUnit 5

---

### Task 1: Add Vault compileOnly dependency

**Files:**
- Modify: `build.gradle:29-38`

**Step 1: Add Vault repo and dependency**

Add to `repositories`:
```groovy
maven {
    name = "jitpack"
    url = "https://jitpack.io"
}
```

Add to `dependencies`:
```groovy
compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
```

**Step 2: Verify build compiles**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add build.gradle
git commit -m "Add Vault API as compileOnly dependency for quest rewards"
```

---

### Task 2: Create VaultHook utility class

**Files:**
- Create: `src/main/java/net/axther/serverCore/hook/VaultHook.java`

**Step 1: Write VaultHook**

```java
package net.axther.serverCore.hook;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class VaultHook {

    private static Economy economy;
    private static Permission permission;

    private VaultHook() {}

    public static boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return true;
    }

    public static boolean setupPermissions() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Permission> rsp = Bukkit.getServicesManager().getRegistration(Permission.class);
        if (rsp == null) return false;
        permission = rsp.getProvider();
        return true;
    }

    public static boolean hasEconomy() { return economy != null; }
    public static boolean hasPermissions() { return permission != null; }

    public static boolean deposit(Player player, double amount) {
        if (economy == null) return false;
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    public static boolean addPermission(Player player, String node) {
        if (permission == null) return false;
        return permission.playerAdd(player, node);
    }

    public static boolean removePermission(Player player, String node) {
        if (permission == null) return false;
        return permission.playerRemove(player, node);
    }
}
```

**Step 2: Verify build compiles**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add src/main/java/net/axther/serverCore/hook/VaultHook.java
git commit -m "Add VaultHook for soft Vault economy and permissions integration"
```

---

### Task 3: Expand QuestObjective with new types

**Files:**
- Modify: `src/main/java/net/axther/serverCore/quest/QuestObjective.java`
- Modify: `src/test/java/net/axther/serverCore/quest/QuestDataModelTest.java`

**Step 1: Write failing tests for new objective types and description field**

Add to `QuestDataModelTest.QuestObjectiveTests`:

```java
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
    assertEquals("DIAMOND_ORE", obj.getTarget());
    assertEquals(5, obj.getAmount());
    assertNull(obj.getDescription());
}

@Test
void placeObjectiveStoresCorrectly() {
    QuestObjective obj = new QuestObjective(QuestObjective.Type.PLACE, "STONE", 64, null);
    assertEquals(QuestObjective.Type.PLACE, obj.getType());
    assertEquals(64, obj.getAmount());
}

@Test
void fishObjectiveStoresCorrectly() {
    QuestObjective obj = new QuestObjective(QuestObjective.Type.FISH, "ANY", 3, null);
    assertEquals(QuestObjective.Type.FISH, obj.getType());
    assertEquals("ANY", obj.getTarget());
}

@Test
void breedObjectiveStoresCorrectly() {
    QuestObjective obj = new QuestObjective(QuestObjective.Type.BREED, "COW", 2, null);
    assertEquals(QuestObjective.Type.BREED, obj.getType());
}

@Test
void smeltObjectiveStoresCorrectly() {
    QuestObjective obj = new QuestObjective(QuestObjective.Type.SMELT, "IRON_INGOT", 16, null);
    assertEquals(QuestObjective.Type.SMELT, obj.getType());
}

@Test
void exploreObjectiveStoresCorrectly() {
    QuestObjective obj = new QuestObjective(QuestObjective.Type.EXPLORE, "world,100,64,200", 1, null);
    assertEquals(QuestObjective.Type.EXPLORE, obj.getType());
    assertEquals("world,100,64,200", obj.getTarget());
    assertEquals(50.0, obj.getRadius());
}

@Test
void interactObjectiveStoresCorrectly() {
    QuestObjective obj = new QuestObjective(QuestObjective.Type.INTERACT, "CRAFTING_TABLE", 3, null);
    assertEquals(QuestObjective.Type.INTERACT, obj.getType());
}

@Test
void descriptionOverridesDefault() {
    QuestObjective obj = new QuestObjective(QuestObjective.Type.KILL, "ZOMBIE", 5, "Slay the undead");
    assertEquals("Slay the undead", obj.getDescription());
}

@Test
void nullDescriptionAllowed() {
    QuestObjective obj = new QuestObjective(QuestObjective.Type.KILL, "ZOMBIE", 5, null);
    assertNull(obj.getDescription());
}
```

**Step 2: Run tests to verify they fail**

Run: `./gradlew test`
Expected: FAIL — new constructor and types don't exist yet

**Step 3: Update QuestObjective.java**

Replace entire file with:

```java
package net.axther.serverCore.quest;

import org.bukkit.configuration.ConfigurationSection;

public class QuestObjective {

    public enum Type { FETCH, KILL, TALK, CRAFT, MINE, PLACE, FISH, BREED, SMELT, EXPLORE, INTERACT }

    private final Type type;
    private final String target;
    private final int amount;
    private final String description;
    private final double radius; // only for EXPLORE

    public QuestObjective(Type type, String target, int amount) {
        this(type, target, amount, null);
    }

    public QuestObjective(Type type, String target, int amount, String description) {
        this(type, target, amount, description, 50.0);
    }

    public QuestObjective(Type type, String target, int amount, String description, double radius) {
        this.type = type;
        this.target = target;
        this.amount = amount;
        this.description = description;
        this.radius = radius;
    }

    public static QuestObjective fromConfig(ConfigurationSection section) {
        String typeStr = section.getString("type", "fetch");
        String desc = section.getString("description", null);
        return switch (typeStr.toLowerCase()) {
            case "kill" -> new QuestObjective(Type.KILL,
                    section.getString("entity", "ZOMBIE"), section.getInt("amount", 1), desc);
            case "talk" -> new QuestObjective(Type.TALK,
                    section.getString("npc", ""), 1, desc);
            case "craft" -> new QuestObjective(Type.CRAFT,
                    section.getString("material", "DIRT"), section.getInt("amount", 1), desc);
            case "mine" -> new QuestObjective(Type.MINE,
                    section.getString("material", "STONE"), section.getInt("amount", 1), desc);
            case "place" -> new QuestObjective(Type.PLACE,
                    section.getString("material", "STONE"), section.getInt("amount", 1), desc);
            case "fish" -> new QuestObjective(Type.FISH,
                    section.getString("material", "ANY"), section.getInt("amount", 1), desc);
            case "breed" -> new QuestObjective(Type.BREED,
                    section.getString("entity", "COW"), section.getInt("amount", 1), desc);
            case "smelt" -> new QuestObjective(Type.SMELT,
                    section.getString("material", "IRON_INGOT"), section.getInt("amount", 1), desc);
            case "explore" -> new QuestObjective(Type.EXPLORE,
                    section.getString("location", "world,0,64,0"), 1, desc,
                    section.getDouble("radius", 50.0));
            case "interact" -> new QuestObjective(Type.INTERACT,
                    section.getString("material", section.getString("entity", "CRAFTING_TABLE")),
                    section.getInt("amount", 1), desc);
            default -> new QuestObjective(Type.FETCH,
                    section.getString("material", "DIRT"), section.getInt("amount", 1), desc);
        };
    }

    public Type getType() { return type; }
    public String getTarget() { return target; }
    public int getAmount() { return amount; }
    public String getDescription() { return description; }
    public double getRadius() { return radius; }
}
```

**Step 4: Run tests to verify they pass**

Run: `./gradlew test`
Expected: ALL PASS

**Step 5: Commit**

```bash
git add src/main/java/net/axther/serverCore/quest/QuestObjective.java src/test/java/net/axther/serverCore/quest/QuestDataModelTest.java
git commit -m "Add 8 new quest objective types and per-objective description field"
```

---

### Task 4: Expand QuestReward with new types

**Files:**
- Modify: `src/main/java/net/axther/serverCore/quest/QuestReward.java`
- Modify: `src/test/java/net/axther/serverCore/quest/QuestDataModelTest.java`

**Step 1: Write failing tests for new reward types**

Add to `QuestDataModelTest.QuestRewardTests`:

```java
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
    assertEquals(3600, reward.getAmount()); // duration in seconds
}

@Test
void petRewardStoresCorrectly() {
    QuestReward reward = new QuestReward(QuestReward.Type.PET, "dragon", 1);
    assertEquals(QuestReward.Type.PET, reward.getType());
    assertEquals("dragon", reward.getValue());
}
```

**Step 2: Run tests to verify they fail**

Run: `./gradlew test`
Expected: FAIL — new enum values don't exist

**Step 3: Update QuestReward.java**

Replace the `Type` enum and `fromConfig` and `give` methods:

```java
package net.axther.serverCore.quest;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.logging.Logger;

public class QuestReward {

    public enum Type { ITEM, XP, COMMAND, MONEY, PERMISSION, PET }

    private final Type type;
    private final String value;
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
            case "money" -> new QuestReward(Type.MONEY, "", section.getInt("amount", 0));
            case "permission" -> new QuestReward(Type.PERMISSION,
                    section.getString("value", ""), section.getInt("duration", 0));
            case "pet" -> new QuestReward(Type.PET,
                    section.getString("value", ""), section.getInt("amount", 1));
            default -> new QuestReward(Type.ITEM,
                    section.getString("material", "DIRT"), section.getInt("amount", 1));
        };
    }

    public void give(Player player) {
        Logger logger = Bukkit.getLogger();
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
            case MONEY -> {
                try {
                    Class.forName("net.axther.serverCore.hook.VaultHook");
                    if (!net.axther.serverCore.hook.VaultHook.hasEconomy()) {
                        logger.warning("Quest reward requires Vault economy but Vault is not available");
                        return;
                    }
                    net.axther.serverCore.hook.VaultHook.deposit(player, amount);
                } catch (ClassNotFoundException e) {
                    logger.warning("Quest reward requires Vault but Vault is not installed");
                }
            }
            case PERMISSION -> {
                try {
                    Class.forName("net.axther.serverCore.hook.VaultHook");
                    if (!net.axther.serverCore.hook.VaultHook.hasPermissions()) {
                        logger.warning("Quest reward requires Vault permissions but Vault is not available");
                        return;
                    }
                    net.axther.serverCore.hook.VaultHook.addPermission(player, value);
                    if (amount > 0) {
                        // Schedule removal after duration (amount = seconds)
                        Bukkit.getScheduler().runTaskLater(
                                Bukkit.getPluginManager().getPlugins()[0], // plugin reference
                                () -> net.axther.serverCore.hook.VaultHook.removePermission(player, value),
                                amount * 20L);
                    }
                } catch (ClassNotFoundException e) {
                    logger.warning("Quest reward requires Vault but Vault is not installed");
                }
            }
            case PET -> {
                // Handled by QuestManager which has access to PetManager
            }
        }
    }

    public Type getType() { return type; }
    public String getValue() { return value; }
    public int getAmount() { return amount; }
}
```

**Step 4: Run tests to verify they pass**

Run: `./gradlew test`
Expected: ALL PASS

**Step 5: Commit**

```bash
git add src/main/java/net/axther/serverCore/quest/QuestReward.java src/test/java/net/axther/serverCore/quest/QuestDataModelTest.java
git commit -m "Add MONEY, PERMISSION, and PET quest reward types"
```

---

### Task 5: Add quest structure parameters to Quest.java

**Files:**
- Modify: `src/main/java/net/axther/serverCore/quest/Quest.java`
- Modify: `src/test/java/net/axther/serverCore/quest/QuestDataModelTest.java`

**Step 1: Write failing tests for new Quest fields**

Add to `QuestDataModelTest.QuestTests`:

```java
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
```

**Step 2: Run tests to verify they fail**

Run: `./gradlew test`
Expected: FAIL — new constructor and getters don't exist

**Step 3: Update Quest.java**

Add new fields after `cooldownSeconds`:
```java
private final String requiredPermission;
private final List<String> prerequisites;
private final int timeLimit; // seconds, 0 = no limit
private final String category;
private final boolean sequentialObjectives;
```

Add a new constructor that includes these fields. Keep the old constructor as a backwards-compatible delegate:

```java
public Quest(String id, String displayName, String description,
             String acceptNpc, String turnInNpc,
             List<QuestObjective> objectives, List<QuestReward> rewards,
             boolean repeatable, int cooldownSeconds) {
    this(id, displayName, description, acceptNpc, turnInNpc,
         objectives, rewards, repeatable, cooldownSeconds,
         null, List.of(), 0, "general", false);
}

public Quest(String id, String displayName, String description,
             String acceptNpc, String turnInNpc,
             List<QuestObjective> objectives, List<QuestReward> rewards,
             boolean repeatable, int cooldownSeconds,
             String requiredPermission, List<String> prerequisites,
             int timeLimit, String category, boolean sequentialObjectives) {
    this.id = id;
    this.displayName = displayName;
    this.description = description;
    this.acceptNpc = acceptNpc;
    this.turnInNpc = turnInNpc;
    this.objectives = objectives;
    this.rewards = rewards;
    this.repeatable = repeatable;
    this.cooldownSeconds = cooldownSeconds;
    this.requiredPermission = requiredPermission;
    this.prerequisites = prerequisites != null ? prerequisites : List.of();
    this.timeLimit = timeLimit;
    this.category = category != null ? category : "general";
    this.sequentialObjectives = sequentialObjectives;
}
```

Update `fromConfig` to parse new fields:
```java
String requiredPermission = section.getString("required-permission", null);
List<String> prerequisites = section.getStringList("prerequisites");
int timeLimit = section.getInt("time-limit", 0);
String category = section.getString("category", "general");
boolean sequentialObjectives = section.getBoolean("sequential-objectives", false);
```

And pass them to the new constructor.

Add getters:
```java
public String getRequiredPermission() { return requiredPermission; }
public List<String> getPrerequisites() { return prerequisites; }
public int getTimeLimit() { return timeLimit; }
public String getCategory() { return category; }
public boolean isSequentialObjectives() { return sequentialObjectives; }
```

**Step 4: Run tests to verify they pass**

Run: `./gradlew test`
Expected: ALL PASS

**Step 5: Commit**

```bash
git add src/main/java/net/axther/serverCore/quest/Quest.java src/test/java/net/axther/serverCore/quest/QuestDataModelTest.java
git commit -m "Add quest structure parameters: prerequisites, permissions, time limits, categories, sequential objectives"
```

---

### Task 6: Add startedAt timestamp to QuestProgress

**Files:**
- Modify: `src/main/java/net/axther/serverCore/quest/QuestProgress.java`
- Modify: `src/test/java/net/axther/serverCore/quest/QuestDataModelTest.java`

**Step 1: Write failing tests**

Add to `QuestDataModelTest.QuestProgressTests`:

```java
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
```

**Step 2: Run tests to verify they fail**

Run: `./gradlew test`
Expected: FAIL

**Step 3: Update QuestProgress.java**

Add `startedAt` field and update constructors:

```java
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

    public boolean isExpired(int timeLimitSeconds) {
        if (timeLimitSeconds <= 0) return false;
        long elapsed = (System.currentTimeMillis() - startedAt) / 1000;
        return elapsed >= timeLimitSeconds;
    }

    public String getQuestId() { return questId; }
    public int[] getObjectiveProgress() { return objectiveProgress; }
    public long getStartedAt() { return startedAt; }

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
```

**Step 4: Run tests to verify they pass**

Run: `./gradlew test`
Expected: ALL PASS

**Step 5: Commit**

```bash
git add src/main/java/net/axther/serverCore/quest/QuestProgress.java src/test/java/net/axther/serverCore/quest/QuestDataModelTest.java
git commit -m "Add startedAt timestamp and time limit expiry to QuestProgress"
```

---

### Task 7: Update QuestManager with new logic

**Files:**
- Modify: `src/main/java/net/axther/serverCore/quest/QuestManager.java`

**Step 1: Update canAccept() to check prerequisites, permissions, max-active, and time limits**

In `canAccept()`, add after the cooldown check:

```java
// Check required permission
if (quest.getRequiredPermission() != null && !player.hasPermission(quest.getRequiredPermission())) {
    return false;
}

// Check prerequisites
for (String prereq : quest.getPrerequisites()) {
    if (!hasCompleted(player.getUniqueId(), prereq)) return false;
}

// Check max active quests
if (maxActiveQuests > 0) {
    List<QuestProgress> active = activeQuests.get(player.getUniqueId());
    if (active != null && active.size() >= maxActiveQuests) return false;
}
```

**Step 2: Add maxActiveQuests field and setter**

```java
private int maxActiveQuests = 0;

public void setMaxActiveQuests(int max) { this.maxActiveQuests = max; }
```

**Step 3: Add new handler methods for each objective type**

Add these methods mirroring `handleKill` pattern:

```java
public void handleCraft(UUID playerId, String materialName, int amount) {
    incrementObjective(playerId, QuestObjective.Type.CRAFT, materialName, amount);
}

public void handleMine(UUID playerId, String materialName) {
    incrementObjective(playerId, QuestObjective.Type.MINE, materialName, 1);
}

public void handlePlace(UUID playerId, String materialName) {
    incrementObjective(playerId, QuestObjective.Type.PLACE, materialName, 1);
}

public void handleFish(UUID playerId, String materialName) {
    incrementObjective(playerId, QuestObjective.Type.FISH, materialName, 1);
}

public void handleBreed(UUID playerId, String entityTypeName) {
    incrementObjective(playerId, QuestObjective.Type.BREED, entityTypeName, 1);
}

public void handleSmelt(UUID playerId, String materialName, int amount) {
    incrementObjective(playerId, QuestObjective.Type.SMELT, materialName, amount);
}

public void handleInteract(UUID playerId, String targetName) {
    incrementObjective(playerId, QuestObjective.Type.INTERACT, targetName, 1);
}

public void handleExplore(UUID playerId, org.bukkit.Location location) {
    List<QuestProgress> list = activeQuests.get(playerId);
    if (list == null) return;

    for (QuestProgress progress : list) {
        Quest quest = quests.get(progress.getQuestId());
        if (quest == null) continue;

        List<QuestObjective> objectives = quest.getObjectives();
        for (int i = 0; i < objectives.size(); i++) {
            QuestObjective obj = objectives.get(i);
            if (obj.getType() != QuestObjective.Type.EXPLORE) continue;
            if (quest.isSequentialObjectives() && i > getFirstIncompleteIndex(progress, objectives)) continue;
            if (progress.getProgress(i) >= 1) continue;

            String[] parts = obj.getTarget().split(",");
            if (parts.length != 4) continue;
            String world = parts[0];
            if (!location.getWorld().getName().equalsIgnoreCase(world)) continue;

            double tx = Double.parseDouble(parts[1]);
            double ty = Double.parseDouble(parts[2]);
            double tz = Double.parseDouble(parts[3]);
            double distSq = Math.pow(location.getX() - tx, 2) + Math.pow(location.getY() - ty, 2) + Math.pow(location.getZ() - tz, 2);
            if (distSq <= obj.getRadius() * obj.getRadius()) {
                progress.setProgress(i, 1);
            }
        }
    }
}
```

**Step 4: Add shared incrementObjective helper and sequential objective support**

```java
private void incrementObjective(UUID playerId, QuestObjective.Type type, String target, int amount) {
    List<QuestProgress> list = activeQuests.get(playerId);
    if (list == null) return;

    for (QuestProgress progress : list) {
        Quest quest = quests.get(progress.getQuestId());
        if (quest == null) continue;

        // Check time limit expiry
        if (quest.getTimeLimit() > 0 && progress.isExpired(quest.getTimeLimit())) continue;

        List<QuestObjective> objectives = quest.getObjectives();
        for (int i = 0; i < objectives.size(); i++) {
            QuestObjective obj = objectives.get(i);
            if (obj.getType() != type) continue;

            // Sequential: skip if not the current objective
            if (quest.isSequentialObjectives() && i > getFirstIncompleteIndex(progress, objectives)) continue;

            boolean matches = type == QuestObjective.Type.FISH
                    ? (obj.getTarget().equalsIgnoreCase("ANY") || obj.getTarget().equalsIgnoreCase(target))
                    : obj.getTarget().equalsIgnoreCase(target);

            if (matches && progress.getProgress(i) < obj.getAmount()) {
                for (int a = 0; a < amount && progress.getProgress(i) < obj.getAmount(); a++) {
                    progress.increment(i);
                }
            }
        }
    }
}

private int getFirstIncompleteIndex(QuestProgress progress, List<QuestObjective> objectives) {
    for (int i = 0; i < objectives.size(); i++) {
        if (progress.getProgress(i) < objectives.get(i).getAmount()) return i;
    }
    return objectives.size();
}
```

**Step 5: Refactor existing handleKill and handleTalk to use incrementObjective**

Replace `handleKill`:
```java
public void handleKill(UUID playerId, String entityTypeName) {
    incrementObjective(playerId, QuestObjective.Type.KILL, entityTypeName, 1);
}
```

Replace `handleTalk` — keep special behavior (sets to 1 instead of incrementing):
```java
public void handleTalk(UUID playerId, String npcId) {
    List<QuestProgress> list = activeQuests.get(playerId);
    if (list == null) return;
    for (QuestProgress progress : list) {
        Quest quest = quests.get(progress.getQuestId());
        if (quest == null) continue;
        List<QuestObjective> objectives = quest.getObjectives();
        for (int i = 0; i < objectives.size(); i++) {
            QuestObjective obj = objectives.get(i);
            if (obj.getType() == QuestObjective.Type.TALK
                    && obj.getTarget().equalsIgnoreCase(npcId)
                    && progress.getProgress(i) < 1) {
                if (quest.isSequentialObjectives() && i > getFirstIncompleteIndex(progress, objectives)) continue;
                progress.setProgress(i, 1);
            }
        }
    }
}
```

**Step 6: Add PetManager reference for PET rewards**

```java
private net.axther.serverCore.pet.PetManager petManager;

public void setPetManager(net.axther.serverCore.pet.PetManager petManager) {
    this.petManager = petManager;
}
```

Update `completeQuest` to handle PET rewards:
```java
for (QuestReward reward : quest.getRewards()) {
    if (reward.getType() == QuestReward.Type.PET && petManager != null) {
        var profile = petManager.getProfile(reward.getValue());
        if (profile != null) {
            for (int i = 0; i < reward.getAmount(); i++) {
                player.getInventory().addItem(profile.createItem());
            }
        }
    } else {
        reward.give(player);
    }
}
```

**Step 7: Add time-limit expiry check method**

```java
public void checkExpiredQuests(Player player) {
    List<QuestProgress> list = activeQuests.get(player.getUniqueId());
    if (list == null) return;

    list.removeIf(progress -> {
        Quest quest = quests.get(progress.getQuestId());
        if (quest == null) return false;
        if (quest.getTimeLimit() > 0 && progress.isExpired(quest.getTimeLimit())) {
            player.sendMessage(net.kyori.adventure.text.Component.text(
                    "Quest '" + quest.getDisplayName() + "' has expired!",
                    net.kyori.adventure.text.format.NamedTextColor.RED));
            return true;
        }
        return false;
    });
    if (list != null && list.isEmpty()) activeQuests.remove(player.getUniqueId());
}
```

**Step 8: Verify build compiles**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 9: Commit**

```bash
git add src/main/java/net/axther/serverCore/quest/QuestManager.java
git commit -m "Add quest manager support for new objectives, rewards, prerequisites, time limits, and sequential objectives"
```

---

### Task 8: Expand QuestListener with new event handlers

**Files:**
- Modify: `src/main/java/net/axther/serverCore/quest/listener/QuestListener.java`

**Step 1: Add all new event handlers and action bar notifications**

Replace entire file:

```java
package net.axther.serverCore.quest.listener;

import net.axther.serverCore.quest.Quest;
import net.axther.serverCore.quest.QuestManager;
import net.axther.serverCore.quest.QuestObjective;
import net.axther.serverCore.quest.QuestProgress;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

public class QuestListener implements Listener {

    private final QuestManager manager;
    private boolean actionBarEnabled = true;

    public QuestListener(QuestManager manager) {
        this.manager = manager;
    }

    public void setActionBarEnabled(boolean enabled) {
        this.actionBarEnabled = enabled;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        String entityTypeName = event.getEntityType().name();
        manager.handleKill(killer.getUniqueId(), entityTypeName);
        if (actionBarEnabled) sendProgressBar(killer);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack result = event.getRecipe().getResult();
        int amount = result.getAmount();
        if (event.isShiftClick()) {
            // Estimate max craft amount from ingredients
            amount = estimateShiftCraftAmount(event);
        }
        manager.handleCraft(player.getUniqueId(), result.getType().name(), amount);
        if (actionBarEnabled) sendProgressBar(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        manager.handleMine(event.getPlayer().getUniqueId(), event.getBlock().getType().name());
        if (actionBarEnabled) sendProgressBar(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        manager.handlePlace(event.getPlayer().getUniqueId(), event.getBlock().getType().name());
        if (actionBarEnabled) sendProgressBar(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        String material = "ANY";
        if (event.getCaught() instanceof Item item) {
            material = item.getItemStack().getType().name();
        }
        manager.handleFish(event.getPlayer().getUniqueId(), material);
        if (actionBarEnabled) sendProgressBar(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
        if (!(event.getBreeder() instanceof Player player)) return;
        manager.handleBreed(player.getUniqueId(), event.getEntityType().name());
        if (actionBarEnabled) sendProgressBar(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSmelt(FurnaceExtractEvent event) {
        manager.handleSmelt(event.getPlayer().getUniqueId(),
                event.getItemType().name(), event.getItemAmount());
        if (actionBarEnabled) sendProgressBar(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        manager.handleInteract(event.getPlayer().getUniqueId(),
                event.getClickedBlock().getType().name());
        if (actionBarEnabled) sendProgressBar(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        manager.handleInteract(event.getPlayer().getUniqueId(),
                event.getRightClicked().getType().name());
        if (actionBarEnabled) sendProgressBar(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        // Only check on block change to avoid excessive processing
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        Player player = event.getPlayer();
        manager.handleExplore(player.getUniqueId(), player.getLocation());
        manager.checkExpiredQuests(player);
    }

    private void sendProgressBar(Player player) {
        var active = manager.getActiveQuests(player.getUniqueId());
        if (active.isEmpty()) return;

        // Show progress for the first quest with an incomplete objective
        for (QuestProgress progress : active) {
            Quest quest = manager.getQuest(progress.getQuestId());
            if (quest == null) continue;

            var objectives = quest.getObjectives();
            for (int i = 0; i < objectives.size(); i++) {
                QuestObjective obj = objectives.get(i);
                int current = progress.getProgress(i);
                if (obj.getType() == QuestObjective.Type.FETCH) {
                    current = manager.countMaterial(player, obj.getTarget());
                }
                if (current < obj.getAmount()) {
                    String desc = obj.getDescription() != null ? obj.getDescription()
                            : generateDescription(obj);
                    player.sendActionBar(Component.text(
                            quest.getDisplayName() + ": " + desc
                                    + " (" + Math.min(current, obj.getAmount()) + "/" + obj.getAmount() + ")",
                            NamedTextColor.AQUA));
                    return;
                }
            }
        }
    }

    private String generateDescription(QuestObjective obj) {
        return switch (obj.getType()) {
            case FETCH -> "Collect " + obj.getTarget();
            case KILL -> "Kill " + obj.getTarget();
            case TALK -> "Talk to " + obj.getTarget();
            case CRAFT -> "Craft " + obj.getTarget();
            case MINE -> "Mine " + obj.getTarget();
            case PLACE -> "Place " + obj.getTarget();
            case FISH -> obj.getTarget().equals("ANY") ? "Catch fish" : "Catch " + obj.getTarget();
            case BREED -> "Breed " + obj.getTarget();
            case SMELT -> "Smelt " + obj.getTarget();
            case EXPLORE -> "Explore location";
            case INTERACT -> "Interact with " + obj.getTarget();
        };
    }

    private int estimateShiftCraftAmount(CraftItemEvent event) {
        int resultAmount = event.getRecipe().getResult().getAmount();
        int min = 64;
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (item != null && !item.getType().isAir()) {
                min = Math.min(min, item.getAmount());
            }
        }
        return min * resultAmount;
    }
}
```

**Step 2: Verify build compiles**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add src/main/java/net/axther/serverCore/quest/listener/QuestListener.java
git commit -m "Add event listeners for all new quest objective types and action bar progress"
```

---

### Task 9: Update QuestCommand for category grouping and custom descriptions

**Files:**
- Modify: `src/main/java/net/axther/serverCore/quest/command/QuestCommand.java`

**Step 1: Update handleActive to group by category and use custom descriptions**

Replace `handleActive`:

```java
private void handleActive(Player player) {
    List<QuestProgress> active = manager.getActiveQuests(player.getUniqueId());
    if (active.isEmpty()) {
        player.sendMessage(Component.text("No active quests.", NamedTextColor.GRAY));
        return;
    }

    MiniMessage mm = MiniMessage.miniMessage();
    player.sendMessage(Component.text("--- Active Quests (" + active.size() + ") ---", NamedTextColor.GREEN));

    // Group by category
    Map<String, List<QuestProgress>> byCategory = new LinkedHashMap<>();
    for (QuestProgress progress : active) {
        Quest quest = manager.getQuest(progress.getQuestId());
        if (quest == null) continue;
        byCategory.computeIfAbsent(quest.getCategory(), k -> new ArrayList<>()).add(progress);
    }

    for (Map.Entry<String, List<QuestProgress>> entry : byCategory.entrySet()) {
        player.sendMessage(Component.text(" [" + entry.getKey() + "]", NamedTextColor.GOLD));

        for (QuestProgress progress : entry.getValue()) {
            Quest quest = manager.getQuest(progress.getQuestId());
            if (quest == null) continue;

            // Show time remaining if time-limited
            String timeInfo = "";
            if (quest.getTimeLimit() > 0) {
                long elapsed = (System.currentTimeMillis() - progress.getStartedAt()) / 1000;
                long remaining = quest.getTimeLimit() - elapsed;
                if (remaining > 0) {
                    timeInfo = " <gray>(" + formatTime(remaining) + " remaining)";
                } else {
                    timeInfo = " <red>(EXPIRED)";
                }
            }

            player.sendMessage(Component.text("  ").append(mm.deserialize(quest.getDisplayName() + timeInfo)));

            List<QuestObjective> objectives = quest.getObjectives();
            for (int i = 0; i < objectives.size(); i++) {
                QuestObjective obj = objectives.get(i);
                int current = progress.getProgress(i);

                if (obj.getType() == QuestObjective.Type.FETCH) {
                    current = manager.countMaterial(player, obj.getTarget());
                }

                String desc = obj.getDescription() != null ? obj.getDescription()
                        : generateDescription(obj);

                boolean done = current >= obj.getAmount();
                // For sequential quests, show locked objectives
                boolean locked = quest.isSequentialObjectives() && i > getFirstIncomplete(progress, objectives);
                String prefix = done ? "+" : locked ? "x" : "-";
                NamedTextColor color = done ? NamedTextColor.GREEN : locked ? NamedTextColor.DARK_GRAY : NamedTextColor.GRAY;

                player.sendMessage(Component.text("   " + prefix + " " + desc
                                + " (" + Math.min(current, obj.getAmount()) + "/" + obj.getAmount() + ")",
                        color));
            }
        }
    }
}
```

Add helper methods:

```java
private String generateDescription(QuestObjective obj) {
    return switch (obj.getType()) {
        case FETCH -> "Collect " + obj.getAmount() + " " + obj.getTarget();
        case KILL -> "Kill " + obj.getAmount() + " " + obj.getTarget();
        case TALK -> "Talk to " + obj.getTarget();
        case CRAFT -> "Craft " + obj.getAmount() + " " + obj.getTarget();
        case MINE -> "Mine " + obj.getAmount() + " " + obj.getTarget();
        case PLACE -> "Place " + obj.getAmount() + " " + obj.getTarget();
        case FISH -> obj.getTarget().equals("ANY") ? "Catch fish" : "Catch " + obj.getTarget();
        case BREED -> "Breed " + obj.getAmount() + " " + obj.getTarget();
        case SMELT -> "Smelt " + obj.getAmount() + " " + obj.getTarget();
        case EXPLORE -> "Explore location";
        case INTERACT -> "Interact with " + obj.getTarget();
    };
}

private int getFirstIncomplete(QuestProgress progress, List<QuestObjective> objectives) {
    for (int i = 0; i < objectives.size(); i++) {
        if (progress.getProgress(i) < objectives.get(i).getAmount()) return i;
    }
    return objectives.size();
}

private String formatTime(long seconds) {
    if (seconds >= 3600) return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
    if (seconds >= 60) return (seconds / 60) + "m " + (seconds % 60) + "s";
    return seconds + "s";
}
```

Add required imports:
```java
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
```

**Step 2: Verify build compiles**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add src/main/java/net/axther/serverCore/quest/command/QuestCommand.java
git commit -m "Update quest command to show category grouping, custom descriptions, and time remaining"
```

---

### Task 10: Update config.yml and ServerCore.java for new settings

**Files:**
- Modify: `src/main/resources/config.yml`
- Modify: `src/main/java/net/axther/serverCore/ServerCore.java`

**Step 1: Add quest settings to config.yml**

Under `systems.quests`:
```yaml
  quests:
    enabled: true
    max-active-quests: 0
    action-bar-progress: true
```

**Step 2: Update ServerCore.java quest init block**

After creating `QuestListener`, pass settings:

```java
// Read quest config values
int maxActive = serverCoreConfig.getConfig().getInt("systems.quests.max-active-quests", 0);
boolean actionBar = serverCoreConfig.getConfig().getBoolean("systems.quests.action-bar-progress", true);
questManager.setMaxActiveQuests(maxActive);

QuestListener questListener = new QuestListener(questManager);
questListener.setActionBarEnabled(actionBar);
getServer().getPluginManager().registerEvents(questListener, this);
```

After pet system init, wire pet manager into quest manager:
```java
if (petManager != null && questManager != null) {
    questManager.setPetManager(petManager);
}
```

In the quest system block, set up Vault:
```java
// Set up Vault (soft dependency)
boolean vaultPresent = getServer().getPluginManager().getPlugin("Vault") != null;
if (vaultPresent) {
    setupVault();
}
```

Add isolated method:
```java
private void setupVault() {
    boolean econ = net.axther.serverCore.hook.VaultHook.setupEconomy();
    boolean perm = net.axther.serverCore.hook.VaultHook.setupPermissions();
    if (econ) getLogger().info("Vault economy hooked for quest rewards");
    if (perm) getLogger().info("Vault permissions hooked for quest rewards");
}
```

**Step 3: Verify build compiles**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add src/main/resources/config.yml src/main/java/net/axther/serverCore/ServerCore.java
git commit -m "Wire quest expansion settings: max-active, action bar toggle, Vault hook, pet rewards"
```

---

### Task 11: Create example quest YAML showcasing new features

**Files:**
- Create: `src/main/resources/quests/example_advanced.yml`

**Step 1: Write example quest file**

```yaml
# Example quest showcasing all new parameters
# Place in plugins/ServerCore/quests/ folder

id: dragon_hunter
display-name: "<gradient:#FF4500:#FFD700><bold>Dragon Hunter</bold></gradient>"
description: "Prove your worth by crafting gear, slaying foes, and exploring the end."
category: "combat"
required-permission: "quests.advanced"
prerequisites:
  - beginner_quest
sequential-objectives: true
time-limit: 7200  # 2 hours
repeatable: true
cooldown: 86400  # 24 hours
accept-npc: blacksmith
turn-in-npc: blacksmith

objectives:
  - type: craft
    material: DIAMOND_SWORD
    amount: 1
    description: "Forge a diamond blade"
  - type: mine
    material: OBSIDIAN
    amount: 10
    description: "Harvest obsidian for the portal"
  - type: kill
    entity: ENDERMAN
    amount: 15
  - type: explore
    location: "world_the_end,0,64,0"
    radius: 100
    description: "Enter the End dimension"
  - type: kill
    entity: ENDER_DRAGON
    amount: 1
    description: "Slay the Ender Dragon"

rewards:
  - type: item
    material: ELYTRA
    amount: 1
  - type: xp
    amount: 1000
  - type: money
    amount: 5000
  - type: pet
    value: dragon
  - type: permission
    value: "cosmetics.dragon_wings"
    duration: 0  # permanent
```

**Step 2: Commit**

```bash
git add src/main/resources/quests/example_advanced.yml
git commit -m "Add advanced example quest YAML showcasing all new parameters"
```

---

### Task 12: Final build and integration test

**Step 1: Run full build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL, all tests pass

**Step 2: Verify no compilation warnings**

Run: `./gradlew build 2>&1 | grep -i "warning\|error"`
Expected: No errors, CRLF warnings only

**Step 3: Final commit and summary**

```bash
git log --oneline -12
```

Verify all commits are present.
