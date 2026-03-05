package net.axther.serverCore.gui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;

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
}
