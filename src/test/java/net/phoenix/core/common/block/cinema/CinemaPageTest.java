package net.phoenix.core.common.block.cinema;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CinemaPageTest {

    @Test
    void aPlainLineIsJustItsBody() {
        var page = CinemaPage.parse("Right-click to advance");
        assertEquals("", page.title());
        assertEquals("Right-click to advance", page.body());
        assertTrue(page.buttons().isEmpty());
    }

    @Test
    void theFirstOfSeveralLinesIsTheTitleAndButtonsAreCollected() {
        var page = CinemaPage.parse("THE CHOICE\nYour first choice is free.\n@button Choose | open_discipline_picker\n@button Next");
        assertEquals("THE CHOICE", page.title());
        assertEquals("Your first choice is free.", page.body());
        assertEquals(2, page.buttons().size());
        assertEquals("open_discipline_picker", page.buttons().get(0).action());
        assertEquals("next", page.buttons().get(1).action());
    }

    @Test
    void backslashNIsALineBreakAndHashMakesATitle() {
        var typed = CinemaPage.parse("Title\nBody text");
        assertEquals("Title", typed.title());
        assertEquals("Body text", typed.body());
        var hashed = CinemaPage.parse("# Only a title");
        assertEquals("Only a title", hashed.title());
        assertEquals("", hashed.body());
    }
}
