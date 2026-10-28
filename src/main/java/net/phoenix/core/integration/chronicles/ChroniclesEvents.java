package net.phoenix.core.integration.chronicles;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.api.event.ConfluxResearchEvent;

/** Conflux events passed on to Chronicles. */
@EventBusSubscriber(modid = PhoenixCore.MOD_ID)
public final class ChroniclesEvents {

    private ChroniclesEvents() {}

    @SubscribeEvent
    public static void onResearch(ConfluxResearchEvent.Post event) {
        ChroniclesLink.fireTeam(event.getServer(), event.getTeam(), PhoenixChroniclesTriggers.CONFLUX_RESEARCH,
                ChroniclesLink.data("node", event.getNodeId().toString()));
    }
}
