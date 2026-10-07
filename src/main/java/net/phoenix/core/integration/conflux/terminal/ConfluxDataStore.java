package net.phoenix.core.integration.conflux.terminal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.integration.conflux.ConfluxDataType;
import net.phoenix.core.integration.conflux.network.ConfluxNetwork;
import net.phoenix.core.integration.conflux.research.ResearchTeamHelper;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Conflux data each team has banked. Research Terminals are only a way in (pipes insert into the owner's team
 * here) and a way to look at it, so breaking one loses nothing.
 */
@Mod.EventBusSubscriber(modid = "phoenixcore")
public class ConfluxDataStore extends SavedData {

    public static final long CAPACITY_PER_TYPE = 1_000_000L;
    private static final String ID = "conflux_team_data";

    private final Map<UUID, Map<ConfluxDataType, Long>> data = new HashMap<>();
    private final Set<UUID> dirtyTeams = new HashSet<>();

    public static ConfluxDataStore get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(ConfluxDataStore::load, ConfluxDataStore::new, ID);
    }

    private Map<ConfluxDataType, Long> map(UUID team) {
        return data.computeIfAbsent(team, t -> new EnumMap<>(ConfluxDataType.class));
    }

    public long stored(UUID team, ConfluxDataType type) {
        Map<ConfluxDataType, Long> m = data.get(team);
        return m == null ? 0L : m.getOrDefault(type, 0L);
    }

    public Map<ConfluxDataType, Long> snapshot(UUID team) {
        Map<ConfluxDataType, Long> copy = new EnumMap<>(ConfluxDataType.class);
        for (ConfluxDataType t : ConfluxDataType.values()) copy.put(t, stored(team, t));
        return copy;
    }

    /** Adds up to the capacity and returns how much was accepted. */
    public long insert(UUID team, ConfluxDataType type, long amount) {
        long have = stored(team, type);
        long accepted = Math.max(0L, Math.min(amount, CAPACITY_PER_TYPE - have));
        if (accepted > 0) {
            map(team).put(type, have + accepted);
            touch(team);
        }
        return accepted;
    }

    public long extract(UUID team, ConfluxDataType type, long amount) {
        long have = stored(team, type);
        long given = Math.max(0L, Math.min(amount, have));
        if (given > 0) {
            map(team).put(type, have - given);
            touch(team);
        }
        return given;
    }

    public boolean trySpend(UUID team, Map<ConfluxDataType, Long> costs) {
        for (var e : costs.entrySet()) {
            if (stored(team, e.getKey()) < e.getValue()) return false;
        }
        for (var e : costs.entrySet()) extract(team, e.getKey(), e.getValue());
        return true;
    }

    private void touch(UUID team) {
        dirtyTeams.add(team);
        setDirty();
    }

    /** Pushes changed balances to the team's online players, at most once a second. */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 0) return;

        ConfluxDataStore store = get(server.overworld());
        if (store.dirtyTeams.isEmpty()) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (store.dirtyTeams.contains(ResearchTeamHelper.getTeamId(player))) {
                ConfluxNetwork.syncResearchToPlayer(player);
            }
        }
        store.dirtyTeams.clear();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag teams = new CompoundTag();
        data.forEach((team, m) -> {
            CompoundTag t = new CompoundTag();
            m.forEach((type, amt) -> t.putLong(type.id(), amt));
            teams.put(team.toString(), t);
        });
        tag.put("teams", teams);
        return tag;
    }

    public static ConfluxDataStore load(CompoundTag tag) {
        ConfluxDataStore store = new ConfluxDataStore();
        CompoundTag teams = tag.getCompound("teams");
        for (String key : teams.getAllKeys()) {
            try {
                UUID team = UUID.fromString(key);
                CompoundTag t = teams.getCompound(key);
                for (ConfluxDataType type : ConfluxDataType.values()) {
                    if (t.contains(type.id())) store.map(team).put(type, t.getLong(type.id()));
                }
            } catch (IllegalArgumentException ignored) {}
        }
        return store;
    }
}
