package net.phoenix.core.integration.conflux.dimension.shaders;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "phoenixcore", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ShaderIntegration {

    private static String currentDimension = "";
    private static long lastShaderUpdate = 0;

    @SubscribeEvent
    public static void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Level level = mc.level;

        if (player == null || level == null) return;

        if (mc.level.getGameTime() % 1 == 0) {
            updateShaderUniforms(mc, player, level);
        }
    }

    private static void updateShaderUniforms(Minecraft mc, Player player, Level level) {
        String dimensionId = getDimensionId(level);

        if (!dimensionId.equals(currentDimension)) {

            currentDimension = dimensionId;
            initializeShadersForDimension(dimensionId);
        }

        ShaderManager shaderManager = ShaderManager.getInstance();
        long gameTime = level.getGameTime();

        switch (dimensionId) {
            case "phoenix" -> updatePhoenixShaders(shaderManager, level);
            case "sculk" -> updateSculkShaders(shaderManager, level, player);
            case "void" -> updateVoidShaders(shaderManager, level, player);
            case "sealed_a" -> updateSealedAShaders(shaderManager, level);
            case "sealed_b" -> updateSealedBShaders(shaderManager, level, player);
        }

        shaderManager.setUniform("all", "GameTime", gameTime * 0.05f);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;

        if (level == null || !isDisciplineDimension(level)) {
            return;
        }

        String dimensionId = getDimensionId(level);
        ShaderManager shaderManager = ShaderManager.getInstance();

        String primaryShader = getPrimaryShaderForDimension(dimensionId);
        if (primaryShader != null) {
            shaderManager.useShader(primaryShader);

        }

        String secondaryShader = getSecondaryShaderForDimension(dimensionId);
        if (secondaryShader != null) {
            shaderManager.useShader(secondaryShader);

        }
    }

    private static void updatePhoenixShaders(ShaderManager manager, Level level) {
        float heat = 0.6f + (float) Math.sin(level.getGameTime() * 0.02f) * 0.4f;
        manager.setUniform("phoenix_glow", "HeatIntensity", heat);
    }

    private static void updateSculkShaders(ShaderManager manager, Level level, Player player) {
        manager.setUniform3f("sculk_biolum", "PlayerPos",
                (float) player.getX(),
                (float) player.getY(),
                (float) player.getZ());

        float soundIntensity = (float) Math.sin(level.getGameTime() * 0.1f) * 0.5f + 0.5f;
        manager.setUniform("sound_ripples", "SoundIntensity", soundIntensity);
    }

    private static void updateVoidShaders(ShaderManager manager, Level level, Player player) {
        manager.setUniform3f("gravity_bridge", "BridgeCenter",
                (float) player.getX(),
                (float) player.getY(),
                (float) player.getZ());

        float bridgePulse = (float) Math.sin(level.getGameTime() * 0.03f) * 0.3f + 0.7f;
        manager.setUniform("gravity_bridge", "BridgeRadius", bridgePulse * 15.0f);
    }

    private static void updateSealedAShaders(ShaderManager manager, Level level) {
        float neonPulse = 0.7f + (float) Math.sin(level.getGameTime() * 0.05f) * 0.3f;
        manager.setUniform("neon_glow", "NeonIntensity", neonPulse);

        manager.setUniform("industrial_sky", "SmogDensity", 0.6f);
    }

    private static void updateSealedBShaders(ShaderManager manager, Level level, Player player) {
        manager.setUniform3f("color_inversion", "InversionZoneCenter",
                (float) player.getX(),
                (float) player.getY(),
                (float) player.getZ());

        float zoneRadius = 15.0f + (float) Math.sin(level.getGameTime() * 0.02f) * 8.0f;
        manager.setUniform("color_inversion", "InversionZoneRadius", zoneRadius);

        float distortionStrength = 0.5f + (float) Math.sin(level.getGameTime() * 0.03f) * 0.3f;
        manager.setUniform("reality_distortion", "DistortionStrength", distortionStrength);
    }

    private static void initializeShadersForDimension(String dimensionId) {
        ShaderManager manager = ShaderManager.getInstance();

        switch (dimensionId) {
            case "phoenix" -> manager.useShader("phoenix_glow");
            case "sculk" -> {
                manager.useShader("sculk_biolum");
                manager.useShader("sound_ripples");
            }
            case "void" -> {
                manager.useShader("void_space");
                manager.useShader("gravity_bridge");
            }
            case "sealed_a" -> {
                manager.useShader("neon_glow");
                manager.useShader("industrial_sky");
            }
            case "sealed_b" -> {
                manager.useShader("color_inversion");
                manager.useShader("reality_distortion");
            }
        }
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

    private static boolean isDisciplineDimension(Level level) {
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
}
