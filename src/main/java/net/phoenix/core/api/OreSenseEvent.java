package net.phoenix.core.api;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

import java.util.UUID;

public abstract class OreSenseEvent extends Event {

    private final MinecraftServer server;
    private final UUID team;
    private final String discipline;

    protected OreSenseEvent(MinecraftServer server, UUID team, String discipline) {
        this.server = server;
        this.team = team;
        this.discipline = discipline;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public UUID getTeam() {
        return team;
    }

    public String getDiscipline() {
        return discipline;
    }

    public static final class Remember extends OreSenseEvent implements ICancellableEvent {

        private final Block block;
        private final int chunkX;
        private final int chunkZ;
        private final int count;

        public Remember(MinecraftServer server, UUID team, String discipline, Block block, int chunkX, int chunkZ,
                        int count) {
            super(server, team, discipline);
            this.block = block;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.count = count;
        }

        public Block getBlock() {
            return block;
        }

        public int getChunkX() {
            return chunkX;
        }

        public int getChunkZ() {
            return chunkZ;
        }

        public int getCount() {
            return count;
        }
    }
}
