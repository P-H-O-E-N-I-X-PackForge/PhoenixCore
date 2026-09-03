package net.phoenix.core.integration.conflux.dimension.shaders;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import org.lwjgl.opengl.GL13;

@Mod.EventBusSubscriber(modid = "phoenixcore", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ShaderRenderingIntegration {

    private static String lastAppliedShader = "";

    @SubscribeEvent
    public static void onRenderLevelPost(RenderLevelStageEvent event) {
        
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;

        if (level == null || !isInDisciplineDimension(level)) {
            return;
        }

        if (!ClientSetupHandler.areShadersInitialized()) {
            return;
        }

        try {
            String dimensionId = getDimensionId(level);
            ShaderManager shaderManager = ShaderManager.getInstance();

            String primaryShader = getPrimaryShaderForDimension(dimensionId);
            if (primaryShader != null) {
                applyShaderEffect(shaderManager, primaryShader, event.getPoseStack());
            }

            String secondaryShader = getSecondaryShaderForDimension(dimensionId);
            if (secondaryShader != null) {
                applyShaderEffect(shaderManager, secondaryShader, event.getPoseStack());
            }

            shaderManager.stopUsingShader();

        } catch (Exception e) {
            System.err.println("[PhoenixCore] Error applying shaders: " + e.getMessage());
        }
    }

    private static void applyShaderEffect(ShaderManager shaderManager, String shaderName, PoseStack poseStack) {
        CompiledShaderProgram shader = shaderManager.getShader(shaderName);
        if (shader == null || !shader.isValid()) {
            return;
        }

        try {

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            RenderSystem.bindTexture(mainTarget.getColorTextureId());

            shader.use();
            shader.setUniform1i("Sampler0", 0);
            poseStack.pushPose();

            FullscreenQuadRenderer quadRenderer = shaderManager.getQuadRenderer();
            quadRenderer.render(poseStack);

            poseStack.popPose();

            RenderSystem.disableBlend();

        } catch (Exception e) {
            System.err.println("[PhoenixCore] Failed to apply shader " + shaderName + ": " + e.getMessage());
        }
    }

    private static boolean isInDisciplineDimension(Level level) {
        String dimensionId = level.dimension().location().toString();
        return dimensionId.startsWith("phoenixcore:conflux/");
    }

    private static String getDimensionId(Level level) {

        String path = level.dimension().location().getPath();
        String discipline = path.startsWith("conflux/") ? path.substring("conflux/".length()) : path;

        if (discipline.startsWith("phoenix")) return "phoenix";
        if (discipline.startsWith("sculk")) return "sculk";
        if (discipline.startsWith("void")) return "void";
        if (discipline.startsWith("sealed_a")) return "sealed_a";
        if (discipline.startsWith("sealed_b")) return "sealed_b";

        return "";
    }

    private static String getPrimaryShaderForDimension(String dimensionId) {
        return switch (dimensionId) {
            case "phoenix" -> "phoenix_glow";
            case "sculk" -> "sculk_biolum";
            case "void" -> "void_space";
            case "sealed_a" -> "neon_glow";
            case "sealed_b" -> "color_inversion";
            default -> null;
        };
    }

    private static String getSecondaryShaderForDimension(String dimensionId) {
        return switch (dimensionId) {
            case "sculk" -> "sound_ripples";
            case "void" -> "gravity_bridge";
            case "sealed_a" -> "industrial_sky";
            case "sealed_b" -> "reality_distortion";
            default -> null;
        };
    }
}
