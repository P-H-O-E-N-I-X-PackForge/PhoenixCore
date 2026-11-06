package net.phoenix.core.api;

import com.gregtechceu.gtceu.api.sound.SoundEntry;

import net.minecraft.sounds.SoundSource;

import com.tterrag.registrate.util.entry.RegistryEntry;

import static net.phoenix.core.common.registry.PhoenixRegistration.REGISTRATE;

public class PhoenixSounds {

    public static final RegistryEntry<SoundEntry, SoundEntry> MICROVERSE = REGISTRATE.sound("microverse").register();

    public static final RegistryEntry<SoundEntry, SoundEntry> PHOENIX_VOLCANIC_RUMBLE = ambient(
            "ambient/phoenix/volcanic_rumble");
    public static final RegistryEntry<SoundEntry, SoundEntry> PHOENIX_VOLCANIC_WIND = ambient(
            "ambient/phoenix/volcanic_wind");
    public static final RegistryEntry<SoundEntry, SoundEntry> SCULK_BIOLUM_HUM = ambient("ambient/sculk/biolum_hum");
    public static final RegistryEntry<SoundEntry, SoundEntry> SCULK_DEEP_CAVE = ambient("ambient/sculk/deep_cave");
    public static final RegistryEntry<SoundEntry, SoundEntry> VOID_COSMIC_DRONE = ambient("ambient/void/cosmic_drone");
    public static final RegistryEntry<SoundEntry, SoundEntry> VOID_ETHEREAL_WIND = ambient(
            "ambient/void/ethereal_wind");
    public static final RegistryEntry<SoundEntry, SoundEntry> SEALED_A_INDUSTRIAL = ambient(
            "ambient/sealed_a/industrial");
    public static final RegistryEntry<SoundEntry, SoundEntry> SEALED_A_NEON_HUM = ambient("ambient/sealed_a/neon_hum");
    public static final RegistryEntry<SoundEntry, SoundEntry> SEALED_B_INVERTED_HUM = ambient(
            "ambient/sealed_b/inverted_hum");
    public static final RegistryEntry<SoundEntry, SoundEntry> SEALED_B_REALITY_GLITCH = ambient(
            "ambient/sealed_b/reality_glitch");

    public static final RegistryEntry<SoundEntry, SoundEntry> PHOENIX_HEAT_SHIMMER = ambient(
            "effect/phoenix/heat_shimmer");
    public static final RegistryEntry<SoundEntry, SoundEntry> PHOENIX_UPDRAFT_FADE = ambient(
            "effect/phoenix/updraft_fade");
    public static final RegistryEntry<SoundEntry, SoundEntry> SCULK_CONVEYOR_ACTIVATE = ambient(
            "effect/sculk/conveyor_activate");
    public static final RegistryEntry<SoundEntry, SoundEntry> SCULK_CONVEYOR_LOOP = ambient(
            "effect/sculk/conveyor_loop");
    public static final RegistryEntry<SoundEntry, SoundEntry> VOID_ZERO_G_ENTER = ambient("effect/void/zero_g_enter");
    public static final RegistryEntry<SoundEntry, SoundEntry> VOID_SPIRAL_ASCEND = ambient("effect/void/spiral_ascend");
    public static final RegistryEntry<SoundEntry, SoundEntry> SEALED_A_CONVEYOR_WHIRR = ambient(
            "effect/sealed_a/conveyor_whirr");
    public static final RegistryEntry<SoundEntry, SoundEntry> SEALED_A_ELEVATOR_HUM = ambient(
            "effect/sealed_a/elevator_hum");
    public static final RegistryEntry<SoundEntry, SoundEntry> SEALED_B_GRAVITY_FLIP = ambient(
            "effect/sealed_b/gravity_flip");
    public static final RegistryEntry<SoundEntry, SoundEntry> SEALED_B_REALITY_CRACK = ambient(
            "effect/sealed_b/reality_crack");

    public static final RegistryEntry<SoundEntry, SoundEntry> CUTSCENE_ETHEREAL = cutscene("cutscene/ethereal");
    public static final RegistryEntry<SoundEntry, SoundEntry> CUTSCENE_THRESHOLD = cutscene("cutscene/threshold");
    public static final RegistryEntry<SoundEntry, SoundEntry> CUTSCENE_PYRE = cutscene("cutscene/pyre");
    public static final RegistryEntry<SoundEntry, SoundEntry> CUTSCENE_HORIZON = cutscene("cutscene/horizon");

    private static RegistryEntry<SoundEntry, SoundEntry> ambient(String path) {
        return REGISTRATE.sound(path).noSubtitle().category(SoundSource.AMBIENT).register();
    }

    private static RegistryEntry<SoundEntry, SoundEntry> cutscene(String path) {
        return REGISTRATE.sound(path).noSubtitle().category(SoundSource.MUSIC).register();
    }

    public static void init() {}
}
