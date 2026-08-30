package net.phoenix.core.integration.conflux.dimension.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.fml.common.Mod;
import net.phoenix.core.integration.conflux.dimension.DisciplineTheme;
import net.phoenix.core.integration.conflux.dimension.DisciplineThemeRegistry;
import org.lwjgl.opengl.GL11;

@Mod.EventBusSubscriber(modid = "phoenixcore", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class DisciplineSkyRenderer {

    private static String currentDiscipline;
    private static DisciplineTheme.SkyboxProfile skyboxProfile;
    private static float fogDensityOverride = 1.0f;

    public static void setSkyboxProfile(String discipline) {
        currentDiscipline = discipline;
        DisciplineTheme theme = DisciplineThemeRegistry.getTheme(discipline);
        if (theme != null) {
            skyboxProfile = theme.skybox;
        }
    }

    public static int getSkyColor() {
        if (skyboxProfile != null) {
            return skyboxProfile.skyColor;
        }
        return 0xFF8080FF; 
    }

    public static int getFogColor() {
        if (skyboxProfile != null) {
            return skyboxProfile.fogColor;
        }
        return 0xFFC0C0FF;
    }

    public static float getFogDensity() {
        if (skyboxProfile != null) {
            return skyboxProfile.fogDensity;
        }
        return 1.0f;
    }

    public static String getRenderMode() {
        if (skyboxProfile != null) {
            return skyboxProfile.renderMode;
        }
        return "default";
    }

    public static void renderCustomSky(PoseStack poseStack, Level level, float partialTick) {
        if (skyboxProfile == null) {
            return;
        }

        String mode = skyboxProfile.renderMode;
        switch (mode) {
            case "sunset" -> renderSunsetSky(poseStack, partialTick);
            case "bioluminescent" -> renderBioluminescentSky(poseStack, partialTick);
            case "stellar" -> renderStellarSky(poseStack, partialTick);
            case "glitch", "extinction" -> renderDistortedSky(poseStack, partialTick);
        }
    }

    private static void renderSunsetSky(PoseStack poseStack, float partialTick) {
        
        float cycle = (System.currentTimeMillis() % 24000) / 24000.0f;
        float intensity = (float) Math.sin(cycle * Math.PI);

        renderGradientSky(poseStack, 0xFFFF8C00, 0xFFFF4500, intensity);
    }

    private static void renderBioluminescentSky(PoseStack poseStack, float partialTick) {
        
        float cycle = (System.currentTimeMillis() % 8000) / 8000.0f;
        float glow = 0.5f + 0.5f * (float) Math.sin(cycle * Math.PI * 2);

        renderGradientSky(poseStack, 0xFF1A4D6D, 0xFF2E8B9E, glow);
        renderGlowingParticles(poseStack, cycle);
    }

    private static void renderStellarSky(PoseStack poseStack, float partialTick) {
        
        renderGradientSky(poseStack, 0xFF4B0082, 0xFF0B0014, 1.0f);
        renderStars(poseStack);
    }

    private static void renderDistortedSky(PoseStack poseStack, float partialTick) {
        
        float time = System.currentTimeMillis() / 1000.0f;
        int glitchR = (int) (Math.sin(time * 2.3) * 127 + 128);
        int glitchG = (int) (Math.cos(time * 1.7) * 127 + 128);
        int glitchB = (int) (Math.sin(time * 3.1) * 127 + 128);

        int glitchColor = (glitchR << 16) | (glitchG << 8) | glitchB;
        renderGradientSky(poseStack, glitchColor, 0xFF2F1F4D, 0.8f);
    }

    private static void renderGradientSky(PoseStack poseStack, int topColor, int bottomColor, float intensity) {

        float r1 = ((topColor >> 16) & 0xFF) / 255.0f * intensity;
        float g1 = ((topColor >> 8) & 0xFF) / 255.0f * intensity;
        float b1 = (topColor & 0xFF) / 255.0f * intensity;

        float r2 = ((bottomColor >> 16) & 0xFF) / 255.0f * intensity;
        float g2 = ((bottomColor >> 8) & 0xFF) / 255.0f * intensity;
        float b2 = (bottomColor & 0xFF) / 255.0f * intensity;

        RenderSystem.setShaderFogStart(0);
        RenderSystem.setShaderFogEnd(200);
        RenderSystem.setShaderFogColor(r2, g2, b2, 1.0f);
    }

    private static void renderGlowingParticles(PoseStack poseStack, float time) {

    }

    private static void renderStars(PoseStack poseStack) {

    }

    public static void reset() {
        currentDiscipline = null;
        skyboxProfile = null;
    }
}
