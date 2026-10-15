package net.phoenix.core.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.entity.TetoSlime;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

public class TetoSlimeRenderer extends MobRenderer<TetoSlime, TetoSlimeModel<TetoSlime>> {

    public static final ModelLayerLocation BODY = new ModelLayerLocation(PhoenixCore.id("teto_slime"), "main");
    public static final ModelLayerLocation OUTER = new ModelLayerLocation(PhoenixCore.id("teto_slime"), "outer");
    private static final ResourceLocation TEXTURE = PhoenixCore.id("textures/entity/teto_slime.png");

    public TetoSlimeRenderer(EntityRendererProvider.Context context) {
        super(context, new TetoSlimeModel<>(context.bakeLayer(BODY)), 0.25f);
        addLayer(new OuterLayer(this, new TetoSlimeModel<>(context.bakeLayer(OUTER))));
    }

    @Override
    public void render(TetoSlime slime, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffer,
                       int light) {
        shadowRadius = 0.25f * slime.getSize();
        super.render(slime, yaw, partialTick, pose, buffer, light);
    }

    @Override
    protected void scale(TetoSlime slime, PoseStack pose, float partialTick) {
        pose.scale(0.999f, 0.999f, 0.999f);
        pose.translate(0.0f, 0.001f, 0.0f);
        float size = slime.getSize();
        float squish = Mth.lerp(partialTick, slime.oSquish, slime.squish) / (size * 0.5f + 1.0f);
        float stretch = 1.0f / (squish + 1.0f);
        pose.scale(stretch * size, 1.0f / stretch * size, stretch * size);
    }

    @Override
    public ResourceLocation getTextureLocation(TetoSlime slime) {
        return TEXTURE;
    }

    private static final class OuterLayer extends RenderLayer<TetoSlime, TetoSlimeModel<TetoSlime>> {

        private final TetoSlimeModel<TetoSlime> shell;

        private OuterLayer(RenderLayerParent<TetoSlime, TetoSlimeModel<TetoSlime>> parent,
                           TetoSlimeModel<TetoSlime> shell) {
            super(parent);
            this.shell = shell;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffer, int light, TetoSlime slime, float limbSwing,
                           float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
                           float headPitch) {
            if (slime.isInvisible()) return;

            getParentModel().copyPropertiesTo(shell);
            shell.prepareMobModel(slime, limbSwing, limbSwingAmount, partialTick);
            shell.setupAnim(slime, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
            shell.renderToBuffer(pose, consumer, light, LivingEntityRenderer.getOverlayCoords(slime, 0.0f), 1.0f, 1.0f,
                    1.0f, 1.0f);
        }
    }
}
