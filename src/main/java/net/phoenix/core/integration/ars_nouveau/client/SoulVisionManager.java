package net.phoenix.core.integration.ars_nouveau.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.client.worldfx.IWorldFXEmitter;
import net.phoenix.core.client.worldfx.PhoenixScreenEffect;
import net.phoenix.core.client.worldfx.PhoenixSkyLayer;
import net.phoenix.core.client.worldfx.WorldFXManager;
import net.phoenix.core.integration.ars_nouveau.common.data.item.SoulLensItem;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.client.gui.GuiGraphics;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SoulVisionManager {

    private static final int GRID_RADIUS = SoulLensItem.VISION_RADIUS;
    private static final int GRID_SIZE = GRID_RADIUS * 2 + 1;
    
    private static final float DENSITY_SCALE = 2.5f;
    
    private static final float DENSITY_CURVE = 0.6f;
    
    private static final float SURFACE_OFFSET = 0.5f;
    
    private static final boolean DEBUG_SOLID_RED = true;

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
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !active) return;

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
        CompoundTag tag = lens.getTag();
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

        if (!debugPrinted) {
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

        String line1 = "[SoulVision] player chunk=(" + playerChunkX + "," + playerChunkZ
                + ") CenterChunk=(" + centerChunkX + "," + centerChunkZ + ") rel=(" + relX + "," + relZ + ")";

        String line2;
        if (px < 0 || px >= GRID_SIZE || pz < 0 || pz >= GRID_SIZE) {
            line2 = "[SoulVision] player's chunk is OUTSIDE the " + GRID_SIZE + "x" + GRID_SIZE + " grid";
        } else {
            float density = densityGrid[px][pz];
            float factor = (float) Math.pow(Math.min(density / DENSITY_SCALE, 1.0f), DENSITY_CURVE);
            boolean chunkLoaded = Minecraft.getInstance().level != null
                    && Minecraft.getInstance().level.hasChunk(playerChunkX, playerChunkZ);
            line2 = "[SoulVision] densityGrid value here=" + density + " -> factor=" + factor
                    + " (DENSITY_SCALE=" + DENSITY_SCALE + ", DENSITY_CURVE=" + DENSITY_CURVE
                    + ") level.hasChunk=" + chunkLoaded;
        }

        player.displayClientMessage(Component.literal(line1), false);
        player.displayClientMessage(Component.literal(line2), false);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (!active || !hasGridData) return;

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {

            if (!DEBUG_SOLID_RED) {
                WorldFXManager.applyScreenEffects(event.getPartialTick());
            }
            return;
        }

        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        PoseStack stack = event.getPoseStack();
        Vec3 camPos = event.getCamera().getPosition();

        mc.getMainRenderTarget().bindWrite(true);

        if (!DEBUG_SOLID_RED) {
            GlStateManager._colorMask(false, false, false, true);
        }

        if (!DEBUG_SOLID_RED) {
            RenderSystem.clearColor(0f, 0f, 0f, 0f);
            RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT, Minecraft.ON_OSX);
        }

        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.disableBlend();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        int playerChunkX = mc.player != null ? mc.player.blockPosition().getX() >> 4 : Integer.MIN_VALUE;
        int playerChunkZ = mc.player != null ? mc.player.blockPosition().getZ() >> 4 : Integer.MIN_VALUE;
        int quadsDrawn = 0;

        Matrix4f pose = stack.last().pose();
        for (int px = 0; px < GRID_SIZE; px++) {
            for (int pz = 0; pz < GRID_SIZE; pz++) {
                int chunkX = centerChunkX + (px - GRID_RADIUS);
                int chunkZ = centerChunkZ + (pz - GRID_RADIUS);
                if (!level.hasChunk(chunkX, chunkZ)) continue;

                if (!debugGeometryChecked && chunkX == playerChunkX && chunkZ == playerChunkZ) {
                    printGeometrySnapshot(level, chunkX, chunkZ, camPos);
                }

                drawChunkAlphaQuad(pose, buffer, camPos, level, chunkX, chunkZ, densityGrid[px][pz]);
                quadsDrawn++;
            }
        }
        debugGeometryChecked = true;

        tesselator.end();

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.enableBlend();
        if (!DEBUG_SOLID_RED) {
            GlStateManager._colorMask(true, true, true, true);
        }

        if (!debugGlErrorChecked) {
            debugGlErrorChecked = true;
            int glError = GL11.glGetError();
            Player debugPlayer = mc.player;
            if (debugPlayer != null) {
                debugPlayer.displayClientMessage(Component.literal(
                        "[SoulVision] glGetError after mask draw: " + glError
                                + (glError == GL11.GL_NO_ERROR ? " (no error)" : " (see GL11 constants)")), false);
                debugPlayer.displayClientMessage(Component.literal(
                        "[SoulVision] quads drawn this frame: " + quadsDrawn), false);
            }
        }
    }

    private static void printGeometrySnapshot(ClientLevel level, int chunkX, int chunkZ, Vec3 camPos) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        int x0 = chunkX << 4;
        int x1 = x0 + 16;
        int z0 = chunkZ << 4;
        int z1 = z0 + 16;

        int y00 = level.getHeight(Heightmap.Types.WORLD_SURFACE, x0, z0);
        int y10 = level.getHeight(Heightmap.Types.WORLD_SURFACE, x1, z0);
        int y01 = level.getHeight(Heightmap.Types.WORLD_SURFACE, x0, z1);
        int y11 = level.getHeight(Heightmap.Types.WORLD_SURFACE, x1, z1);

        player.displayClientMessage(Component.literal(
                "[SoulVision] quad corner heights (WORLD_SURFACE) for your chunk: "
                        + y00 + "," + y10 + "," + y01 + "," + y11
                        + " | your actual Y=" + player.getY() + " | camera Y=" + camPos.y), false);
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (!active) return;
        GuiGraphics graphics = event.getGuiGraphics();
        graphics.fill(20, 20, 220, 220, 0xFFFF0000);
    }

    private static void drawChunkAlphaQuad(Matrix4f pose, VertexConsumer buffer, Vec3 camPos, ClientLevel level,
                                           int chunkX, int chunkZ, float density) {
        int x0 = chunkX << 4;
        int x1 = x0 + 16;
        int z0 = chunkZ << 4;
        int z1 = z0 + 16;

        float y00 = level.getHeight(Heightmap.Types.WORLD_SURFACE, x0, z0) + SURFACE_OFFSET;
        float y10 = level.getHeight(Heightmap.Types.WORLD_SURFACE, x1, z0) + SURFACE_OFFSET;
        float y01 = level.getHeight(Heightmap.Types.WORLD_SURFACE, x0, z1) + SURFACE_OFFSET;
        float y11 = level.getHeight(Heightmap.Types.WORLD_SURFACE, x1, z1) + SURFACE_OFFSET;

        float factor = (float) Math.pow(Math.min(density / DENSITY_SCALE, 1.0f), DENSITY_CURVE);

        float rx0 = (float) (x0 - camPos.x);
        float rx1 = (float) (x1 - camPos.x);
        float rz0 = (float) (z0 - camPos.z);
        float rz1 = (float) (z1 - camPos.z);

        if (DEBUG_SOLID_RED) {
            buffer.vertex(pose, rx0, y00 - (float) camPos.y, rz0).color(1f, 0f, 0f, 1f).endVertex();
            buffer.vertex(pose, rx0, y01 - (float) camPos.y, rz1).color(1f, 0f, 0f, 1f).endVertex();
            buffer.vertex(pose, rx1, y11 - (float) camPos.y, rz1).color(1f, 0f, 0f, 1f).endVertex();
            buffer.vertex(pose, rx1, y10 - (float) camPos.y, rz0).color(1f, 0f, 0f, 1f).endVertex();
            return;
        }

        buffer.vertex(pose, rx0, y00 - (float) camPos.y, rz0).color(0f, 0f, 0f, factor).endVertex();
        buffer.vertex(pose, rx0, y01 - (float) camPos.y, rz1).color(0f, 0f, 0f, factor).endVertex();
        buffer.vertex(pose, rx1, y11 - (float) camPos.y, rz1).color(0f, 0f, 0f, factor).endVertex();
        buffer.vertex(pose, rx1, y10 - (float) camPos.y, rz0).color(0f, 0f, 0f, factor).endVertex();
    }
}