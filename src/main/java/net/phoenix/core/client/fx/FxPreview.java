package net.phoenix.core.client.fx;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.data.RotationState;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.phoenix.core.PhoenixCore;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@EventBusSubscriber(modid = PhoenixCore.MOD_ID, value = Dist.CLIENT)
public final class FxPreview {

    private record Entry(BlockState old, BlockState shown, long until) {}

    private static final Map<BlockPos, Entry> ACTIVE = new HashMap<>();

    private FxPreview() {}

    public static boolean isPreview(BlockPos pos) {
        return !ACTIVE.isEmpty() && ACTIVE.containsKey(pos);
    }

    public static String spawn(FxRender fx, int seconds) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (mc.player == null || level == null) return "not in a world";
        if (fx.spawner() != null) return fx.spawner().apply(seconds);
        String id = fx.machineId();
        if (id == null) return fx.id() + " has no machine of its own to put down";
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null || !BuiltInRegistries.BLOCK.containsKey(rl)) return "there is no block " + id;
        Block block = BuiltInRegistries.BLOCK.get(rl);
        BlockPos pos = mc.player.blockPosition().below();
        if (level.getBlockEntity(pos) != null && !ACTIVE.containsKey(pos)) {
            return "there is a block with its own data under you; step somewhere else";
        }

        BlockState old = ACTIVE.containsKey(pos) ? ACTIVE.get(pos).old() : level.getBlockState(pos);
        BlockState state = block.defaultBlockState();
        if (block instanceof MetaMachineBlock machine) {
            RotationState rotation = machine.getRotationState();
            if (rotation != RotationState.NONE) {

                Direction face = mc.player.getDirection().getOpposite();
                state = state.setValue(rotation.property, rotation.test(face) ? face : rotation.defaultDirection);
            }
        }
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        ACTIVE.put(pos, new Entry(old, state, System.currentTimeMillis() + seconds * 1000L));
        return fx.title() + " put down for " + seconds + " seconds. /pftfx clear takes it away.";
    }

    public static int clear() {
        net.phoenix.core.client.renderer.phoenix.PhoenixForgeWorldRenderer.clearPreview();
        Level level = Minecraft.getInstance().level;
        int n = ACTIVE.size();
        if (level != null) {
            for (var e : ACTIVE.entrySet()) restore(level, e.getKey(), e.getValue());
        }
        ACTIVE.clear();
        return n;
    }

    private static void restore(Level level, BlockPos pos, Entry entry) {
        if (level.getBlockState(pos) == entry.shown()) level.setBlock(pos, entry.old(), Block.UPDATE_CLIENTS);
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (ACTIVE.isEmpty()) return;
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            ACTIVE.clear();
            return;
        }
        long now = System.currentTimeMillis();
        for (Iterator<Map.Entry<BlockPos, Entry>> it = ACTIVE.entrySet().iterator(); it.hasNext();) {
            var e = it.next();
            if (now >= e.getValue().until() || level.getBlockState(e.getKey()) != e.getValue().shown()) {
                restore(level, e.getKey(), e.getValue());
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onLeave(ClientPlayerNetworkEvent.LoggingOut event) {
        ACTIVE.clear();
    }
}
