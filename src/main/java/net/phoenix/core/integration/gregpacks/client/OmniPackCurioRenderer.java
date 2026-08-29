package net.phoenix.core.integration.gregpacks.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public class OmniPackCurioRenderer implements ICurioRenderer {

    public static final OmniPackCurioRenderer INSTANCE = new OmniPackCurioRenderer();

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(
                                                                          ItemStack stack, SlotContext slotContext,
                                                                          PoseStack poseStack,
                                                                          RenderLayerParent<T, M> renderLayerParent,
                                                                          MultiBufferSource bufferSource,
                                                                          int light, float limbSwing,
                                                                          float limbSwingAmount,
                                                                          float partialTicks, float ageInTicks,
                                                                          float netHeadYaw, float headPitch) {
        if (!(stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem)) return;

        poseStack.pushPose();

        poseStack.translate(0.0, 0.30, 0.25);
        poseStack.mulPose(Axis.XP.rotationDegrees(180f));
        poseStack.scale(0.7f, 0.7f, 0.7f);
        poseStack.translate(-0.5, -0.5, -0.5);

        Minecraft mc = Minecraft.getInstance();
        net.minecraft.world.level.block.state.BlockState state = blockItem.getBlock().defaultBlockState();
        mc.getBlockRenderer().renderSingleBlock(state, poseStack, bufferSource, light,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);

        poseStack.popPose();
    }
}
