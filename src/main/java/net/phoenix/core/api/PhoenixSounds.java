package net.phoenix.core.api;

import com.gregtechceu.gtceu.api.sound.SoundEntry;

import net.minecraft.sounds.SoundSource;
import net.phoenix.core.PhoenixCore;

import static com.gregtechceu.gtceu.common.registry.GTRegistration.REGISTRATE;

public class PhoenixSounds {

    public static final SoundEntry MICROVERSE = REGISTRATE.sound(PhoenixCore.id("microverse")).build();

    // Conflux dimension ambience (see DimensionAudioPresets). Seamless 30 s loops.
    public static final SoundEntry PHOENIX_VOLCANIC_RUMBLE = ambient("ambient/phoenix/volcanic_rumble");
    public static final SoundEntry PHOENIX_VOLCANIC_WIND = ambient("ambient/phoenix/volcanic_wind");
    public static final SoundEntry SCULK_BIOLUM_HUM = ambient("ambient/sculk/biolum_hum");
    public static final SoundEntry SCULK_DEEP_CAVE = ambient("ambient/sculk/deep_cave");
    public static final SoundEntry VOID_COSMIC_DRONE = ambient("ambient/void/cosmic_drone");
    public static final SoundEntry VOID_ETHEREAL_WIND = ambient("ambient/void/ethereal_wind");
    public static final SoundEntry SEALED_A_INDUSTRIAL = ambient("ambient/sealed_a/industrial");
    public static final SoundEntry SEALED_A_NEON_HUM = ambient("ambient/sealed_a/neon_hum");
    public static final SoundEntry SEALED_B_INVERTED_HUM = ambient("ambient/sealed_b/inverted_hum");
    public static final SoundEntry SEALED_B_REALITY_GLITCH = ambient("ambient/sealed_b/reality_glitch");

    // Conflux dimension effects (see DimensionAudioPresets).
    public static final SoundEntry PHOENIX_HEAT_SHIMMER = ambient("effect/phoenix/heat_shimmer");
    public static final SoundEntry PHOENIX_UPDRAFT_FADE = ambient("effect/phoenix/updraft_fade");
    public static final SoundEntry SCULK_CONVEYOR_ACTIVATE = ambient("effect/sculk/conveyor_activate");
    public static final SoundEntry SCULK_CONVEYOR_LOOP = ambient("effect/sculk/conveyor_loop");
    public static final SoundEntry VOID_ZERO_G_ENTER = ambient("effect/void/zero_g_enter");
    public static final SoundEntry VOID_SPIRAL_ASCEND = ambient("effect/void/spiral_ascend");
    public static final SoundEntry SEALED_A_CONVEYOR_WHIRR = ambient("effect/sealed_a/conveyor_whirr");
    public static final SoundEntry SEALED_A_ELEVATOR_HUM = ambient("effect/sealed_a/elevator_hum");
    public static final SoundEntry SEALED_B_GRAVITY_FLIP = ambient("effect/sealed_b/gravity_flip");
    public static final SoundEntry SEALED_B_REALITY_CRACK = ambient("effect/sealed_b/reality_crack");

    // Cutscene background tracks. Seamless 40 s loops, used by the lore cutscenes.
    public static final SoundEntry CUTSCENE_ETHEREAL = cutscene("cutscene/ethereal");
    public static final SoundEntry CUTSCENE_THRESHOLD = cutscene("cutscene/threshold");
    public static final SoundEntry CUTSCENE_PYRE = cutscene("cutscene/pyre");
    public static final SoundEntry CUTSCENE_HORIZON = cutscene("cutscene/horizon");

    private static SoundEntry ambient(String path) {
        return REGISTRATE.sound(PhoenixCore.id(path)).noSubtitle().category(SoundSource.AMBIENT).build();
    }

    private static SoundEntry cutscene(String path) {
        return REGISTRATE.sound(PhoenixCore.id(path)).noSubtitle().category(SoundSource.MUSIC).build();
    }

    public static void init() {}
}
