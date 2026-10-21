package net.phoenix.core.api.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.integration.conflux.research.ResearchNode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

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
