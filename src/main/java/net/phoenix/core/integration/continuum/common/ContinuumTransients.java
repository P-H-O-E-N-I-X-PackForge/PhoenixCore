package net.phoenix.core.integration.continuum.common;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.phoenix.core.integration.continuum.data.ContinuumBody;
import net.phoenix.core.integration.continuum.data.ContinuumData;
import net.phoenix.core.utils.TeamUtils;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public final class ContinuumTransients extends SavedData {

    private record Window(long start, long end, long nextEligible, boolean ended) {}

    private final Map<ResourceLocation, Window> windows = new HashMap<>();
    private final Random random = new Random();

    public static ContinuumTransients get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(ContinuumTransients::load,
                ContinuumTransients::new, "phoenixcore_continuum_transients");
    }

    public boolean active(ResourceLocation body, long now) {
        Window window = windows.get(body);
        return window != null && now >= window.start && now < window.end;
    }

    public @Nullable long[] window(ResourceLocation body, long now) {
        Window window = windows.get(body);
        return window != null && now >= window.start && now < window.end ? new long[] { window.start, window.end } :
                null;
    }

    public Map<ResourceLocation, long[]> activeWindows(long now) {
        Map<ResourceLocation, long[]> open = new LinkedHashMap<>();
        windows.forEach((id, w) -> {
            if (now >= w.start && now < w.end) open.put(id, new long[] { w.start, w.end });
        });
        return open;
    }

    public void open(MinecraftServer server, ContinuumBody body, long minutes) {
        long now = System.currentTimeMillis();
        long end = now + minutes * 60_000L;
        var info = body.transientInfo();
        long cooldown = info == null ? 0L : info.cooldownMinutes() * 60_000L;
        windows.put(body.id(), new Window(now, end, end + cooldown, false));
        setDirty();
        announceArrival(server, body, end - now);
    }

    public void close(MinecraftServer server, ContinuumBody body) {
        Window window = windows.get(body.id());
        if (window == null) return;
        windows.put(body.id(), new Window(window.start, System.currentTimeMillis(), window.nextEligible, false));
        setDirty();
    }

    public void tick(MinecraftServer server) {
        long now = System.currentTimeMillis();
        for (ContinuumBody body : ContinuumData.allBodies()) {
            var info = body.transientInfo();
            if (info == null) continue;

            Window window = windows.get(body.id());
            if (window != null && now >= window.end && !window.ended) {
                windows.put(body.id(), new Window(window.start, window.end, window.nextEligible, true));
                setDirty();
                broadcast(server, body, body.name() + " has left the " + systemName(body) + " system.",
                        ChatFormatting.GRAY);
                continue;
            }
            boolean open = window != null && now >= window.start && now < window.end;
            boolean ready = window == null || now >= window.nextEligible;
            if (open || !ready) continue;

            double perMinute = 1.0 - Math.pow(1.0 - Math.max(0.0f, Math.min(1.0f, info.chance())), 0.1);
            if (random.nextDouble() >= perMinute) continue;

            long end = now + info.durationMinutes() * 60_000L;
            windows.put(body.id(), new Window(now, end, end + info.cooldownMinutes() * 60_000L, false));
            setDirty();
            announceArrival(server, body, end - now);
        }
    }

    private static String systemName(ContinuumBody body) {
        var system = ContinuumData.system(body.system());
        return system != null ? system.name() : body.system().getPath();
    }

    private void announceArrival(MinecraftServer server, ContinuumBody body, long millis) {
        broadcast(server, body, "A comet, " + body.name() + ", is passing through " + systemName(body) +
                ". It will be gone in " + ContinuumMissions.durationText(millis) +
                ": send an extraction run before it leaves.", ChatFormatting.GOLD);
    }

    private static void broadcast(MinecraftServer server, ContinuumBody body, String text, ChatFormatting colour) {
        ContinuumTeamData teams = ContinuumTeamData.get(server);
        Set<UUID> logged = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID team = TeamUtils.getTeamIdOrPlayerFallback(player.getUUID());
            
            if (!teams.isEngaged(team)) continue;
            player.sendSystemMessage(Component.literal(text).withStyle(colour));
            if (logged.add(team)) teams.log(team, MissionLog.Kind.COMET, body.id(), text);
            ContinuumServerEvents.sendState(player);
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        windows.forEach((id, w) -> {
            CompoundTag e = new CompoundTag();
            e.putString("Body", id.toString());
            e.putLong("Start", w.start);
            e.putLong("End", w.end);
            e.putLong("Next", w.nextEligible);
            e.putBoolean("Ended", w.ended);
            list.add(e);
        });
        tag.put("Windows", list);
        return tag;
    }

    public static ContinuumTransients load(CompoundTag tag) {
        ContinuumTransients transients = new ContinuumTransients();
        for (Tag t : tag.getList("Windows", Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) t;
            transients.windows.put(ResourceLocation.parse(e.getString("Body")),
                    new Window(e.getLong("Start"), e.getLong("End"), e.getLong("Next"), e.getBoolean("Ended")));
        }
        return transients;
    }
}
