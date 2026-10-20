package net.phoenix.core.api.event;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.Event;

import java.util.UUID;

/**
 * A team's Conflux state changed in a way other mods may need to resync: a discipline was chosen or abandoned, or a dev
 * command rewrote their progress. Research unlocks have their own event, {@link ConfluxResearchEvent}.
 */
public class ConfluxTeamChangedEvent extends Event {

    private final MinecraftServer server;
    private final UUID team;

    public ConfluxTeamChangedEvent(MinecraftServer server, UUID team) {
        this.server = server;
        this.team = team;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public UUID getTeam() {
        return team;
    }
}
