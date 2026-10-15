package net.phoenix.core.integration.conflux.dimension.audio;

import net.minecraft.resources.ResourceLocation;

public class DimensionAudioPresets {

    public static void initializeAllDimensionAudio(AudioManager manager) {
        initializePhoenixAudio(manager);
        initializeSculkAudio(manager);
        initializeVoidAudio(manager);
        initializeSealedAAudio(manager);
        initializeSealedBAudio(manager);
    }

    private static void initializePhoenixAudio(AudioManager manager) {
        ResourceLocation modId = new ResourceLocation("phoenixcore");

        AmbientTrack phoenixMain = new AmbientTrack(
                new ResourceLocation(modId.getNamespace(), "ambient/phoenix/volcanic_rumble"),
                0.5f,
                1.0f,
                true,
                3.0f,
                2.0f,
                "Phoenix - Volcanic Rumble");
        manager.registerAmbientTrack("phoenix_main", phoenixMain);

        AmbientTrack phoenixWind = new AmbientTrack(
                new ResourceLocation(modId.getNamespace(), "ambient/phoenix/volcanic_wind"),
                0.3f, 0.9f, true, 2.0f, 2.0f,
                "Phoenix - Volcanic Wind");
        manager.registerAmbientTrack("phoenix_wind", phoenixWind);

        SoundEffect phoenixGravityEnter = new SoundEffect(
                new ResourceLocation(modId.getNamespace(), "effect/phoenix/heat_shimmer"),
                SoundEffect.SoundType.GRAVITY_ZONE_ENTER,
                0.6f, 1.0f, 20.0f,
                "Phoenix - Heat Shimmer (Enter Zone)");
        manager.registerSoundEffect("phoenix_gravity_enter", phoenixGravityEnter);

        SoundEffect phoenixGravityExit = new SoundEffect(
                new ResourceLocation(modId.getNamespace(), "effect/phoenix/updraft_fade"),
                SoundEffect.SoundType.GRAVITY_ZONE_EXIT,
                0.5f, 0.95f, 15.0f,
                "Phoenix - Updraft Fade (Exit Zone)");
        manager.registerSoundEffect("phoenix_gravity_exit", phoenixGravityExit);

        System.out.println("[PhoenixCore Audio] Initialized Phoenix audio (2 tracks + 2 effects)");
    }

    private static void initializeSculkAudio(AudioManager manager) {
        ResourceLocation modId = new ResourceLocation("phoenixcore");

        AmbientTrack sculkMain = new AmbientTrack(
                new ResourceLocation(modId.getNamespace(), "ambient/sculk/deep_cave"),
                0.4f, 1.0f, true, 3.0f, 2.5f,
                "Sculk - Deep Cave Ambience");
        manager.registerAmbientTrack("sculk_main", sculkMain);

        AmbientTrack sculkBiolum = new AmbientTrack(
                new ResourceLocation(modId.getNamespace(), "ambient/sculk/biolum_hum"),
                0.2f, 0.85f, true, 2.0f, 2.0f,
                "Sculk - Bioluminescent Hum");
        manager.registerAmbientTrack("sculk_biolum", sculkBiolum);

        SoundEffect sculkPlatformBoard = new SoundEffect(
                new ResourceLocation(modId.getNamespace(), "effect/sculk/conveyor_activate"),
                SoundEffect.SoundType.PLATFORM_BOARDING,
                0.7f, 1.0f, 25.0f,
                "Sculk - Conveyor Activate");
        manager.registerSoundEffect("sculk_platform_board", sculkPlatformBoard);

        SoundEffect sculkConveyorLoop = new SoundEffect(
                new ResourceLocation(modId.getNamespace(), "effect/sculk/conveyor_loop"),
                SoundEffect.SoundType.PLATFORM_MOVING,
                0.4f, 0.8f, 30.0f,
                "Sculk - Conveyor Loop");
        manager.registerSoundEffect("sculk_conveyor_loop", sculkConveyorLoop);

        System.out.println("[PhoenixCore Audio] Initialized Sculk audio (2 tracks + 2 effects)");
    }

    private static void initializeVoidAudio(AudioManager manager) {
        ResourceLocation modId = new ResourceLocation("phoenixcore");

        AmbientTrack voidMain = new AmbientTrack(
                new ResourceLocation(modId.getNamespace(), "ambient/void/cosmic_drone"),
                0.3f, 0.95f, true, 4.0f, 3.0f,
                "Void - Cosmic Drone");
        manager.registerAmbientTrack("void_main", voidMain);

        AmbientTrack voidWind = new AmbientTrack(
                new ResourceLocation(modId.getNamespace(), "ambient/void/ethereal_wind"),
                0.25f, 0.9f, true, 2.5f, 2.5f,
                "Void - Ethereal Wind");
        manager.registerAmbientTrack("void_wind", voidWind);

        SoundEffect voidGravityZero = new SoundEffect(
                new ResourceLocation(modId.getNamespace(), "effect/void/zero_g_enter"),
                SoundEffect.SoundType.GRAVITY_ZONE_ENTER,
                0.5f, 1.1f, 20.0f,
                "Void - Zero-G Enter");
        manager.registerSoundEffect("void_gravity_enter", voidGravityZero);

        SoundEffect voidSpiralRise = new SoundEffect(
                new ResourceLocation(modId.getNamespace(), "effect/void/spiral_ascend"),
                SoundEffect.SoundType.PLATFORM_MOVING,
                0.6f, 0.8f, 30.0f,
                "Void - Spiral Ascend");
        manager.registerSoundEffect("void_spiral_rise", voidSpiralRise);

        System.out.println("[PhoenixCore Audio] Initialized Void audio (2 tracks + 2 effects)");
    }

    private static void initializeSealedAAudio(AudioManager manager) {
        ResourceLocation modId = new ResourceLocation("phoenixcore");

        AmbientTrack sealedAMain = new AmbientTrack(
                new ResourceLocation(modId.getNamespace(), "ambient/sealed_a/industrial"),
                0.5f, 1.0f, true, 2.0f, 2.0f,
                "Sealed-A - Industrial Machinery");
        manager.registerAmbientTrack("sealed_a_main", sealedAMain);

        AmbientTrack sealedANeon = new AmbientTrack(
                new ResourceLocation(modId.getNamespace(), "ambient/sealed_a/neon_hum"),
                0.3f, 1.05f, true, 2.0f, 2.0f,
                "Sealed-A - Neon Hum");
        manager.registerAmbientTrack("sealed_a_neon", sealedANeon);

        SoundEffect sealedAConveyor = new SoundEffect(
                new ResourceLocation(modId.getNamespace(), "effect/sealed_a/conveyor_whirr"),
                SoundEffect.SoundType.PLATFORM_MOVING,
                0.6f, 1.0f, 32.0f,
                "Sealed-A - Conveyor Whirr");
        manager.registerSoundEffect("sealed_a_conveyor", sealedAConveyor);

        SoundEffect sealedAElevator = new SoundEffect(
                new ResourceLocation(modId.getNamespace(), "effect/sealed_a/elevator_hum"),
                SoundEffect.SoundType.PLATFORM_BOARDING,
                0.7f, 0.95f, 25.0f,
                "Sealed-A - Elevator Hum");
        manager.registerSoundEffect("sealed_a_elevator", sealedAElevator);

        System.out.println("[PhoenixCore Audio] Initialized Sealed-A audio (2 tracks + 2 effects)");
    }

    private static void initializeSealedBAudio(AudioManager manager) {
        ResourceLocation modId = new ResourceLocation("phoenixcore");

        AmbientTrack sealedBMain = new AmbientTrack(
                new ResourceLocation(modId.getNamespace(), "ambient/sealed_b/reality_glitch"),
                0.4f, 1.0f, true, 3.0f, 2.5f,
                "Sealed-B - Reality Glitch");
        manager.registerAmbientTrack("sealed_b_main", sealedBMain);

        AmbientTrack sealedBInverted = new AmbientTrack(
                new ResourceLocation(modId.getNamespace(), "ambient/sealed_b/inverted_hum"),
                0.25f, 0.7f, true, 2.5f, 2.5f,
                "Sealed-B - Inverted Hum");
        manager.registerAmbientTrack("sealed_b_inverted", sealedBInverted);

        SoundEffect sealedBGravityInvert = new SoundEffect(
                new ResourceLocation(modId.getNamespace(), "effect/sealed_b/gravity_flip"),
                SoundEffect.SoundType.GRAVITY_ZONE_ENTER,
                0.8f, 0.6f, 20.0f,
                "Sealed-B - Gravity Flip");
        manager.registerSoundEffect("sealed_b_gravity_flip", sealedBGravityInvert);

        SoundEffect sealedBGlitch = new SoundEffect(
                new ResourceLocation(modId.getNamespace(), "effect/sealed_b/reality_crack"),
                SoundEffect.SoundType.SHADER_ACTIVE,
                0.7f, 1.1f, 15.0f,
                "Sealed-B - Reality Crack");
        manager.registerSoundEffect("sealed_b_glitch", sealedBGlitch);

        System.out.println("[PhoenixCore Audio] Initialized Sealed-B audio (2 tracks + 2 effects)");
    }

    public static String getAudioDescription(String dimensionId) {
        return switch (dimensionId) {
            case "phoenix" -> "Volcanic rumbles + thermal wind ambience";
            case "sculk" -> "Deep cave echoes + bioluminescent hum";
            case "void" -> "Cosmic drone + ethereal wind";
            case "sealed_a" -> "Industrial machinery + neon hum";
            case "sealed_b" -> "Reality glitches + inverted frequencies";
            default -> "No audio configured";
        };
    }
}
