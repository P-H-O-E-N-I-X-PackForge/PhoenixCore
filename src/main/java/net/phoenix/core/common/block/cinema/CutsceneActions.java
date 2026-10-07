package net.phoenix.core.common.block.cinema;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.PhoenixCore;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Server-side consequences for cutscene choices, loaded from {@code data/<namespace>/cutscene_actions/<path>.json}.
 * Cutscenes (client assets) only refer to these by id, so a modified client can never run anything that isn't
 * defined here.
 *
 * <pre>
 * {
 *   "cutscene": "phoenixcore:lore/03_ash",     // only this cutscene may trigger it (recommended)
 *   "once": true,                              // at most once per player, ever (default true)
 *   "exclusive_group": "ash_notebook",         // only one action per group per player, ever (optional)
 *   "permission_level": 2,                     // level the commands run at (default 2)
 *   "commands": ["give @s minecraft:book", "advancement grant @s only phoenixcore:lore/ash"]
 * }
 * </pre>
 *
 * Commands run as the player ({@code @s}, their position), with output hidden. A player can only trigger an action
 * while inside a cutscene the server started for them, and only if it belongs to that cutscene.
 */
@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID)
public final class CutsceneActions extends SimpleJsonResourceReloadListener {

    public static final CutsceneActions INSTANCE = new CutsceneActions();

    private static final String USED_TAG = "phoenixcore_cutscene_actions";
    private static final String GROUPS_TAG = "phoenixcore_cutscene_groups";

    public record Action(Optional<ResourceLocation> cutscene, boolean once, Optional<String> exclusiveGroup,
                         int permissionLevel, List<String> commands) {

        public static final Codec<Action> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.optionalFieldOf("cutscene").forGetter(Action::cutscene),
                Codec.BOOL.optionalFieldOf("once", true).forGetter(Action::once),
                Codec.STRING.optionalFieldOf("exclusive_group").forGetter(Action::exclusiveGroup),
                Codec.intRange(0, 4).optionalFieldOf("permission_level", 2).forGetter(Action::permissionLevel),
                Codec.STRING.listOf().optionalFieldOf("commands", List.of()).forGetter(Action::commands))
                .apply(i, Action::new));
    }

    private Map<ResourceLocation, Action> actions = Map.of();

    private CutsceneActions() {
        super(new Gson(), "cutscene_actions");
    }

    @SubscribeEvent
    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        Map<ResourceLocation, Action> loaded = new HashMap<>();
        jsons.forEach((id, json) -> Action.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> PhoenixCore.LOGGER.error("Failed to load cutscene action {}: {}", id, error))
                .ifPresent(action -> loaded.put(id, action)));
        actions = Map.copyOf(loaded);
        PhoenixCore.LOGGER.info("Loaded {} cutscene action(s)", actions.size());
    }

    /** Runs an action a client asked for, if the player is allowed to trigger it right now. */
    public static void trigger(ServerPlayer player, ResourceLocation cutsceneId, ResourceLocation actionId) {
        Action action = INSTANCE.actions.get(actionId);
        if (action == null) {
            PhoenixCore.LOGGER.warn("Cutscene {} asked for unknown action {}", cutsceneId, actionId);
            return;
        }
        ResourceLocation session = Cutscenes.activeCutscene(player);
        if (session == null || !session.equals(cutsceneId) ||
                (action.cutscene().isPresent() && !action.cutscene().get().equals(cutsceneId))) {
            PhoenixCore.LOGGER.warn("Rejected cutscene action {} from {} (cutscene {}, session {})",
                    actionId, player.getGameProfile().getName(), cutsceneId, session);
            return;
        }

        CompoundTag data = persisted(player);
        ListTag used = data.getList(USED_TAG, Tag.TAG_STRING);
        ListTag groups = data.getList(GROUPS_TAG, Tag.TAG_STRING);
        if (action.once() && used.contains(StringTag.valueOf(actionId.toString()))) return;
        if (action.exclusiveGroup().isPresent() &&
                groups.contains(StringTag.valueOf(action.exclusiveGroup().get())))
            return;

        used.add(StringTag.valueOf(actionId.toString()));
        data.put(USED_TAG, used);
        action.exclusiveGroup().ifPresent(group -> {
            groups.add(StringTag.valueOf(group));
            data.put(GROUPS_TAG, groups);
        });

        var source = player.createCommandSourceStack().withPermission(action.permissionLevel()).withSuppressedOutput();
        for (String command : action.commands()) {
            player.server.getCommands().performPrefixedCommand(source, command);
        }
    }

    /** Forgets every action and group this player has used, e.g. for testing. Returns how many were cleared. */
    public static int reset(ServerPlayer player) {
        CompoundTag data = persisted(player);
        int cleared = data.getList(USED_TAG, Tag.TAG_STRING).size();
        data.remove(USED_TAG);
        data.remove(GROUPS_TAG);
        return cleared;
    }

    // Stored under Forge's PlayerPersisted tag so it survives death.
    private static CompoundTag persisted(ServerPlayer player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return root.getCompound(Player.PERSISTED_NBT_TAG);
    }
}
