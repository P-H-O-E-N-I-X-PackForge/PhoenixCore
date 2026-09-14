package net.phoenix.core.integration.conflux.dimension.particles;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "phoenixcore", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ParticleEventHook {

    private static long lastParticleSpawn = 0;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        Player player = mc.player;

        if (level == null || player == null) {
            return;
        }

        if (level.getGameTime() % 5 != 0) {
            return;
        }

        String dimensionId = getDimensionId(level);
        Vec3 playerPos = player.position();

        switch (dimensionId) {
            case "phoenix" -> spawnPhoenixParticles(player, playerPos, level);
            case "sculk" -> spawnSculkParticles(player, playerPos, level);
            case "void" -> spawnVoidParticles(player, playerPos, level);
            case "sealed_a" -> spawnSealedAParticles(player, playerPos, level);
            case "sealed_b" -> spawnSealedBParticles(player, playerPos, level);
        }
    }

    private static void spawnPhoenixParticles(Player player, Vec3 playerPos, Level level) {
        ParticleEffectSystem.spawnAshRain(playerPos, 3);

        if (playerPos.y % 20 < 5) {
            ParticleEffectSystem.spawnHeatShimmer(playerPos.add(0, -5, 0), 2);
        }

        if (Math.random() < 0.1) {
            ParticleEffectSystem.spawnLavaSparks(playerPos.add(
                    (Math.random() - 0.5) * 100,
                    (Math.random() - 0.5) * 100,
                    (Math.random() - 0.5) * 100), 5);
        }
    }

    private static void spawnSculkParticles(Player player, Vec3 playerPos, Level level) {
        ParticleEffectSystem.spawnSculkSpores(playerPos, 2);

        ParticleEffectSystem.spawnBiolumGlow(playerPos.add(0, 2, 0), 3);

        if (Math.random() < 0.05) {
            ParticleEffectSystem.spawnSculkSpores(playerPos.add(
                    (Math.random() - 0.5) * 50,
                    (Math.random() - 0.5) * 50,
                    (Math.random() - 0.5) * 50), 4);
        }
    }

    private static void spawnVoidParticles(Player player, Vec3 playerPos, Level level) {
        ParticleEffectSystem.spawnCosmicDust(playerPos, 2);

        ParticleEffectSystem.spawnVoidShimmer(playerPos.add(0, 5, 0), 1);

        if (Math.random() < 0.08) {
            ParticleEffectSystem.spawnStarSparkles(playerPos.add(
                    (Math.random() - 0.5) * 200,
                    (Math.random() - 0.5) * 200,
                    (Math.random() - 0.5) * 200), 3);
        }
    }

    private static void spawnSealedAParticles(Player player, Vec3 playerPos, Level level) {
        ParticleEffectSystem.spawnIndustrialSmoke(playerPos.add(0, 5, 0), 2);

        ParticleEffectSystem.spawnNeonSparks(playerPos, 1);

        if (Math.random() < 0.06) {
            ParticleEffectSystem.spawnNeonSparks(playerPos.add(
                    (Math.random() - 0.5) * 80,
                    (Math.random() - 0.5) * 80,
                    (Math.random() - 0.5) * 80), 4);
        }
    }

    private static void spawnSealedBParticles(Player player, Vec3 playerPos, Level level) {
        ParticleEffectSystem.spawnGlitchEffect(playerPos, 2);

        if (Math.random() < 0.08) {
            ParticleEffectSystem.spawnRealityTears(playerPos.add(
                    (Math.random() - 0.5) * 60,
                    (Math.random() - 0.5) * 60,
                    (Math.random() - 0.5) * 60), 3);
        }

        ParticleEffectSystem.spawnGlitchEffect(playerPos.add(0, -5, 0), 1);
    }

    public static void triggerParticles(String dimensionId, String eventType, Vec3 position, int intensity) {
        switch (dimensionId) {
            case "phoenix" -> triggerPhoenixEvent(eventType, position, intensity);
            case "sculk" -> triggerSculkEvent(eventType, position, intensity);
            case "void" -> triggerVoidEvent(eventType, position, intensity);
            case "sealed_a" -> triggerSealedAEvent(eventType, position, intensity);
            case "sealed_b" -> triggerSealedBEvent(eventType, position, intensity);
        }
    }

    private static void triggerPhoenixEvent(String event, Vec3 pos, int intensity) {
        switch (event) {
            case "enter_zone" -> ParticleEffectSystem.spawnHeatShimmer(pos, intensity * 5);
            case "exit_zone" -> ParticleEffectSystem.spawnAshRain(pos, intensity * 3);
            case "volcano_erupt" -> ParticleEffectSystem.spawnLavaSparks(pos, intensity * 10);
        }
    }

    private static void triggerSculkEvent(String event, Vec3 pos, int intensity) {
        switch (event) {
            case "enter_platform" -> ParticleEffectSystem.spawnBiolumGlow(pos, intensity * 5);
            case "tendril_grow" -> ParticleEffectSystem.spawnSculkSpores(pos, intensity * 8);
        }
    }

    private static void triggerVoidEvent(String event, Vec3 pos, int intensity) {
        switch (event) {
            case "enter_zero_g" -> ParticleEffectSystem.spawnVoidShimmer(pos, intensity * 6);
            case "planet_pass" -> ParticleEffectSystem.spawnCosmicDust(pos, intensity * 4);
        }
    }

    private static void triggerSealedAEvent(String event, Vec3 pos, int intensity) {
        switch (event) {
            case "conveyor_start" -> ParticleEffectSystem.spawnNeonSparks(pos, intensity * 4);
            case "factory_active" -> ParticleEffectSystem.spawnIndustrialSmoke(pos, intensity * 5);
        }
    }

    private static void triggerSealedBEvent(String event, Vec3 pos, int intensity) {
        switch (event) {
            case "glitch" -> ParticleEffectSystem.spawnGlitchEffect(pos, intensity * 6);
            case "reality_break" -> ParticleEffectSystem.spawnRealityTears(pos, intensity * 8);
        }
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
