package net.phoenix.core.client.renderer.machine;

import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;
import com.gregtechceu.gtceu.client.util.ModelEventHelper;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.model.data.ModelData;

import net.phoenix.core.common.machine.multiblock.ward.SanctumWardMachine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import org.joml.Quaternionf;

import java.util.List;

/**
 * Reuses just the sphere half of {@link PlasmaArcFurnaceRender} (the "blue_star" obj model, no rings)
 * as the Sanctum Ward's visual - a translucent dome scaled directly to {@link SanctumWardMachine#WARD_RADIUS_XZ}
 * (25 blocks), so its visible size always matches the actual ward radius rather than being a fixed
 * decorative prop like the furnace's floating star. Registers its own bake-event listener for the same
 * {@link PlasmaArcFurnaceRender#SPHERE_MODEL_RL} asset rather than reaching into that class's private
 * field - the asset itself only needs registering once (already done for the furnace in
 * {@code PhoenixClient}), and Forge's model-bake event happily notifies multiple independent listeners
 * for the same resource location.
 */
@SuppressWarnings("all")
public class SanctumWardRender extends DynamicRender<WorkableElectricMultiblockMachine, SanctumWardRender> {

    public static final SanctumWardRender INSTANCE = new SanctumWardRender();
    public static final Codec<SanctumWardRender> CODEC = Codec.unit(SanctumWardRender.INSTANCE);
    public static final DynamicRenderType<WorkableElectricMultiblockMachine, SanctumWardRender> TYPE = new DynamicRenderType<>(
            SanctumWardRender.CODEC);

    private static BakedModel sphereModel;
    private static final RandomSource random = RandomSource.create();

    private static final float ROTATION_SPEED = 0.006F;
    private static final float DOME_ALPHA = 0.12F;

    private SanctumWardRender() {
        ModelEventHelper.registerBakeEventListener(true, (rl, bakedModel, rootModel, modelBakery) -> {
            if (rl.equals(PlasmaArcFurnaceRender.SPHERE_MODEL_RL)) {
                sphereModel = bakedModel;
            }
            return bakedModel;
        });
    }

    @Override
    public DynamicRenderType<WorkableElectricMultiblockMachine, SanctumWardRender> getType() {
        return TYPE;
    }

    @Override
    public void render(WorkableElectricMultiblockMachine machine, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!(machine instanceof SanctumWardMachine ward) || !ward.isWardActive()) return;
        if (sphereModel == null) return;

        float tick = machine.getOffsetTimer() + partialTick;
        float angle = (tick * ROTATION_SPEED) % 360.0F;

        poseStack.pushPose();
        poseStack.translate(0.5, 1.5, 0.5);
        poseStack.mulPose(new Quaternionf().fromAxisAngleDeg(0.0F, 1.0F, 0.0F, angle));
        poseStack.scale((float) SanctumWardMachine.WARD_RADIUS_XZ, (float) SanctumWardMachine.WARD_RADIUS_XZ,
                (float) SanctumWardMachine.WARD_RADIUS_XZ);

        PoseStack.Pose pose = poseStack.last();
        VertexConsumer consumer = buffer.getBuffer(Sheets.translucentCullBlockSheet());
        List<BakedQuad> quads = sphereModel.getQuads(null, null, random, ModelData.EMPTY, null);
        for (BakedQuad quad : quads) {
            consumer.putBulkData(pose, quad, 0.55F, 0.85F, 1.0F, DOME_ALPHA, LightTexture.FULL_BRIGHT, packedOverlay,
                    false);
        }

        poseStack.popPose();
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public boolean shouldRenderOffScreen(WorkableElectricMultiblockMachine machine) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(WorkableElectricMultiblockMachine machine) {
        return new AABB(machine.getBlockPos()).inflate(SanctumWardMachine.WARD_RADIUS_XZ + 4.0);
    }
}
