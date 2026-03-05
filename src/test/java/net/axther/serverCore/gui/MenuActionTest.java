package net.axther.serverCore.gui;

import net.axther.serverCore.api.builder.HologramBuilder;
import net.axther.serverCore.api.event.*;
import net.axther.serverCore.hologram.HologramManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.List;

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

    @Nested
    class EventTests {
        @Test
        void hologramClickEventFieldsStored() {
            Player player = mock(Player.class);
            HologramClickEvent event = new HologramClickEvent(player, "test-holo", List.of());
            assertEquals(player, event.getPlayer());
            assertEquals("test-holo", event.getHologramId());
            assertTrue(event.getActions().isEmpty());
            assertFalse(event.isCancelled());
            event.setCancelled(true);
            assertTrue(event.isCancelled());
        }

        @Test
        void questProgressEventFieldsStored() {
            Player player = mock(Player.class);
            QuestProgressEvent event = new QuestProgressEvent(player, "gather-wood", 2, 5);
            assertEquals(player, event.getPlayer());
            assertEquals("gather-wood", event.getQuestId());
            assertEquals(2, event.getObjectiveIndex());
            assertEquals(5, event.getNewProgress());
        }

        @Test
        void menuOpenEventIsCancellable() {
            Player player = mock(Player.class);
            MenuOpenEvent event = new MenuOpenEvent(player, "shop-menu");
            assertEquals(player, event.getPlayer());
            assertEquals("shop-menu", event.getMenuId());
            assertFalse(event.isCancelled());
            event.setCancelled(true);
            assertTrue(event.isCancelled());
        }

        @Test
        void menuCloseEventFieldsStored() {
            Player player = mock(Player.class);
            MenuCloseEvent event = new MenuCloseEvent(player, "shop-menu");
            assertEquals(player, event.getPlayer());
            assertEquals("shop-menu", event.getMenuId());
        }

        @Test
        void questAbandonEventIsCancellable() {
            Player player = mock(Player.class);
            QuestAbandonEvent event = new QuestAbandonEvent(player, "dragon_hunter");
            assertEquals(player, event.getPlayer());
            assertEquals("dragon_hunter", event.getQuestId());
            assertFalse(event.isCancelled());
            event.setCancelled(true);
            assertTrue(event.isCancelled());
        }

        @Test
        void petStateChangeEventFieldsStored() {
            Player player = mock(Player.class);
            PetStateChangeEvent event = new PetStateChangeEvent(player, "dragon", "FOLLOWING", "SITTING");
            assertEquals(player, event.getPlayer());
            assertEquals("dragon", event.getPetProfile());
            assertEquals("FOLLOWING", event.getOldState());
            assertEquals("SITTING", event.getNewState());
        }

        @Test
        void reactiveRuleTriggeredEventFieldsStored() {
            Player player = mock(Player.class);
            ReactiveRuleTriggeredEvent event = new ReactiveRuleTriggeredEvent(player, "night-glow");
            assertEquals(player, event.getPlayer());
            assertEquals("night-glow", event.getRuleId());
        }
    }

    @Nested
    class BuilderTests {
        @Test
        void hologramBuilderRequiresLocation() {
            HologramManager manager = mock(HologramManager.class);
            HologramBuilder builder = new HologramBuilder("test", manager);
            builder.lines("<gold>Hello");
            assertThrows(IllegalStateException.class, builder::spawn);
        }
    }
}
