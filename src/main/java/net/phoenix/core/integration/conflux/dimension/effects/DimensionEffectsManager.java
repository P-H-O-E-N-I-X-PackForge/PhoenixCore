package net.phoenix.core.integration.conflux.dimension.effects;

import net.minecraft.world.level.Level;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public class DimensionEffectsManager {

    private static final DimensionEffectsManager INSTANCE = new DimensionEffectsManager();

    private BioluminescentLighting biolumSystem;
    private NeonLightingSystem neonSystem;
    private SkyEffectsRenderer skyRenderer;
    private PostProcessingEffects postProcessor;
    private ParticleEffectSystem particleSystem;
    private PhysicsEffectsSystem physicsSystem;

    public static DimensionEffectsManager getInstance() {
        return INSTANCE;
    }

    public void initializeForDimension(String disciplineId) {
        switch (disciplineId) {
            case "phoenix" -> initializePhoenixEffects();
            case "sculk" -> initializeSculkEffects();
            case "void" -> initializeVoidEffects();
            case "sealed_a" -> initializeSealedAEffects();
            case "sealed_b" -> initializeSealedBEffects();
        }
    }

    private void initializePhoenixEffects() {
        skyRenderer = new SkyEffectsRenderer.PhoenixSkyRenderer();
        particleSystem = new ParticleEffectSystem.AshRainParticles();
        physicsSystem = new PhysicsEffectsSystem.GravityAnomalies();
    }

    private void initializeSculkEffects() {
        biolumSystem = new BioluminescentLighting();
        particleSystem = new ParticleEffectSystem.SoundRipples();
        physicsSystem = new PhysicsEffectsSystem.SculkTendrilGrowth();
    }

    private void initializeVoidEffects() {
        skyRenderer = new SkyEffectsRenderer.VoidSkyRenderer();
        particleSystem = new ParticleEffectSystem.CosmicDust();
        physicsSystem = new PhysicsEffectsSystem.GravityBridges();
    }

    private void initializeSealedAEffects() {
        neonSystem = new NeonLightingSystem();
        skyRenderer = new SkyEffectsRenderer.IndustrialSkyRenderer();
        physicsSystem = new PhysicsEffectsSystem.MovingPlatforms();
    }

    private void initializeSealedBEffects() {
        postProcessor = new PostProcessingEffects.RealityDistortion();
        particleSystem = new ParticleEffectSystem.GlitchEffects();
        physicsSystem = new PhysicsEffectsSystem.RealityGlitches();
    }

    public void tick(Level level, @Nullable Player player) {
        if (biolumSystem != null) {
            biolumSystem.update(level, player);
        }
        if (neonSystem != null) {
            neonSystem.update(level, player);
        }
        if (skyRenderer != null) {
            skyRenderer.update(level);
        }
        if (particleSystem != null) {
            particleSystem.update(level, player);
        }
        if (physicsSystem != null) {
            physicsSystem.update(level, player);
        }
    }

    public void render(GameRenderer renderer, float partialTick) {
        if (skyRenderer != null) {
            skyRenderer.render(partialTick);
        }
        if (postProcessor != null) {
            postProcessor.render(partialTick);
        }
    }

    @Nullable
    public BioluminescentLighting getBiolumSystem() {
        return biolumSystem;
    }

    @Nullable
    public NeonLightingSystem getNeonSystem() {
        return neonSystem;
    }

    @Nullable
    public SkyEffectsRenderer getSkyRenderer() {
        return skyRenderer;
    }

    @Nullable
    public PostProcessingEffects getPostProcessor() {
        return postProcessor;
    }
}
