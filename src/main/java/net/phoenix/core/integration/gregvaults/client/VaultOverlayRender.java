package net.phoenix.core.integration.gregvaults.client;

import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.integration.gregvaults.common.multiblock.VaultMachine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mojang.serialization.Codec;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class VaultOverlayRender extends DynamicRender<VaultMachine, VaultOverlayRender> {

    public static final VaultOverlayRender INSTANCE = new VaultOverlayRender();
    public static final Codec<VaultOverlayRender> CODEC = Codec.unit(INSTANCE);
    public static final DynamicRenderType<VaultMachine, VaultOverlayRender> TYPE = new DynamicRenderType<>(CODEC);

    public static final ResourceLocation OVERLAY_MODEL = PhoenixCore.id("block/vault_controller_overlay");

    private static final RandomSource random = RandomSource.create();

    private VaultOverlayRender() {}

    @Override
    public DynamicRenderType<VaultMachine, VaultOverlayRender> getType() {
        return TYPE;
    }

    public static void registerModel(ModelEvent.RegisterAdditional event) {
        event.register(OVERLAY_MODEL);
    }

    @Override
    public boolean shouldRenderOffScreen(VaultMachine machine) {
        return true;
    }

    @Override
    public void render(VaultMachine machine, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!machine.isFormed()) return;
        BakedModel overlayModel = Minecraft.getInstance().getModelManager().getModel(OVERLAY_MODEL);
        if (overlayModel == null) return;

        poseStack.pushPose();

        Direction front = machine.getFrontFacing();
        Direction up = machine.getUpwardsFacing();

        poseStack.translate(0.5, 0.5, 0.5);

        switch (front) {
            case NORTH -> {}
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(270));
            case UP -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(90));
                switch (up) {
                    case EAST -> poseStack.mulPose(Axis.ZP.rotationDegrees(90));
                    case WEST -> poseStack.mulPose(Axis.ZN.rotationDegrees(90));
                    case SOUTH -> poseStack.mulPose(Axis.ZP.rotationDegrees(180));
                    default -> {}
                }
            }
            case DOWN -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                switch (up) {
                    case EAST -> poseStack.mulPose(Axis.ZN.rotationDegrees(90));
                    case WEST -> poseStack.mulPose(Axis.ZP.rotationDegrees(90));
                    case SOUTH -> poseStack.mulPose(Axis.ZP.rotationDegrees(180));
                    default -> {}
                }
            }
        }

        if (front.getAxis() != Direction.Axis.Y) {
            switch (up) {
                case EAST -> poseStack.mulPose(Axis.ZN.rotationDegrees(90));
                case WEST -> poseStack.mulPose(Axis.ZP.rotationDegrees(90));
                case SOUTH -> poseStack.mulPose(Axis.ZP.rotationDegrees(180));
                default -> {}
            }
        }

        poseStack.scale(3.0f, 3.0f, 3.0f);
        poseStack.translate(-0.5, -0.5, -0.17);

        VertexConsumer consumer = buffer.getBuffer(RenderType.cutout());
        PoseStack.Pose pose = poseStack.last();

        List<BakedQuad> quads = overlayModel.getQuads(null, null, random, ModelData.EMPTY, RenderType.cutout());
        for (BakedQuad quad : quads) {
            consumer.putBulkData(pose, quad, 1f, 1f, 1f, LightTexture.FULL_BRIGHT, packedOverlay);
        }
        for (Direction side : Direction.values()) {
            quads = overlayModel.getQuads(null, side, random, ModelData.EMPTY, RenderType.cutout());
            for (BakedQuad quad : quads) {
                consumer.putBulkData(pose, quad, 1f, 1f, 1f, LightTexture.FULL_BRIGHT, packedOverlay);
            }
        }

        poseStack.popPose();
    }
}
