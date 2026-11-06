package net.phoenix.core.integration.conflux.research;

import net.phoenix.core.integration.conflux.research.ResearchTreeValidator.Issue;
import net.phoenix.core.integration.conflux.research.ResearchTreeValidator.Node;
import net.phoenix.core.integration.conflux.research.ResearchTreeValidator.Severity;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResearchTreeValidatorTest {

    private static Node node(String id, int x, int y, String... prereqs) {
        return new Node(id, "Title " + id, x, y, List.of(prereqs), "", true, List.of("flag"));
    }

    private static boolean has(List<Issue> issues, Severity severity, String nodeId, String part) {
        return issues.stream().anyMatch(i -> i.severity() == severity && java.util.Objects.equals(i.nodeId(), nodeId) &&
                i.message().contains(part));
    }

    @Test
    void aCleanTreeHasNoIssues() {
        List<Issue> issues = ResearchTreeValidator.check(List.of(node("a", 0, 0), node("b", 1, 0, "a"),
                node("c", 2, 0, "b")));
        assertEquals(List.of(), issues);
        assertFalse(ResearchTreeValidator.hasErrors(issues));
    }

    @Test
    void findsLoopsMissingPrerequisitesAndDuplicateIds() {
        List<Issue> issues = ResearchTreeValidator.check(List.of(node("a", 0, 0), node("b", 1, 0, "c"),
                node("c", 2, 0, "b"), node("d", 3, 0, "ghost"), node("a", 4, 0)));
        assertTrue(has(issues, Severity.ERROR, "b", "loop"));
        assertTrue(has(issues, Severity.ERROR, "c", "loop"));
        assertTrue(has(issues, Severity.ERROR, "d", "not in this tree"));
        assertTrue(has(issues, Severity.ERROR, "a", "share the id"));
        assertTrue(ResearchTreeValidator.hasErrors(issues));
        assertEquals(Severity.ERROR, issues.get(0).severity());
    }

    @Test
    void aTreeWithNoRootCannotBeStartedAndAnEmptyOneIsNoted() {
        List<Issue> noRoot = ResearchTreeValidator.check(List.of(node("a", 0, 0, "b"), node("b", 1, 0, "a")));
        assertTrue(has(noRoot, Severity.ERROR, null, "nothing can be started"));
        assertTrue(has(ResearchTreeValidator.check(List.of()), Severity.WARNING, null, "no nodes"));
    }

    @Test
    void warnsAboutTheEasyMistakes() {
        Node blank = new Node("x", "", 5, 5, List.of(), "", false, List.of());
        Node odd = new Node("y", "Odd", 6, 5, List.of(), "alone", true, List.of("teleport"));
        Node same = new Node("z", "Same", 6, 5, List.of(), "", true, List.of("flag"));
        List<Issue> issues = ResearchTreeValidator.check(List.of(blank, odd, same));
        assertTrue(has(issues, Severity.WARNING, "x", "no title"));
        assertTrue(has(issues, Severity.WARNING, "x", "unlocks nothing"));
        assertTrue(has(issues, Severity.WARNING, "y", "Unknown unlock type 'teleport'"));
        assertTrue(has(issues, Severity.WARNING, "y", "only node in the exclusion group"));
        assertTrue(has(issues, Severity.WARNING, "y", "on top of another"));
        assertTrue(has(issues, Severity.WARNING, "z", "on top of another"));
        assertFalse(ResearchTreeValidator.hasErrors(issues));
    }
}
