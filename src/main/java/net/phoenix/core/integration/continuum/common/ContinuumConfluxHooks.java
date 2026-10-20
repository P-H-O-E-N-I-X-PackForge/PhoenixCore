package net.phoenix.core.integration.continuum.common;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.api.event.ConfluxResearchEvent;
import net.phoenix.core.api.event.ConfluxTeamChangedEvent;
import net.phoenix.core.integration.continuum.data.DiscoveryStage;

import java.util.Map;

/**
 * Everything Continuum does in answer to Conflux, through PhoenixCore's public events only. When Continuum becomes its own
 * mod this class moves with it unchanged.
 */
@EventBusSubscriber(modid = PhoenixCore.MOD_ID)
public final class ContinuumConfluxHooks {

    private ContinuumConfluxHooks() {}

    private static final String SNAPSHOT = "continuum_discovery_before";

    @SubscribeEvent
    public static void onResearchPre(ConfluxResearchEvent.Pre event) {
        event.getContext().put(SNAPSHOT, ContinuumDiscoveryNotifier.snapshot(event.getServer(), event.getTeam()));
    }

    @SuppressWarnings("unchecked")
    @SubscribeEvent
    public static void onResearchPost(ConfluxResearchEvent.Post event) {
        Object before = event.getContext().get(SNAPSHOT);
        if (before instanceof Map<?, ?> snapshot) {
            ContinuumDiscoveryNotifier.announce(event.getServer(), event.getTeam(),
                    (Map<ResourceLocation, DiscoveryStage>) snapshot);
        }
        ContinuumServerEvents.sendStateToTeam(event.getServer(), event.getTeam());
    }

    @SubscribeEvent
    public static void onTeamChanged(ConfluxTeamChangedEvent event) {
        ContinuumServerEvents.sendStateToTeam(event.getServer(), event.getTeam());
    }
}
