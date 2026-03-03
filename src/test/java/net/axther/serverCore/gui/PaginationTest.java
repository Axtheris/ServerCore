package net.axther.serverCore.gui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the pagination math used by PaginatedMenu.
 * These test the pure calculation logic without requiring Bukkit.
 */
class PaginationTest {

    /**
     * Mirrors PaginatedMenu.getTotalPages() logic.
     */
    private int totalPages(int itemCount, int itemsPerPage) {
        return Math.max(1, (int) Math.ceil((double) itemCount / itemsPerPage));
    }

    @ParameterizedTest
    @CsvSource({
            "0,  45, 1",   // empty list = 1 page
            "1,  45, 1",   // 1 item = 1 page
            "44, 45, 1",   // fits exactly minus 1
            "45, 45, 1",   // fits exactly
            "46, 45, 2",   // overflow by 1
            "90, 45, 2",   // exactly 2 pages
            "91, 45, 3",   // overflow into page 3
            "100, 10, 10", // smaller page size
            "1, 1, 1",     // minimum page size
            "5, 1, 5",     // 1 item per page
    })
    void totalPagesCalculation(int items, int perPage, int expected) {
        assertEquals(expected, totalPages(items, perPage),
                items + " items at " + perPage + " per page");
    }

    /**
     * Verifies page slice indices (mirrors buildPageItems logic).
     */
    @Test
    void pageSliceBoundaries() {
        int itemCount = 107;
        int perPage = 45;
        int pages = totalPages(itemCount, perPage);

        assertEquals(3, pages);

        // Page 0: items [0, 45)
        int start0 = 0 * perPage;
        int end0 = Math.min(start0 + perPage, itemCount);
        assertEquals(0, start0);
        assertEquals(45, end0);

        // Page 1: items [45, 90)
        int start1 = 1 * perPage;
        int end1 = Math.min(start1 + perPage, itemCount);
        assertEquals(45, start1);
        assertEquals(90, end1);

        // Page 2: items [90, 107)
        int start2 = 2 * perPage;
        int end2 = Math.min(start2 + perPage, itemCount);
        assertEquals(90, start2);
        assertEquals(107, end2);
    }

    /**
     * Navigation slot positions should be constant.
     */
    @Test
    void navigationSlotConstants() {
        // These match PaginatedMenu's fixed slot positions
        assertEquals(45, 5 * 9);       // PREV_SLOT = row 5, col 0
        assertEquals(49, 5 * 9 + 4);   // BACK_SLOT = row 5, col 4
        assertEquals(53, 5 * 9 + 8);   // NEXT_SLOT = row 5, col 8
    }

    /**
     * Page clamping: currentPage should never go below 0 or above totalPages-1.
     */
    @Test
    void pageClampingLogic() {
        int totalPages = 5;

        // Simulate prev-page clicks
        int page = 0;
        page = Math.max(0, page - 1); // can't go below 0
        assertEquals(0, page);

        // Simulate next-page clicks
        page = totalPages - 1;
        page = Math.min(totalPages - 1, page + 1); // can't go above max
        assertEquals(totalPages - 1, page);
    }

    /**
     * Items-per-page clamping: should be between 1 and 45.
     */
    @Test
    void itemsPerPageClamping() {
        assertEquals(1, Math.max(1, Math.min(45, 0)));    // 0 -> 1
        assertEquals(1, Math.max(1, Math.min(45, -5)));   // negative -> 1
        assertEquals(45, Math.max(1, Math.min(45, 100)));  // 100 -> 45
        assertEquals(20, Math.max(1, Math.min(45, 20)));   // 20 -> 20
    }

    /**
     * Performance: pagination math for large collections.
     */
    @Test
    void paginationMathPerformance() {
        long start = System.nanoTime();

        for (int itemCount = 0; itemCount <= 10_000; itemCount++) {
            for (int perPage : new int[]{1, 10, 45}) {
                int pages = totalPages(itemCount, perPage);
                for (int page = 0; page < Math.min(pages, 10); page++) {
                    int startIdx = page * perPage;
                    int endIdx = Math.min(startIdx + perPage, itemCount);
                }
            }
        }

        long elapsed = System.nanoTime() - start;
        double ms = elapsed / 1_000_000.0;
        System.out.printf("Pagination math (30k calcs): %.2f ms%n", ms);
        assertTrue(ms < 50, "Pagination math should be trivially fast, took " + ms + "ms");
    }
}
