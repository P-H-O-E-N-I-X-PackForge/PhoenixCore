package net.phoenix.core.integration.ars_nouveau.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.ars_nouveau.common.data.item.SoulLensItem;

import com.mojang.blaze3d.platform.NativeImage;

@Mod.EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SoulVisionManager {

    public static final ResourceLocation SOUL_VISION_SHADER = new ResourceLocation(PhoenixCore.MOD_ID,
            "shaders/post/soul_vision.json");
    public static final String SOUL_VISION_EFFECT_NAME = "phoenixcore:soul_vision";

    private static final int GRID_RADIUS = SoulLensItem.VISION_RADIUS;
    private static final int GRID_SIZE = GRID_RADIUS * 2 + 1;
    private static final float DENSITY_SCALE = 2.5f;
    private static final ResourceLocation DENSITY_TEXTURE_ID = new ResourceLocation(PhoenixCore.MOD_ID,
            "dynamic/soul_vision_density");

    private static boolean active = false;
    private static DynamicTexture densityTexture;
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
        Minecraft.getInstance().gameRenderer.loadEffect(SOUL_VISION_SHADER);
        player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.35f, 1.6f, false);
    }

    private static void deactivate() {
        if (!active) return;
        active = false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameRenderer.currentEffect() != null) {
            mc.gameRenderer.shutdownEffect();
        }

        Player player = mc.player;
        if (player != null) {
            player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.35f, 1.6f, false);
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean hasDensityData() {
        return active && densityTexture != null;
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

        updateDensityTexture(lens);
    }

    private static ItemStack findHeldLens(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof SoulLensItem) return main;
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof SoulLensItem) return off;
        return null;
    }

    private static void updateDensityTexture(ItemStack lens) {
        CompoundTag tag = lens.getTag();
        if (tag == null || !tag.contains("MapData")) return;

        ensureTexture();
        NativeImage image = densityTexture.getPixels();
        if (image == null) return;

        for (int x = 0; x < GRID_SIZE; x++) {
            for (int z = 0; z < GRID_SIZE; z++) {
                image.setPixelRGBA(x, z, 0xFF000000);
            }
        }

        ListTag mapData = tag.getList("MapData", Tag.TAG_COMPOUND);
        for (int i = 0; i < mapData.size(); i++) {
            CompoundTag chunk = mapData.getCompound(i);
            int px = chunk.getInt("relX") + GRID_RADIUS;
            int pz = chunk.getInt("relZ") + GRID_RADIUS;
            if (px < 0 || px >= GRID_SIZE || pz < 0 || pz >= GRID_SIZE) continue;

            float density = chunk.getFloat("density");
            int level = (int) (Math.min(density / DENSITY_SCALE, 1f) * 255f);

            int color = 0xFF000000 | (level << 16) | (level << 8) | level;
            image.setPixelRGBA(px, pz, color);
        }

        densityTexture.upload();

        centerChunkX = tag.getInt("CenterChunkX");
        centerChunkZ = tag.getInt("CenterChunkZ");
    }

    private static void ensureTexture() {
        if (densityTexture != null) return;
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, GRID_SIZE, GRID_SIZE, false);
        densityTexture = new DynamicTexture(image);
        densityTexture.setFilter(false, false);
        Minecraft.getInstance().getTextureManager().register(DENSITY_TEXTURE_ID, densityTexture);
    }

    public static float getCenterChunkX() {
        return centerChunkX;
    }

    public static float getCenterChunkZ() {
        return centerChunkZ;
    }

    public static float getGridRadius() {
        return GRID_RADIUS;
    }

    public static int getDensityTextureId() {
        return densityTexture != null ? densityTexture.getId() : -1;
    }
}
