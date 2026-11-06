package net.phoenix.core.integration.conflux.research;

import net.phoenix.core.integration.conflux.research.ResearchCategories.Entry;
import net.phoenix.core.integration.conflux.research.ResearchCategories.Item;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResearchCategoriesTest {

    private static final List<String> TOP = List.of("Foundations", "Discipline", "Chronicle");

    private static String show(List<Entry> entries) {
        StringBuilder sb = new StringBuilder();
        for (Entry e : entries) {
            sb.append("  ".repeat(e.depth())).append(e.isHeader() ? "[" + e.name() + (e.collapsed() ? " +" : "") + "]" :
                    "t" + e.treeIndex()).append('\n');
        }
        return sb.toString();
    }

    @Test
    void aGroupWithoutASlashIsATopLevelCategoryLikeBefore() {
        List<Entry> out = ResearchCategories.layout(List.of(
                new Item(0, "Chronicle", 0, "b"), new Item(1, "Foundations", 0, "a"), new Item(2, "Chronicle", 0, "a")),
                TOP, Set.of());
        assertEquals("""
                [Foundations]
                  t1
                [Chronicle]
                  t2
                  t0
                """, show(out));
    }

    @Test
    void slashesNestCategoriesToAnyDepthAndTiersFollowTheirOrder() {
        List<Entry> out = ResearchCategories.layout(List.of(
                new Item(0, "Discipline/Tier 2", 2, "Second"),
                new Item(1, "Discipline/Tier 1", 1, "First"),
                new Item(2, "Discipline/Tier 1/Side", 1, "Side branch"),
                new Item(3, "Discipline", 0, "Overview"),
                new Item(4, "Discipline/Tier 3/Deep/Deeper", 3, "Deepest")), TOP, Set.of());
        assertEquals("""
                [Discipline]
                  t3
                  [Tier 1]
                    t1
                    [Side]
                      t2
                  [Tier 2]
                    t0
                  [Tier 3]
                    [Deep]
                      [Deeper]
                        t4
                """, show(out));
    }

    @Test
    void aCollapsedCategoryShowsOnlyItsHeaderAndKnowsEverythingInsideIt() {
        List<Item> items = List.of(
                new Item(0, "Discipline/Tier 1", 1, "a"), new Item(1, "Discipline/Tier 2", 2, "b"),
                new Item(2, "Discipline/Tier 2/Deep", 2, "c"));
        List<Entry> out = ResearchCategories.layout(items, TOP, Set.of("Discipline/Tier 2"));
        assertEquals("""
                [Discipline]
                  [Tier 1]
                    t0
                  [Tier 2 +]
                """, show(out));
        Entry shut = out.get(out.size() - 1);
        assertTrue(shut.collapsed());
        assertEquals(List.of(1, 2), shut.trees());

        List<Entry> all = ResearchCategories.layout(items, TOP, Set.of("Discipline"));
        assertEquals(1, all.size());
        assertEquals(List.of(0, 1, 2), all.get(0).trees());
    }

    @Test
    void emptyAndSloppyPathsFallBackToResearch() {
        assertEquals(List.of("Research"), ResearchCategories.segments(""));
        assertEquals(List.of("Research"), ResearchCategories.segments(" / "));
        assertEquals(List.of("A", "B"), ResearchCategories.segments(" A / B /"));
        assertEquals(List.of("Research"), ResearchCategories.segments(null));
    }
}
