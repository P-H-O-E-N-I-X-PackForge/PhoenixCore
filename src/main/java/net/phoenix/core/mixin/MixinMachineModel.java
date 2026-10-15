package net.phoenix.core.mixin;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.client.model.GTModelProperties;
import com.gregtechceu.gtceu.client.model.machine.MachineModel;
import com.gregtechceu.gtceu.client.model.quad.StaticFaceBakery;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;
import net.phoenix.core.integration.gregvaults.common.multiblock.VaultInterfacePart;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = MachineModel.class, remap = false)
@OnlyIn(Dist.CLIENT)
public class MixinMachineModel {

    private static final AABB MODE_COLOR_OVERLAY = new AABB(
            -0.010f, -0.010f, -0.010f,
            1.010f, 1.010f, 1.010f);

    private static TextureAtlasSprite pipeOverlaySprite;
    private static TextureAtlasSprite itemInputOverlaySprite;
    private static TextureAtlasSprite itemOutputOverlaySprite;
    private static TextureAtlasSprite pipeInEmissiveSprite;
    private static TextureAtlasSprite pipeOutEmissiveSprite;

    @Inject(
            method = "getMachineQuads",
            at = @At("RETURN"),
            cancellable = true,
            remap = false)
    private void gregvaults$addVaultInterfaceOverlay(
                                                     @Nullable BlockState blockState,
                                                     @Nullable Direction side,
                                                     RandomSource rand,
                                                     ModelData modelData,
                                                     @Nullable RenderType renderType,
                                                     CallbackInfoReturnable<List<BakedQuad>> cir) {
        if (side == null) return;

        BlockAndTintGetter level = modelData.get(GTModelProperties.LEVEL);
        BlockPos pos = modelData.get(GTModelProperties.POS);
        if (level == null || pos == null) return;

        MetaMachine machine = MetaMachine.getMachine(level, pos);
        if (!(machine instanceof VaultInterfacePart part)) return;

        Direction facing = part.getItemFacing();
        if (facing == null || facing != side) return;

        VaultInterfacePart.ItemIoMode mode = part.getItemIoMode();
        if (mode == VaultInterfacePart.ItemIoMode.DISABLED) return;

        loadSprites();

        TextureAtlasSprite hatchSprite = switch (mode) {
            case INPUT -> itemInputOverlaySprite;
            case OUTPUT -> itemOutputOverlaySprite;
            case DISABLED -> null;
        };

        TextureAtlasSprite colorSprite = switch (mode) {
            case INPUT -> pipeInEmissiveSprite;
            case OUTPUT -> pipeOutEmissiveSprite;
            case DISABLED -> null;
        };

        if (hatchSprite == null || colorSprite == null) return;

        List<BakedQuad> quads = new ArrayList<>(cir.getReturnValue());

        quads.add(StaticFaceBakery.bakeFace(
                StaticFaceBakery.OUTPUT_OVERLAY,
                side,
                pipeOverlaySprite));

        quads.add(StaticFaceBakery.bakeFace(
                StaticFaceBakery.AUTO_OUTPUT_OVERLAY,
                side,
                hatchSprite));

        quads.add(StaticFaceBakery.bakeFace(
                MODE_COLOR_OVERLAY,
                side,
                colorSprite,
                BlockModelRotation.X0_Y0,
                -1,
                15,
                false,
                false));

        cir.setReturnValue(quads);
    }

    private static void loadSprites() {
        if (pipeOverlaySprite != null && itemInputOverlaySprite != null && itemOutputOverlaySprite != null &&
                pipeInEmissiveSprite != null && pipeOutEmissiveSprite != null) {
            return;
        }

        var atlas = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS);

        pipeOverlaySprite = atlas
                .apply(ResourceLocation.fromNamespaceAndPath("gtceu", "block/overlay/machine/overlay_pipe"));
        itemInputOverlaySprite = atlas
                .apply(ResourceLocation.fromNamespaceAndPath("gtceu",
                        "block/overlay/machine/overlay_item_hatch_input"));
        itemOutputOverlaySprite = atlas
                .apply(ResourceLocation.fromNamespaceAndPath("gtceu",
                        "block/overlay/machine/overlay_item_hatch_output"));
        pipeInEmissiveSprite = atlas
                .apply(ResourceLocation.fromNamespaceAndPath("gtceu",
                        "block/overlay/machine/overlay_pipe_in_emissive"));
        pipeOutEmissiveSprite = atlas
                .apply(ResourceLocation.fromNamespaceAndPath("gtceu",
                        "block/overlay/machine/overlay_pipe_out_emissive"));
    }
}
