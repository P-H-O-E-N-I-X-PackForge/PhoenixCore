package net.phoenix.core.api.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Posting side of the Conflux events, so the call sites stay one line. */
public final class ConfluxEvents {

    private ConfluxEvents() {}

    /** Posts the Pre event and returns the context to hand to {@link #postResearch}. */
    public static Map<String, Object> preResearch(MinecraftServer server, UUID team, ResourceLocation nodeId) {
        Map<String, Object> context = new HashMap<>();
        NeoForge.EVENT_BUS.post(new ConfluxResearchEvent.Pre(server, team, nodeId, context));
        return context;
    }

    public static void postResearch(MinecraftServer server, UUID team, ResourceLocation nodeId,
                                    Map<String, Object> context) {
        NeoForge.EVENT_BUS.post(new ConfluxResearchEvent.Post(server, team, nodeId, context));
    }

    public static void teamChanged(MinecraftServer server, UUID team) {
        NeoForge.EVENT_BUS.post(new ConfluxTeamChangedEvent(server, team));
    }
}
