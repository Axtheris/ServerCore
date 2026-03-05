package net.axther.serverCore.gui;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class MenuActionTest {

    @Nested
    class ParseTests {
        @Test
        void parseCommandAction() {
            MenuAction action = MenuAction.parse("command", "say Hello %player%");
            assertEquals("command", action.getType());
            assertEquals("say Hello %player%", action.getValue());
        }

        @Test
        void parsePlayerCommandAction() {
            MenuAction action = MenuAction.parse("player_command", "spawn");
            assertEquals("player_command", action.getType());
        }

        @Test
        void parseMessageAction() {
            MenuAction action = MenuAction.parse("message", "<gold>Welcome!");
            assertEquals("message", action.getType());
        }

        @Test
        void parseSoundAction() {
            MenuAction action = MenuAction.parse("sound", "UI_BUTTON_CLICK");
            assertEquals("sound", action.getType());
        }

        @Test
        void parseCloseAction() {
            MenuAction action = MenuAction.parse("close", "");
            assertEquals("close", action.getType());
        }

        @Test
        void parseOpenMenuAction() {
            MenuAction action = MenuAction.parse("open_menu", "shop");
            assertEquals("open_menu", action.getType());
            assertEquals("shop", action.getValue());
        }

        @Test
        void parseBackAction() {
            MenuAction action = MenuAction.parse("back", "");
            assertEquals("back", action.getType());
        }
    }

    @Nested
    class MenuItemDynamicTests {
        @Test
        void defaultMenuItemIsNotDynamic() {
            MenuItem item = MenuItem.builder(mock(ItemStack.class)).build();
            assertFalse(item.isDynamic());
        }

        @Test
        void dynamicFlagCanBeSet() {
            MenuItem item = MenuItem.builder(mock(ItemStack.class))
                    .dynamic(true).build();
            assertTrue(item.isDynamic());
        }

        @Test
        void defaultMenuItemHasNoActions() {
            MenuItem item = MenuItem.builder(mock(ItemStack.class)).build();
            assertTrue(item.getActions().isEmpty());
            assertTrue(item.getRightActions().isEmpty());
        }

        @Test
        void actionsCanBeSet() {
            MenuItem item = MenuItem.builder(mock(ItemStack.class))
                    .actions(java.util.List.of(MenuAction.parse("close", "")))
                    .build();
            assertEquals(1, item.getActions().size());
        }

        @Test
        void cycleItemsDefaultEmpty() {
            MenuItem item = MenuItem.builder(mock(ItemStack.class)).build();
            assertTrue(item.getCycleItems().isEmpty());
            assertEquals(20, item.getCycleInterval());
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

        @Test
        void nameLoreTemplatesStored() {
            MenuItem item = MenuItem.builder(mock(ItemStack.class))
                    .nameTemplate("<gold>%player_name%")
                    .loreTemplates(java.util.List.of("<gray>Balance: %vault_eco_balance%"))
                    .dynamic(true)
                    .build();
            assertEquals("<gold>%player_name%", item.getNameTemplate());
            assertEquals(1, item.getLoreTemplates().size());
        }
    }
}
