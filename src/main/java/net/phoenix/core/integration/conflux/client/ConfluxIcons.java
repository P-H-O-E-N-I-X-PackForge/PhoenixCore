package net.phoenix.core.integration.conflux.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class ConfluxIcons {

    public static final String FLUID = "fluid:";
    public static final String TEXTURE = "texture:";

    private static final Map<ResourceLocation, int[]> SIZES = new HashMap<>();

    private ConfluxIcons() {}

    public static void draw(GuiGraphics g, String icon, int x, int y) {
        if (icon == null || icon.isEmpty()) return;
        try {
            if (icon.startsWith(FLUID)) {
                drawFluid(g, icon.substring(FLUID.length()), x, y);
            } else if (icon.startsWith(TEXTURE)) {
                drawTexture(g, icon.substring(TEXTURE.length()), x, y);
            } else {
                var item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(icon)).orElse(Items.BOOK);
                g.renderItem(new ItemStack(item), x, y);
            }
        } catch (RuntimeException ignored) {

        }
    }

    public static ItemStack asItem(String icon) {
        if (icon == null || icon.startsWith(FLUID) || icon.startsWith(TEXTURE)) return new ItemStack(Items.BOOK);
        var item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(icon)).orElse(Items.BOOK);
        return new ItemStack(item);
    }

    private static void drawFluid(GuiGraphics g, String id, int x, int y) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return;
        Fluid fluid = BuiltInRegistries.FLUID.getOptional(rl).orElse(Fluids.EMPTY);
        if (fluid == Fluids.EMPTY) return;
        var extensions = IClientFluidTypeExtensions.of(fluid);
        ResourceLocation still = extensions.getStillTexture();
        if (still == null) return;
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(still);
        int tint = extensions.getTintColor();
        float a = ((tint >> 24) & 0xFF) / 255f, r = ((tint >> 16) & 0xFF) / 255f, gr = ((tint >> 8) & 0xFF) / 255f,
                b = (tint & 0xFF) / 255f;
        g.blit(x, y, 0, 16, 16, sprite, r, gr, b, a == 0 ? 1f : a);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private static void drawTexture(GuiGraphics g, String id, int x, int y) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return;
        int[] size = sizeOf(rl);
        if (size[0] <= 0) return;
        g.blit(rl, x, y, 16, 16, 0f, 0f, size[0], size[1], size[0], size[1]);
    }

    private static int[] sizeOf(ResourceLocation rl) {
        return SIZES.computeIfAbsent(rl, k -> {
            var resource = Minecraft.getInstance().getResourceManager().getResource(k);
            if (resource.isEmpty()) return new int[] { 0, 0 };
            try (var in = resource.get().open(); NativeImage image = NativeImage.read(in)) {
                return new int[] { image.getWidth(), image.getHeight() };
            } catch (Exception e) {
                return new int[] { 0, 0 };
            }
        });
    }

    public static String label(String icon) {
        if (icon == null) return "";
        if (icon.startsWith(FLUID)) return "fluid " + icon.substring(FLUID.length());
        if (icon.startsWith(TEXTURE)) return "picture " + icon.substring(TEXTURE.length());
        return icon;
    }
}
