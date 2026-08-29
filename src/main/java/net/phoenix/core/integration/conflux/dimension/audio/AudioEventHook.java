package net.phoenix.core.integration.conflux.dimension.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "phoenixcore", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class AudioEventHook {

    private static String lastDimension = "";
    private static boolean audioInitialized = false;

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

        if (!audioInitialized) {
            initializeAudio();
            audioInitialized = true;
        }

        String currentDimension = getDimensionId(level);
        if (!currentDimension.equals(lastDimension)) {
            lastDimension = currentDimension;
            onDimensionChanged(currentDimension);
        }

        AudioManager.getInstance().updateAmbience(0.05f);
    }

    private static void initializeAudio() {
        AudioManager manager = AudioManager.getInstance();
        DimensionAudioPresets.initializeAllDimensionAudio(manager);

        System.out.println("[PhoenixCore Audio] Audio system initialized");
        manager.logStatistics();
    }

    private static void onDimensionChanged(String dimensionId) {
        AudioManager manager = AudioManager.getInstance();

        if (dimensionId.isEmpty()) {
            manager.stopAllAmbientTracks();
            System.out.println("[PhoenixCore Audio] Left discipline dimension");
        } else {
            manager.onDimensionChange(dimensionId);
            System.out.println("[PhoenixCore Audio] Entered dimension: " + dimensionId);
        }
    }

    public static void onGravityZoneEnter(String dimensionId, Player player) {
        AudioManager manager = AudioManager.getInstance();

        String soundId = switch (dimensionId) {
            case "phoenix" -> "phoenix_gravity_enter";
            case "sculk" -> "sculk_gravity_enter";
            case "void" -> "void_gravity_enter";
            case "sealed_b" -> "sealed_b_gravity_flip";
            default -> null;
        };

        if (soundId != null) {
            manager.playSoundEffect(soundId, player);
        }
    }

    public static void onGravityZoneExit(String dimensionId, Player player) {
        AudioManager manager = AudioManager.getInstance();

        String soundId = switch (dimensionId) {
            case "phoenix" -> "phoenix_gravity_exit";
            case "void" -> "void_gravity_exit";
            default -> null;
        };

        if (soundId != null) {
            manager.playSoundEffect(soundId, player);
        }
    }

    public static void onPlatformBoard(String dimensionId, Player player) {
        AudioManager manager = AudioManager.getInstance();

        String soundId = switch (dimensionId) {
            case "sculk" -> "sculk_platform_board";
            case "sealed_a" -> "sealed_a_elevator";
            default -> null;
        };

        if (soundId != null) {
            manager.playSoundEffect(soundId, player);
        }
    }

    public static void onPlatformMoving(String dimensionId, Player player) {
        AudioManager manager = AudioManager.getInstance();

        String soundId = switch (dimensionId) {
            case "sculk" -> "sculk_conveyor_loop";
            case "void" -> "void_spiral_rise";
            case "sealed_a" -> "sealed_a_conveyor";
            default -> null;
        };

        if (soundId != null) {
            manager.playSoundEffect(soundId, player);
        }
    }

    public static void onShaderActivate(String dimensionId, Player player) {
        AudioManager manager = AudioManager.getInstance();

        String soundId = switch (dimensionId) {
            case "sealed_b" -> "sealed_b_glitch";
            default -> null;
        };

        if (soundId != null) {
            manager.playSoundEffect(soundId, player);
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

    public static boolean isAudioInitialized() {
        return audioInitialized;
    }

    public static void resetAudio() {
        audioInitialized = false;
        lastDimension = "";
        AudioManager.getInstance().cleanup();
    }
}
