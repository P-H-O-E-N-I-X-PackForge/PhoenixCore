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
        var typed = CinemaPage.parse("Title\\nBody text");
        assertEquals("Title", typed.title());
        assertEquals("Body text", typed.body());
        var hashed = CinemaPage.parse("# Only a title");
        assertEquals("Only a title", hashed.title());
        assertEquals("", hashed.body());
    }

    @Test
    void aConditionIsCollectedAndIsNotShown() {
        var page = CinemaPage.parse("YOUR PATH\nYou follow it.\n@if has_discipline");
        assertEquals("has_discipline", page.condition());
        assertEquals("You follow it.", page.body());
    }

    @Test
    void picturesAreCollectedWithTheirCaptions() {
        var page = CinemaPage.parse("HUD\nBody\n@image mod:textures/gui/a.png | The map\n@item minecraft:blaze_rod | Fire");
        assertEquals(2, page.pictures().size());
        assertEquals("mod:textures/gui/a.png", page.pictures().get(0).id());
        assertEquals("The map", page.pictures().get(0).caption());
        assertTrue(page.pictures().get(1).item());
        assertEquals("Body", page.body());
    }

    @Test
    void aContinuumBodyIsAPictureWithItsColour() {
        var page = CinemaPage.parse("SKY\nBody\n@continuum Star #ffcc66 | A star\n@continuum black_hole | Hole");
        assertEquals(2, page.pictures().size());
        assertEquals("continuum:star:ffcc66", page.pictures().get(0).id());
        assertEquals("A star", page.pictures().get(0).caption());
        assertEquals("continuum:black_hole:-", page.pictures().get(1).id());
    }

    @Test
    void aPictureCanHaveAClickAction() {
        var page = CinemaPage.parse("HUD\n@image mod:textures/a.png | The map | hud:solaris\n@item minecraft:stick | Stick");
        assertEquals("The map", page.pictures().get(0).caption());
        assertEquals("hud:solaris", page.pictures().get(0).action());
        assertEquals("", page.pictures().get(1).action());
    }

    @Test
    void noHintTurnsTheHintOff() {
        assertTrue(CinemaPage.parse("A\nB").hint());
        var page = CinemaPage.parse("A\nB\n@nohint");
        assertEquals(false, page.hint());
        assertEquals("B", page.body());
    }

    @Test
    void scrollTakesAnOptionalSpeed() {
        assertEquals(0.0f, CinemaPage.parse("A\nB").scroll());
        assertEquals(0.3f, CinemaPage.parse("A\nB\n@scroll").scroll());
        var fast = CinemaPage.parse("A\nB\n@scroll 0.5");
        assertEquals(0.5f, fast.scroll());
        assertEquals("B", fast.body());
    }
}
