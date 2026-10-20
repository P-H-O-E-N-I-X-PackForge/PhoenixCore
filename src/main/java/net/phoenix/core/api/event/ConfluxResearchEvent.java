package net.phoenix.core.api.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.Event;

import java.util.Map;
import java.util.UUID;

/**
 * A team is about to gain, or has just gained, a research node. Posted on the NeoForge bus on the server thread for every
 * route: the terminal, Research All, dev commands and {@code ConfluxApi.grantUnlock}. {@link Pre} and {@link Post} for one
 * unlock share a context map, so a listener can take a snapshot before and compare after.
 */
public abstract class ConfluxResearchEvent extends Event {

    private final MinecraftServer server;
    private final UUID team;
    private final ResourceLocation nodeId;
    private final Map<String, Object> context;

    protected ConfluxResearchEvent(MinecraftServer server, UUID team, ResourceLocation nodeId,
                                   Map<String, Object> context) {
        this.server = server;
        this.team = team;
        this.nodeId = nodeId;
        this.context = context;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public UUID getTeam() {
        return team;
    }

    public ResourceLocation getNodeId() {
        return nodeId;
    }

    /** Scratch space shared by the Pre and Post event of one unlock. Key it with your own mod id. */
    public Map<String, Object> getContext() {
        return context;
    }

    public static final class Pre extends ConfluxResearchEvent {

        public Pre(MinecraftServer server, UUID team, ResourceLocation nodeId, Map<String, Object> context) {
            super(server, team, nodeId, context);
        }
    }

    public static final class Post extends ConfluxResearchEvent {

        public Post(MinecraftServer server, UUID team, ResourceLocation nodeId, Map<String, Object> context) {
            super(server, team, nodeId, context);
        }
    }
}
