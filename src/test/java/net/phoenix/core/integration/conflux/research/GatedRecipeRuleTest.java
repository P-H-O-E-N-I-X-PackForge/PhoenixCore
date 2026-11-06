package net.phoenix.core.integration.conflux.research;

import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatedRecipeRuleTest {

    private static GatedRecipeRule rule(String flag, List<String> only, List<String> except, boolean crafting,
                                        ResourceLocation... ids) {
        return new GatedRecipeRule(flag, List.of(ids), List.of(), List.of(), List.of(), only, except, crafting);
    }

    private static final ResourceLocation IRON_GEAR = ResourceLocation.fromNamespaceAndPath("phoenixcore",
            "gear_a");

    @Test
    void aRuleWithNoListsIsAboutEveryone() {
        var r = rule("flag", List.of(), List.of(), false);
        assertTrue(r.inScope(null));
        assertTrue(r.inScope("phoenix"));
    }

    @Test
    void onlyDisciplinesRestrictsJustThose() {
        var r = rule("", List.of("phoenix"), List.of(), false);
        assertTrue(r.inScope("phoenix"));
        assertFalse(r.inScope("sculk"));
        assertFalse(r.inScope(null));
    }

    @Test
    void exceptDisciplinesRestrictsEveryoneElseIncludingATeamWithNoPath() {
        var r = rule("", List.of(), List.of("phoenix"), false);
        assertFalse(r.inScope("phoenix"));
        assertTrue(r.inScope("sculk"));
        assertTrue(r.inScope(null));
    }

    @Test
    void aFlagLiftsTheRestrictionButOnlyInsideTheScope() {
        var r = rule("px_flag", List.of("phoenix"), List.of(), false);
        assertTrue(r.blocks("phoenix", f -> false));
        assertFalse(r.blocks("phoenix", Set.of("px_flag")::contains));
        assertFalse(r.blocks("sculk", f -> false));
    }

    @Test
    void aRuleWithNoFlagClosesTheRecipeForGoodAndIsHiddenFromTheViewer() {
        var r = rule("", List.of(), List.of("phoenix"), false);
        assertTrue(r.blocks("sculk", f -> true));
        assertTrue(r.hides());
        assertFalse(rule("px_flag", List.of("phoenix"), List.of(), false).hides());
        assertFalse(rule("", List.of(), List.of(), false).hides());
    }

    @Test
    void onlyRulesMarkedCraftingReachCraftingRecipes() {
        assertTrue(rule("", List.of("phoenix"), List.of(), true, IRON_GEAR).namesCraftingId(IRON_GEAR));
        assertFalse(rule("", List.of("phoenix"), List.of(), false, IRON_GEAR).namesCraftingId(IRON_GEAR));
        assertFalse(rule("", List.of("phoenix"), List.of(), true).namesCraftingId(IRON_GEAR));
    }

    @Test
    void theSameRecipeInTwoVariantsIsOpenToExactlyOneDiscipline() {
        var forPhoenix = rule("", List.of(), List.of("phoenix"), true, IRON_GEAR);
        var forTheRest = rule("", List.of("phoenix"), List.of(), true,
                ResourceLocation.fromNamespaceAndPath("phoenixcore", "gear_b"));
        for (String d : new String[] { "phoenix", "sculk", "void", null }) {
            boolean aOpen = !forPhoenix.blocks(d, f -> false);
            boolean bOpen = !forTheRest.blocks(d, f -> false);
            assertEquals("phoenix".equals(d), aOpen, "variant A for " + d);
            assertEquals(!"phoenix".equals(d), bOpen, "variant B for " + d);
        }
    }
}
