package net.phoenix.core.api.event;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.Event;

import java.util.UUID;

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
