package net.phoenix.core.integration.conflux;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.phoenix.core.api.ConfluxApi;
import net.phoenix.core.integration.conflux.dimension.DisciplineThemeRegistry;
import net.phoenix.core.integration.conflux.research.ResearchTree;
import net.phoenix.core.integration.conflux.research.ResearchTreeRegistry;
import net.phoenixvine.wiki.api.gate.SuiteGates;

public final class ConfluxGates {

    private ConfluxGates() {}

    public static void register() {
        SuiteGates.registerResolver("conflux", ConfluxGates::resolve);
        SuiteGates.registerDescriber("conflux", ConfluxGates::describe);
    }

    private static Component describe(String rest) {
        if (rest.equals("discipline")) return Component.literal("a committed discipline");
        if (rest.startsWith("discipline/")) return Component.literal("the " + disciplineName(rest.substring(11)) +
                " discipline");
        if (rest.startsWith("core/")) return Component.literal("the " + disciplineName(rest.substring(5)) +
                " core research");
        if (rest.startsWith("flag/")) return Component.literal(rest.substring(5).replace('_', ' '));
        if (rest.startsWith("node/")) {
            ResourceLocation id = ResourceLocation.tryParse(rest.substring(5));
            if (id != null) {
                var node = ResearchTreeRegistry.INSTANCE.getNode(id);
                if (node.isPresent()) return Component.literal("the research " + node.get().title);
            }
        }
        return null;
    }

    private static String disciplineName(String id) {
        var theme = DisciplineThemeRegistry.getTheme(id);
        return theme != null ? theme.displayName : id.replace('_', ' ');
    }

    private static boolean resolve(String rest, ServerPlayer player) {
        if (rest.equals("discipline")) return ConfluxApi.discipline(player) != null;
        if (rest.startsWith("discipline/")) return rest.substring(11).equals(ConfluxApi.discipline(player));
        if (rest.startsWith("flag/")) return ConfluxApi.hasFlag(player, rest.substring(5));
        if (rest.startsWith("node/")) {
            ResourceLocation id = ResourceLocation.tryParse(rest.substring(5));
            return id != null && ConfluxApi.isUnlocked(player, id);
        }
        if (rest.startsWith("core/")) {
            String discipline = rest.substring(5);
            for (ResearchTree tree : ResearchTreeRegistry.INSTANCE.getAllTrees()) {
                if (!discipline.equals(tree.discipline)) continue;
                return tree.getCommitmentNode().map(core -> ConfluxApi.isUnlocked(player, core.id)).orElse(false);
            }
        }
        return false;
    }
}
