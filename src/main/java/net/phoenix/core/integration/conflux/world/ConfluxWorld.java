package net.phoenix.core.integration.conflux.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.api.ConfluxApi;
import net.phoenix.core.api.event.ConfluxWorldEvent;
import net.phoenix.core.integration.chronicles.ChroniclesLink;
import net.phoenix.core.integration.conflux.research.ResearchTeamHelper;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class ConfluxWorld {

    public static final String DISCIPLINE_ENTERED = "discipline_entered";
    public static final String DISCIPLINE_LEFT = "discipline_left";
    public static final String REGION_ENTERED = "region_entered";
    public static final String VOID_EDGE = "void_edge";
    public static final String FAMILIARITY_UP = "familiarity_up";
    public static final String LANDFORM_DISCOVERED = "landform_discovered";

    private static final int MAX_CHAIN = 3;
    private static final Map<String, Long> COOLDOWNS = new HashMap<>();
    private static int depth = 0;

    private ConfluxWorld() {}

    public static String chroniclesTrigger(String type) {
        return "phoenixcore:conflux/world/" + type;
    }

    public static void fire(ServerPlayer player, String type, Map<String, Object> data) {
        if (depth >= MAX_CHAIN) return;
        depth++;
        try {
            NeoForge.EVENT_BUS.post(new ConfluxWorldEvent(player, type, data));

            if (ChroniclesLink.present()) {
                ChroniclesLink.fire(player, chroniclesTrigger(type), toTag(data));
            }

            HookRule.Context ctx = context(player, type, data);
            for (HookRule rule : HookRules.INSTANCE.all()) {
                if (!rule.listensTo(type) || !rule.matches(ctx)) continue;
                if (!allowed(player, rule)) continue;
                run(player, rule, type, data);
            }
        } catch (RuntimeException e) {
            PhoenixCore.LOGGER.error("[Conflux] World hook '{}' failed: {}", type, e.toString());
        } finally {
            depth--;
        }
    }

    public static Map<String, Object> data(Object... keysAndValues) {
        Map<String, Object> data = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keysAndValues.length; i += 2)
            data.put(String.valueOf(keysAndValues[i]), keysAndValues[i + 1]);
        return data;
    }

    private static HookRule.Context context(ServerPlayer player, String type, Map<String, Object> data) {
        String dimension = player.level().dimension().location().toString();
        String path = player.level().dimension().location().getPath();
        String discipline = path.startsWith("conflux/") ? path.substring("conflux/".length()) : "";
        return new HookRule.Context(type, dimension, discipline, player.getY(), data);
    }

    private static boolean allowed(ServerPlayer player, HookRule rule) {
        long now = player.level().getGameTime();
        if (rule.cooldownTicks() > 0) {
            String key = rule.id() + "|" + player.getUUID();
            Long last = COOLDOWNS.get(key);
            if (last != null && now - last < rule.cooldownTicks()) return false;
        }
        if (rule.once() != HookRule.Once.NEVER) {
            HookState state = HookState.get(player.server);
            if (state.has(onceKey(player, rule))) return false;
        }
        return true;
    }

    private static String onceKey(ServerPlayer player, HookRule rule) {
        UUID who = rule.once() == HookRule.Once.TEAM ? ResearchTeamHelper.getTeamId(player) : player.getUUID();
        return rule.id() + "|" + rule.once() + "|" + who;
    }

    private static void run(ServerPlayer player, HookRule rule, String type, Map<String, Object> data) {
        if (rule.cooldownTicks() > 0) {
            COOLDOWNS.put(rule.id() + "|" + player.getUUID(), player.level().getGameTime());
        }
        if (rule.once() != HookRule.Once.NEVER) HookState.get(player.server).mark(onceKey(player, rule));

        for (HookRule.Action action : rule.actions()) {
            String value = fill(action.value(), player, data);
            switch (action.type()) {
                case "flag" -> ConfluxApi.grantFlag(player, value);
                case "unlock" -> {
                    ResourceLocation node = ResourceLocation.tryParse(value);
                    if (node != null) ConfluxApi.grantUnlock(player, node);
                    else PhoenixCore.LOGGER.warn("[Conflux] Hook {}: bad node '{}'", rule.id(), value);
                }
                case "archive" -> {
                    var unlocker = ConfluxApi.archiveUnlocker;
                    if (unlocker != null) unlocker.accept(player, value);
                }
                case "chronicles" -> {
                    CompoundTag tag = toTag(data);
                    action.data().forEach((k, v) -> tag.putString(k, fill(v, player, data)));
                    ChroniclesLink.fire(player, value, tag);
                }
                case "command" -> {
                    var source = "server".equals(action.as()) ? player.server.createCommandSourceStack() :
                            player.createCommandSourceStack().withPermission(2);
                    player.server.getCommands().performPrefixedCommand(source.withSuppressedOutput(), value);
                }
                case "message" -> player.sendSystemMessage(Component.literal(value));
                case "event" -> fire(player, value, new LinkedHashMap<>(data));
                default -> PhoenixCore.LOGGER.warn("[Conflux] Hook {}: unknown action type '{}'", rule.id(),
                        action.type());
            }
        }
    }

    static String fill(String text, ServerPlayer player, Map<String, Object> data) {
        String out = text.replace("{player}", player.getGameProfile().getName())
                .replace("{x}", String.valueOf(player.getBlockX()))
                .replace("{y}", String.valueOf(player.getBlockY()))
                .replace("{z}", String.valueOf(player.getBlockZ()));
        for (Map.Entry<String, Object> e : data.entrySet())
            out = out.replace("{" + e.getKey() + "}", String.valueOf(e.getValue()));
        return out;
    }

    private static CompoundTag toTag(Map<String, Object> data) {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<String, Object> e : data.entrySet()) {
            Object v = e.getValue();
            if (v instanceof Integer i) tag.putInt(e.getKey(), i);
            else if (v instanceof Long l) tag.putLong(e.getKey(), l);
            else if (v instanceof Number n) tag.putDouble(e.getKey(), n.doubleValue());
            else tag.putString(e.getKey(), String.valueOf(v));
        }
        return tag;
    }
}
