package net.phoenix.core.integration.recipe_helper;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecipeDumpAnalysisTest {

    private static final int LV = 1;
    private static final int HV = 3;
    private static final int EV = 4;
    private static final int IV = 5;

    private static List<List<Integer>> in(int... items) {
        return java.util.Arrays.stream(items).mapToObj(i -> List.of(i)).toList();
    }

    @Test
    void titaniumCanBeMadeInHvButIsNotNeededUntilEv() {
        var smelt = RecipeDump.Node.of("smelt_titanium", true, HV, in(0), List.of(1));
        var plate = RecipeDump.Node.of("titanium_plate", true, EV, in(1), List.of(2));
        var hull = RecipeDump.Node.of("hull", false, 0, in(2), List.of(3));
        List<RecipeDump.Node> nodes = List.of(smelt, plate, hull);

        RecipeDump.analyse(nodes, 4);

        assertEquals(HV, smelt.tier);
        assertEquals(EV, plate.tier);
        assertEquals(EV, hull.tier, "hand-crafting waits for the plate");
        assertEquals(EV, smelt.need, "titanium is first asked for by an EV recipe");
        assertEquals("titanium_plate", smelt.needBy);
        assertEquals(EV, plate.need);
    }

    @Test
    void aRecipeIsNeededWhenWhatItMakesIsNeededDownTheChain() {
        var makeA = RecipeDump.Node.of("make_a", true, LV, in(0), List.of(1));
        var makeB = RecipeDump.Node.of("make_b", true, LV, in(1), List.of(2));
        var makeC = RecipeDump.Node.of("make_c", true, IV, in(2), List.of(3));
        RecipeDump.analyse(List.of(makeA, makeB, makeC), 4);

        assertEquals(IV, makeC.need);
        assertEquals(IV, makeB.need, "b is needed when c is");
        assertEquals(IV, makeA.need, "and so a is");
    }

    @Test
    void recyclingLoopsSettleAndAnEarlyAskShowsAsABottleneck() {
        var makeX = RecipeDump.Node.of("make_x", true, HV, in(0), List.of(1));
        var xToY = RecipeDump.Node.of("x_to_y", true, HV, in(1), List.of(2));
        var yToX = RecipeDump.Node.of("y_to_x", true, HV, in(2), List.of(1));
        var eat = RecipeDump.Node.of("eat_x", true, LV, in(1), List.of(3));
        RecipeDump.analyse(List.of(makeX, xToY, yToX, eat), 4);

        assertEquals(LV, makeX.need, "an LV recipe asks for x, though x cannot exist before HV");
        assertTrue(makeX.need < makeX.tier, "a bottleneck shows as a negative slack");
    }
}
