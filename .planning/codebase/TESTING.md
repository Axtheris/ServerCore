# Testing Patterns

**Analysis Date:** 2026-03-21

## Test Framework

**Runner:**
- JUnit Jupiter 5.11.4 (`org.junit.jupiter:junit-jupiter`)
- Configuration: `useJUnitPlatform()` in `build.gradle`
- Launcher: `org.junit.platform:junit-platform-launcher`

**Assertion Library:**
- JUnit Jupiter assertions via `org.junit.jupiter.api.Assertions.*`
- Static imports: `assertEquals`, `assertTrue`, `assertFalse`, `assertNull`, `assertNotNull`, `assertThrows`, `assertArrayEquals`

**Mocking:**
- Mockito 5.14.2 (`org.mockito:mockito-core`)
- Static import: `org.mockito.Mockito.mock`

**Run Commands:**
```bash
./gradlew test              # Run all tests
./gradlew test --watch      # Watch mode (requires Gradle daemon)
./gradlew test --info       # Verbose output with testLogging configuration
```

**Test Logging Configuration (from build.gradle):**
```gradle
test {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
        events "passed", "failed"
    }
}
```

## Test File Organization

**Location:**
- Mirrored structure: tests are colocated with source code by package
- Source: `src/main/java/net/axther/serverCore/...`
- Tests: `src/test/java/net/axther/serverCore/...`

**Naming:**
- Class under test: `ClassName.java`
- Test class: `ClassNameTest.java`
- Pattern: append `Test` suffix (e.g., `MenuActionTest`, `ScriptParserTest`, `HologramDataModelTest`, `QuestDataModelTest`)

**Structure:**
```
src/test/java/
├── net/axther/serverCore/
│   ├── gui/
│   │   ├── MenuActionTest.java
│   │   └── PaginationTest.java
│   ├── hologram/
│   │   └── HologramDataModelTest.java
│   ├── particle/
│   │   └── script/
│   │       ├── ScriptEngineTest.java
│   │       ├── ScriptParserTest.java
│   │       └── ScriptPerformanceTest.java
│   ├── quest/
│   │   └── QuestDataModelTest.java
│   └── particle/
│       └── EmitterPatternPerformanceTest.java
```

**Coverage:**
- Currently 8 test files, ~156 source files
- Focus on data models, parsing, pagination logic, and event structures
- No test coverage for: Bukkit event listeners, command execution, entity lifecycle, or persistence

## Test Structure

**Suite Organization:**

```java
class MenuActionTest {

    @Nested
    class ParseTests {
        @Test
        void parseCommandAction() {
            // Arrange: Create test input
            MenuAction action = MenuAction.parse("command", "say Hello %player%");

            // Assert: Verify output
            assertEquals("command", action.getType());
            assertEquals("say Hello %player%", action.getValue());
        }
    }

    @Nested
    class MenuItemDynamicTests {
        @Test
        void defaultMenuItemIsNotDynamic() {
            MenuItem item = MenuItem.builder(mock(ItemStack.class)).build();
            assertFalse(item.isDynamic());
        }
    }
}
```

**Patterns:**

1. **Nested Test Classes** (`@Nested`):
   - Logical grouping of related tests
   - Each `@Nested` class groups tests for a specific concern
   - Example categories: `ParseTests`, `MenuItemDynamicTests`, `EventTests`, `BuilderTests`

2. **Single Assertion Focus**:
   - One test per behavior
   - Test name describes the exact behavior being tested
   - Example: `parseCommandAction()` tests only the command action parsing

3. **Parametrized Tests** (`@ParameterizedTest`):
   - Used for boundary testing and mathematical verification
   - `@CsvSource` provides multiple input/output pairs
   - Example from `PaginationTest`:
   ```java
   @ParameterizedTest
   @CsvSource({
       "0,  45, 1",   // empty list = 1 page
       "1,  45, 1",   // 1 item = 1 page
       "45, 45, 1",   // fits exactly
       "46, 45, 2",   // overflow by 1
   })
   void totalPagesCalculation(int items, int perPage, int expected) {
       assertEquals(expected, totalPages(items, perPage),
               items + " items at " + perPage + " per page");
   }
   ```

4. **Test Naming Convention**:
   - `void <behavior><Condition><Expected>()`
   - Example: `parseCommandAction()`, `defaultMenuItemIsNotDynamic()`, `getProgressReturnsZeroForOutOfBoundsIndex()`
   - Condition is often implicit: `multipleConditionsAllStored()` implies "when adding multiple conditions"

5. **Setup Pattern**:
   - Minimal setup — most tests create objects inline
   - No `@BeforeEach` or setup methods in current tests
   - Private helper methods used sparingly in test classes

6. **Teardown Pattern**:
   - No explicit teardown needed (unit tests, not integration)
   - No `@AfterEach` in current tests

## Mocking

**Framework:** Mockito 5.14.2

**Patterns:**

```java
// Simple mocking of Bukkit objects
Player player = mock(Player.class);
ItemStack item = mock(ItemStack.class);
HologramManager manager = mock(HologramManager.class);

// Using mocks in assertions
HologramClickEvent event = new HologramClickEvent(player, "test-holo", List.of());
assertEquals(player, event.getPlayer());
```

**What to Mock:**
- Bukkit API objects that cannot be instantiated without a server: `Player`, `ItemStack`, `HologramManager`
- External dependencies: `HologramManager`, `MenuManager`
- Event-related objects when testing event state

**What NOT to Mock:**
- Data model classes: `MenuAction`, `MenuItem`, `HologramClickEvent`, `Quest`, `QuestProgress`
- Parser/builder classes: `ScriptParser`, `HologramBuilder`, `MenuAction.parse()`
- Enum-like classes: `QuestObjective.Type`, `QuestReward.Type`
- Configuration/value objects: `QuestObjective`, `QuestReward`, `Quest`

## Fixtures and Factories

**Test Data:**

Most tests create test data inline. No fixture files or factory classes detected.

**Example from `QuestDataModelTest.java`:**
```java
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
    // ... assertions
}
```

**Location:**
- No dedicated fixture/factory directory
- Test data created in test methods directly
- `java.util.List.of()` for immutable test collections

## Coverage

**Requirements:** No coverage target enforced in `build.gradle`

**View Coverage:**
```bash
# JaCoCo plugin not configured — must add to build.gradle:
# plugins { id 'jacoco' }
# jacocoTestReport { dependsOn test }

# Then view report at: build/reports/jacoco/test/html/index.html
```

**Current Gaps:**
- No tests for command handlers (`CosmeticCommand`, `PetCommand`, etc.)
- No tests for event listeners (`CosmeticLifecycleListener`, `PetLifecycleListener`)
- No tests for Manager tick() lifecycle methods beyond assertion on constructor
- No integration tests with Bukkit server
- No tests for persistence layer (`CosmeticStore`, `QuestStore`, `PetStore`)
- No tests for YAML parsing and configuration loading

## Test Types

**Unit Tests:**
- Scope: Pure data model classes, parsing logic, mathematical calculations
- Approach: No Bukkit dependency, no server required
- Examples:
  - `ScriptParserTest` — expression parsing and evaluation
  - `HologramDataModelTest` — hologram state and configuration
  - `QuestDataModelTest` — quest structure and objective types
  - `PaginationTest` — pure math for menu pagination
  - `MenuActionTest` — action parsing and event construction

**Integration Tests:**
- Not implemented
- Would test: Manager lifecycle, event firing, Bukkit integration

**E2E Tests:**
- Not applicable (server plugin; E2E would require running Paper server)

## Common Patterns

**Async Testing:**
Not applicable — codebase is synchronous with no async/CompletableFuture patterns.

**Error Testing:**

```java
@Test
void hologramBuilderRequiresLocation() {
    HologramManager manager = mock(HologramManager.class);
    HologramBuilder builder = new HologramBuilder("test", manager);
    builder.lines("<gold>Hello");
    assertThrows(IllegalStateException.class, builder::spawn);
}
```

**Boundary Testing:**

```java
@Test
void getProgressReturnsZeroForOutOfBoundsIndex() {
    QuestProgress progress = new QuestProgress("q", 2);
    assertEquals(0, progress.getProgress(-1));
    assertEquals(0, progress.getProgress(5));
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
```

**Collection Testing:**

```java
@Test
void defaultMenuItemHasNoActions() {
    MenuItem item = MenuItem.builder(mock(ItemStack.class)).build();
    assertTrue(item.getActions().isEmpty());
    assertTrue(item.getRightActions().isEmpty());
}

@Test
void cycleItemsCanBeSet() {
    MenuItem item = MenuItem.builder(mock(ItemStack.class))
            .cycleItems(java.util.List.of(
                    mock(ItemStack.class),
                    mock(ItemStack.class)))
            .cycleInterval(10)
            .build();
    assertEquals(2, item.getCycleItems().size());
    assertEquals(10, item.getCycleInterval());
}
```

**State Machine Testing:**

```java
@Test
void questAbandonEventIsCancellable() {
    Player player = mock(Player.class);
    QuestAbandonEvent event = new QuestAbandonEvent(player, "dragon_hunter");
    assertFalse(event.isCancelled());
    event.setCancelled(true);
    assertTrue(event.isCancelled());
}
```

**Enum/Type Testing:**

```java
@Test
void fetchObjectiveStoresCorrectly() {
    QuestObjective obj = new QuestObjective(QuestObjective.Type.FETCH, "OAK_LOG", 16);
    assertEquals(QuestObjective.Type.FETCH, obj.getType());
    assertEquals("OAK_LOG", obj.getTarget());
    assertEquals(16, obj.getAmount());
}
```

## Notes

- No test files for major systems (Commands, Listeners, Managers lifecycle)
- Test approach is "test the model, not the infrastructure"
- Heavy reliance on simple unit tests; integration testing would benefit from TestServer or Paper test framework
- Performance tests exist for computation-heavy code (`EmitterPatternPerformanceTest`, `ScriptPerformanceTest`) but are minimal
- Mocking is light and strategic — mostly Bukkit API objects that can't be instantiated

---

*Testing analysis: 2026-03-21*
