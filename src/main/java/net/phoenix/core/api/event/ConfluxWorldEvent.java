package net.phoenix.core.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

import java.util.Map;

public class ConfluxWorldEvent extends Event {

    private final ServerPlayer player;
    private final String type;
    private final Map<String, Object> data;

    public ConfluxWorldEvent(ServerPlayer player, String type, Map<String, Object> data) {
        this.player = player;
        this.type = type;
        this.data = data;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> getData() {
        return data;
    }
}
