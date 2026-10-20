package net.phoenix.core.api.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.integration.conflux.research.ResearchNode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * Lets other mods add lines to the "Also" section of the research node viewer, such as "Surveys Rime". Register a provider
 * once at client setup; it is called with the node being viewed and returns formatted text lines (may be empty).
 */
@OnlyIn(Dist.CLIENT)
public final class ConfluxNodeNotes {

    private ConfluxNodeNotes() {}

    private static final List<Function<ResearchNode, List<String>>> PROVIDERS = new CopyOnWriteArrayList<>();

    public static void register(Function<ResearchNode, List<String>> provider) {
        PROVIDERS.add(provider);
    }

    public static List<String> notesFor(ResearchNode node) {
        List<String> out = new ArrayList<>();
        for (var provider : PROVIDERS) out.addAll(provider.apply(node));
        return out;
    }
}
