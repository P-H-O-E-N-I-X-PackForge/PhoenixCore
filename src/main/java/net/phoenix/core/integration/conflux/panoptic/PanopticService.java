package net.phoenix.core.integration.conflux.panoptic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.phoenix.core.integration.conflux.research.ResearchTeamHelper;
import net.phoenix.core.integration.conflux.research.WorldResearchData;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class PanopticService {

    private PanopticService() {}

    public record Outcome(PanopticPuzzle.Verdict verdict, boolean solved, boolean finished, float growth) {}

    public static @Nullable PanopticPuzzle.Round round(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        UUID team = ResearchTeamHelper.getTeamId(player);
        Testimony testimony = testimony(level, team);
        if (testimony == null) return null;
        return PanopticData.get(level).currentRound(team, testimony);
    }

    public static float growth(ServerPlayer player) {
        return PanopticData.get(player.serverLevel()).growth(ResearchTeamHelper.getTeamId(player));
    }

    public static @Nullable Outcome answer(ServerPlayer player, int[] values) {
        ServerLevel level = player.serverLevel();
        UUID team = ResearchTeamHelper.getTeamId(player);
        PanopticData data = PanopticData.get(level);
        Testimony testimony = testimony(level, team);
        if (testimony == null) return null;
        PanopticPuzzle puzzle = PanopticPuzzles.forDiscipline(testimony.discipline());
        PanopticPuzzle.Round round = data.currentRound(team, testimony);
        if (puzzle == null || round == null) return null;

        PanopticPuzzle.Verdict verdict = puzzle.check(round, new PanopticPuzzle.Answer(values));
        if (!verdict.correct()) {
            data.nudge(team, verdict.closeness());
            return new Outcome(verdict, false, false, data.growth(team));
        }

        int stageBefore = data.team(team).stage;
        boolean finished = data.solveRound(team);
        WorldResearchData research = WorldResearchData.get(level);
        research.grantFlag(team, PanopticData.stageFlag(stageBefore + 1));
        if (finished) research.grantFlag(team, PanopticData.COMPLETE_FLAG);
        return new Outcome(verdict, true, finished, data.growth(team));
    }

    public static java.util.List<String> memories(ServerPlayer player, int limit) {
        Testimony t = testimony(player.serverLevel(), ResearchTeamHelper.getTeamId(player));
        if (t == null) return java.util.List.of();
        java.util.List<String> out = new java.util.ArrayList<>();
        for (Testimony.Fact f : t.facts()) {
            if (out.size() >= limit) break;
            out.add(f.title());
        }
        return out;
    }

    public static int knownCount(ServerPlayer player) {
        Testimony t = testimony(player.serverLevel(), ResearchTeamHelper.getTeamId(player));
        return t == null ? 0 : t.unlocked();
    }

    public record Solution(int[] values, java.util.List<String> lines) {}

    public static @Nullable Solution solution(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        UUID team = ResearchTeamHelper.getTeamId(player);
        Testimony testimony = testimony(level, team);
        if (testimony == null) return null;
        PanopticPuzzle puzzle = PanopticPuzzles.forDiscipline(testimony.discipline());
        PanopticPuzzle.Round round = PanopticData.get(level).currentRound(team, testimony);
        if (puzzle == null || round == null) return null;
        int[] values = puzzle.solution(round);
        return new Solution(values, puzzle.describeSolution(round, values));
    }

    public static PanopticOffering offering(ServerPlayer player, int[] values) {
        ServerLevel level = player.serverLevel();
        UUID team = ResearchTeamHelper.getTeamId(player);
        Testimony testimony = testimony(level, team);
        if (testimony == null) return PanopticOffering.NONE;
        PanopticPuzzle puzzle = PanopticPuzzles.forDiscipline(testimony.discipline());
        PanopticPuzzle.Round round = PanopticData.get(level).currentRound(team, testimony);
        if (puzzle == null || round == null) return PanopticOffering.NONE;
        return puzzle.offering(round, new PanopticPuzzle.Answer(values));
    }

    public static String costHint(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        UUID team = ResearchTeamHelper.getTeamId(player);
        Testimony testimony = testimony(level, team);
        if (testimony == null) return "";
        PanopticPuzzle puzzle = PanopticPuzzles.forDiscipline(testimony.discipline());
        PanopticPuzzle.Round round = PanopticData.get(level).currentRound(team, testimony);
        return puzzle == null || round == null ? "" : puzzle.costHint(round);
    }

    public static @Nullable String discipline(ServerLevel level, UUID team) {
        PanopticData.Team t = PanopticData.get(level).team(team);
        if (t.devDiscipline != null) return t.devDiscipline;
        return WorldResearchData.get(level).getDiscipline(team);
    }

    private static @Nullable Testimony testimony(ServerLevel level, UUID team) {
        WorldResearchData research = WorldResearchData.get(level);
        String discipline = discipline(level, team);
        if (discipline == null || PanopticPuzzles.forDiscipline(discipline) == null) return null;
        return Testimony.of(team, discipline, research.getUnlocked(team));
    }
}
