package net.phoenix.core.client.renderer.cinema;

import net.phoenix.core.integration.conflux.client.ClientResearchCache;

/**
 * The conditions a cinema page can have ({@code @if ...}), tested on the client for the player looking. Separate several
 * with spaces and all must hold; a {@code !} in front of one reverses it:
 * <ul>
 * <li>{@code no_discipline}, {@code has_discipline}, {@code committed};</li>
 * <li>{@code discipline:<id>} (for example {@code discipline:phoenix});</li>
 * <li>{@code flag:<flag>}: the team has that Conflux research flag.</li>
 * </ul>
 */
public final class CinemaConditions {

    private CinemaConditions() {}

    public static boolean test(String condition) {
        if (condition == null || condition.isBlank()) return true;
        for (String part : condition.trim().split("\s+")) {
            boolean negate = part.startsWith("!");
            String term = negate ? part.substring(1) : part;
            if (single(term) == negate) return false;
        }
        return true;
    }

    private static boolean single(String term) {
        var info = ClientResearchCache.getDisciplineInfo();
        if (term.equals("no_discipline")) return !info.hasDiscipline();
        if (term.equals("has_discipline")) return info.hasDiscipline();
        if (term.equals("committed")) return info.committed();
        if (term.startsWith("discipline:")) {
            return info.disciplineId() != null && info.disciplineId().equalsIgnoreCase(term.substring(11));
        }
        if (term.startsWith("flag:")) return ClientResearchCache.flags.contains(term.substring(5));
        return false;
    }
}
