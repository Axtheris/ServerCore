package net.axther.serverCore.hologram;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class HologramDataModelTest {

    @Nested
    class VisualOptionsTests {

        @Test
        void defaultVisualOptionsHaveSensibleDefaults() {
            Hologram h = new Hologram("test", null, List.of("hello"));
            assertEquals("CENTER", h.getBillboard());
            assertFalse(h.isTextShadow());
            assertNull(h.getBackground());
            assertEquals(200, h.getLineWidth());
            assertFalse(h.isSeeThrough());
            assertEquals("CENTER", h.getTextAlignment());
            assertEquals(1.0f, h.getViewRange(), 0.001f);
        }

        @Test
        void settersUpdateVisualOptions() {
            Hologram h = new Hologram("test", null, List.of("hello"));
            h.setBillboard("HORIZONTAL");
            h.setTextShadow(true);
            h.setBackground("#80000000");
            h.setLineWidth(300);
            h.setSeeThrough(true);
            h.setTextAlignment("LEFT");
            h.setViewRange(2.5f);

            assertEquals("HORIZONTAL", h.getBillboard());
            assertTrue(h.isTextShadow());
            assertEquals("#80000000", h.getBackground());
            assertEquals(300, h.getLineWidth());
            assertTrue(h.isSeeThrough());
            assertEquals("LEFT", h.getTextAlignment());
            assertEquals(2.5f, h.getViewRange(), 0.001f);
        }

        @Test
        void backgroundCanBeSetToNullAfterBeingSet() {
            Hologram h = new Hologram("test", null, List.of("hello"));
            h.setBackground("#FF000000");
            assertNotNull(h.getBackground());
            h.setBackground(null);
            assertNull(h.getBackground());
        }

        @Test
        void billboardAcceptsVariousValues() {
            Hologram h = new Hologram("test", null, List.of("hello"));
            h.setBillboard("VERTICAL");
            assertEquals("VERTICAL", h.getBillboard());
            h.setBillboard("FIXED");
            assertEquals("FIXED", h.getBillboard());
        }

        @Test
        void textAlignmentAcceptsVariousValues() {
            Hologram h = new Hologram("test", null, List.of("hello"));
            h.setTextAlignment("LEFT");
            assertEquals("LEFT", h.getTextAlignment());
            h.setTextAlignment("RIGHT");
            assertEquals("RIGHT", h.getTextAlignment());
        }

        @Test
        void viewRangeAcceptsZeroAndNegative() {
            Hologram h = new Hologram("test", null, List.of("hello"));
            h.setViewRange(0.0f);
            assertEquals(0.0f, h.getViewRange(), 0.001f);
            h.setViewRange(-1.0f);
            assertEquals(-1.0f, h.getViewRange(), 0.001f);
        }

        @Test
        void lineWidthAcceptsZero() {
            Hologram h = new Hologram("test", null, List.of("hello"));
            h.setLineWidth(0);
            assertEquals(0, h.getLineWidth());
        }
    }

    @Nested
    class UpdateIntervalTests {
        @Test
        void defaultUpdateIntervalIsTwenty() {
            Hologram h = new Hologram("test", null, List.of("hello"));
            assertEquals(20, h.getUpdateInterval());
        }

        @Test
        void setUpdateInterval() {
            Hologram h = new Hologram("test", null, List.of("hello"));
            h.setUpdateInterval(40);
            assertEquals(40, h.getUpdateInterval());
        }

        @Test
        void containsPlaceholdersDetectsPercent() {
            Hologram h = new Hologram("test", null, List.of("Hello %player_name%"));
            assertTrue(h.containsPlaceholders());
        }

        @Test
        void containsPlaceholdersReturnsFalseForStaticText() {
            Hologram h = new Hologram("test", null, List.of("<gold>Static text"));
            assertFalse(h.containsPlaceholders());
        }

        @Test
        void containsPlaceholdersReturnsFalseForSinglePercent() {
            Hologram h = new Hologram("test", null, List.of("50% off sale"));
            assertFalse(h.containsPlaceholders());
        }

        @Test
        void containsPlaceholdersDetectsAcrossMultipleLines() {
            Hologram h = new Hologram("test", null, List.of("Line 1", "Score: %player_level%"));
            assertTrue(h.containsPlaceholders());
        }
    }
}
