package net.phoenix.core.client.renderer.entity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.phoenix.core.common.entity.PlacedItemEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

/** A placed item drawn on its block face the way an item frame draws its item, only smaller. */
public class PlacedItemRenderer extends EntityRenderer<PlacedItemEntity> {

    private final EntityRendererProvider.Context context;

    public PlacedItemRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.context = context;
    }

    @Override
    public ResourceLocation getTextureLocation(PlacedItemEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");
    }

    @Override
    public void render(PlacedItemEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light) {
        super.render(entity, yaw, partialTick, pose, buffers, light);
        if (entity.item().isEmpty()) return;
        Direction face = entity.face();
        float scale = entity.scale();
        pose.pushPose();
        // as an item frame does: turn so the front faces out of the block
        float xRot = face == Direction.UP ? -90.0f : face == Direction.DOWN ? 90.0f : 0.0f;
        float yRot = face.getAxis().isHorizontal() ? face.get2DDataValue() * 90.0f : 0.0f;
        pose.mulPose(Axis.XP.rotationDegrees(xRot));
        pose.mulPose(Axis.YP.rotationDegrees(180.0f - yRot));
        pose.mulPose(Axis.ZP.rotationDegrees(entity.spin() * 45.0f));
        // a block's middle stands half its size off the face; a flat item lies just off it
        pose.translate(0.0, 0.0, entity.isBlock() ? -scale * 0.5f : -0.03f);
        pose.scale(scale, scale, scale);
        context.getItemRenderer().renderStatic(entity.item(), ItemDisplayContext.FIXED, light,
                OverlayTexture.NO_OVERLAY, pose, buffers, entity.level(), entity.getId());
        pose.popPose();
    }
}
