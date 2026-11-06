package net.phoenix.core.integration.ars_nouveau.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.client.worldfx.IWorldFXEmitter;
import net.phoenix.core.client.worldfx.PhoenixScreenEffect;
import net.phoenix.core.client.worldfx.PhoenixSkyLayer;
import net.phoenix.core.client.worldfx.WorldFXManager;
import net.phoenix.core.integration.ars_nouveau.common.data.item.SoulLensItem;
import net.phoenix.core.util.StackNbt;

import org.joml.Matrix4f;

@EventBusSubscriber(modid = PhoenixCore.MOD_ID, value = Dist.CLIENT)
public final class SoulVisionManager {

    private static final int GRID_RADIUS = SoulLensItem.VISION_RADIUS;
    private static final int GRID_SIZE = GRID_RADIUS * 2 + 1;

    private static final float DENSITY_SCALE = 2.5f;

    private static final float DENSITY_CURVE = 0.6f;

    private static final boolean DEBUG_LOG = false;

    private static final BlockPos EFFECT_KEY = new BlockPos(0, 20_000_000, 0);

    private static boolean active = false;
    private static boolean debugPrinted = false;
    private static boolean debugGlErrorChecked = false;
    private static boolean debugGeometryChecked = false;
    private static final float[][] densityGrid = new float[GRID_SIZE][GRID_SIZE];
    private static boolean hasGridData = false;
    private static int centerChunkX;
    private static int centerChunkZ;

    private SoulVisionManager() {}

    public static void toggle(Player player) {
        if (active) {
            deactivate();
        } else {
            activate(player);
        }
    }

    private static void activate(Player player) {
        active = true;
        debugPrinted = false;
        debugGlErrorChecked = false;
        debugGeometryChecked = false;

        WorldFXManager.register(EFFECT_KEY, new IWorldFXEmitter() {

            @Override
            public PhoenixSkyLayer createSkyLayer() {
                return null;
            }

            @Override
            public PhoenixScreenEffect createScreenEffect() {
                return new SoulVisionScreenEffect();
            }

            @Override
            public float getEffectRadius() {
                return -1f;
            }
        });

        player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.35f, 1.6f, false);
    }

    private static void deactivate() {
        if (!active) return;
        active = false;
        hasGridData = false;

        WorldFXManager.unregister(EFFECT_KEY);

        Player player = Minecraft.getInstance().player;
        if (player != null) {
            player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.35f, 1.6f, false);
        }
    }

    public static boolean isActive() {
        return active;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!active) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) {
            deactivate();
            return;
        }

        ItemStack lens = findHeldLens(player);
        if (lens == null) {
            deactivate();
            return;
        }

        updateDensityGrid(lens);
    }

    private static ItemStack findHeldLens(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof SoulLensItem) return main;
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof SoulLensItem) return off;
        return null;
    }

    private static void updateDensityGrid(ItemStack lens) {
        CompoundTag tag = StackNbt.getTag(lens);
        if (tag == null || !tag.contains("MapData")) return;

        ListTag mapData = tag.getList("MapData", Tag.TAG_COMPOUND);
        for (int i = 0; i < mapData.size(); i++) {
            CompoundTag chunk = mapData.getCompound(i);
            int px = chunk.getInt("relX") + GRID_RADIUS;
            int pz = chunk.getInt("relZ") + GRID_RADIUS;
            if (px < 0 || px >= GRID_SIZE || pz < 0 || pz >= GRID_SIZE) continue;
            densityGrid[px][pz] = chunk.getFloat("density");
        }

        centerChunkX = tag.getInt("CenterChunkX");
        centerChunkZ = tag.getInt("CenterChunkZ");
        hasGridData = true;

        if (DEBUG_LOG && !debugPrinted) {
            debugPrinted = true;
            printDebugSnapshot();
        }
    }

    private static void printDebugSnapshot() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        int playerChunkX = player.blockPosition().getX() >> 4;
        int playerChunkZ = player.blockPosition().getZ() >> 4;
        int relX = playerChunkX - centerChunkX;
        int relZ = playerChunkZ - centerChunkZ;
        int px = relX + GRID_RADIUS;
        int pz = relZ + GRID_RADIUS;

        String line1 = "[SoulVision] player chunk=(" + playerChunkX + "," + playerChunkZ + ") CenterChunk=(" +
                centerChunkX + "," + centerChunkZ + ") rel=(" + relX + "," + relZ + ")";

        String line2;
        if (px < 0 || px >= GRID_SIZE || pz < 0 || pz >= GRID_SIZE) {
            line2 = "[SoulVision] player's chunk is OUTSIDE the " + GRID_SIZE + "x" + GRID_SIZE + " grid";
        } else {
            float density = densityGrid[px][pz];
            float factor = (float) Math.pow(Math.min(density / DENSITY_SCALE, 1.0f), DENSITY_CURVE);
            boolean chunkLoaded = Minecraft.getInstance().level != null &&
                    Minecraft.getInstance().level.hasChunk(playerChunkX, playerChunkZ);
            line2 = "[SoulVision] densityGrid value here=" + density + " -> factor=" + factor + " (DENSITY_SCALE=" +
                    DENSITY_SCALE + ", DENSITY_CURVE=" + DENSITY_CURVE + ") level.hasChunk=" + chunkLoaded;
        }

        player.displayClientMessage(Component.literal(line1), false);
        player.displayClientMessage(Component.literal(line2), false);
    }

    private static final Matrix4f inverseViewProjection = new Matrix4f();
    private static Vec3 cameraPos = Vec3.ZERO;
    private static final float[] gridFactors = new float[GRID_SIZE * GRID_SIZE];

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (!active || !hasGridData) return;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;

        new Matrix4f(event.getProjectionMatrix()).mul(event.getModelViewMatrix()).invert(inverseViewProjection);
        cameraPos = event.getCamera().getPosition();

        WorldFXManager.applyScreenEffects(event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }

    static void uploadUniforms(ShaderInstance shader) {
        shader.setSampler("DepthSampler", Minecraft.getInstance().getMainRenderTarget().getDepthTextureId());
        shader.safeGetUniform("InvViewProj").set(inverseViewProjection);
        shader.safeGetUniform("CamPos").set((float) cameraPos.x, (float) cameraPos.y, (float) cameraPos.z);
        shader.safeGetUniform("GridOrigin").set((float) (centerChunkX - GRID_RADIUS),
                (float) (centerChunkZ - GRID_RADIUS));

        for (int pz = 0; pz < GRID_SIZE; pz++) {
            for (int px = 0; px < GRID_SIZE; px++) {
                float density = densityGrid[px][pz];
                gridFactors[pz * GRID_SIZE + px] = (float) Math.pow(Math.min(density / DENSITY_SCALE, 1.0f),
                        DENSITY_CURVE);
            }
        }
        shader.safeGetUniform("Density").set(gridFactors);
    }
}
