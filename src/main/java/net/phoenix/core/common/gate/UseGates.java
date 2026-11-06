package net.phoenix.core.common.gate;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.phoenix.core.PhoenixCore;
import net.phoenixvine.wiki.api.gate.SuiteGates;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = PhoenixCore.MOD_ID)
public final class UseGates extends SimpleJsonResourceReloadListener {

    public static final UseGates INSTANCE = new UseGates();

    private enum Action {
        USE,
        INTERACT,
        BREAK
    }

    private record Matcher(ResourceLocation id, boolean tag) {}

    private record Rule(String gate, List<Matcher> items, List<Matcher> blocks, boolean use, boolean interact,
                        boolean breaking, String message) {}

    private List<Rule> rules = List.of();

    private UseGates() {
        super(new Gson(), "phoenix/use_gates");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        List<Rule> loaded = new ArrayList<>();
        data.forEach((id, json) -> {
            try {
                loaded.add(parse(json.getAsJsonObject()));
            } catch (RuntimeException e) {
                PhoenixCore.LOGGER.error("Failed to load use gate {}: {}", id, e.toString());
            }
        });
        rules = List.copyOf(loaded);
        PhoenixCore.LOGGER.info("Loaded {} use gate(s)", rules.size());
    }

    private static Rule parse(JsonObject json) {
        String gate = json.get("gate").getAsString();
        boolean use = true;
        boolean interact = true;
        boolean breaking = true;
        if (json.has("actions")) {
            List<String> actions = strings(json.getAsJsonArray("actions"));
            use = actions.contains("use");
            interact = actions.contains("interact");
            breaking = actions.contains("break");
        }
        String message = json.has("message") ? json.get("message").getAsString() : null;
        return new Rule(gate, matchers(json, "items"), matchers(json, "blocks"), use, interact, breaking, message);
    }

    private static List<String> strings(JsonArray array) {
        List<String> out = new ArrayList<>();
        array.forEach(e -> out.add(e.getAsString()));
        return out;
    }

    private static List<Matcher> matchers(JsonObject json, String key) {
        List<Matcher> out = new ArrayList<>();
        if (!json.has(key)) return out;
        for (String entry : strings(json.getAsJsonArray(key))) {
            boolean tag = entry.startsWith("#");
            out.add(new Matcher(ResourceLocation.parse(tag ? entry.substring(1) : entry), tag));
        }
        return out;
    }

    private static boolean matchesItem(List<Matcher> matchers, ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (Matcher m : matchers) {
            if (m.tag() ? stack.is(TagKey.create(Registries.ITEM, m.id())) :
                    BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(m.id())) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesBlock(List<Matcher> matchers, BlockState state) {
        if (state == null) return false;
        for (Matcher m : matchers) {
            if (m.tag() ? state.is(TagKey.create(Registries.BLOCK, m.id())) :
                    BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(m.id())) {
                return true;
            }
        }
        return false;
    }

    private Component deniedBy(ServerPlayer player, ItemStack stack, BlockState state, Action action) {
        for (Rule rule : rules) {
            boolean applies = switch (action) {
                case USE -> rule.use() && matchesItem(rule.items(), stack);
                case INTERACT -> rule.interact() && matchesBlock(rule.blocks(), state);
                case BREAK -> rule.breaking() &&
                        (matchesItem(rule.items(), stack) || matchesBlock(rule.blocks(), state));
            };
            if (applies && !SuiteGates.isOpen(player, rule.gate())) {
                return rule.message() != null ? Component.literal(rule.message()) : SuiteGates.denialFor(rule.gate());
            }
        }
        return null;
    }

    private static void deny(ServerPlayer player, Component message) {
        player.displayClientMessage(message, true);
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || INSTANCE.rules.isEmpty()) return;
        Component denied = INSTANCE.deniedBy(player, event.getItemStack(), null, Action.USE);
        if (denied == null) return;
        deny(player, denied);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || INSTANCE.rules.isEmpty()) return;
        BlockState state = event.getLevel().getBlockState(event.getPos());
        Component denied = INSTANCE.deniedBy(player, event.getItemStack(), null, Action.USE);
        if (denied == null) denied = INSTANCE.deniedBy(player, ItemStack.EMPTY, state, Action.INTERACT);
        if (denied == null) return;
        deny(player, denied);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || INSTANCE.rules.isEmpty()) return;
        Component denied = INSTANCE.deniedBy(player, player.getMainHandItem(), event.getState(), Action.BREAK);
        if (denied == null) return;
        deny(player, denied);
        event.setCanceled(true);
    }
}
