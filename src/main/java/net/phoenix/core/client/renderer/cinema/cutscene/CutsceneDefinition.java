package net.phoenix.core.client.renderer.cinema.cutscene;

import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.phoenix.core.client.renderer.cinema.cutscene.background.CutsceneBackground;
import net.phoenix.core.client.renderer.cinema.cutscene.background.CutsceneBackgrounds;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public record CutsceneDefinition(
                                 List<CutscenePage> pages,
                                 float blackDuration,
                                 float fadeIn,
                                 Optional<Float> textDelay,
                                 float fadeOut,
                                 float charsPerSecond,
                                 float punctuationPause,
                                 float autoAdvance,
                                 boolean skippable,
                                 boolean pauseGame,
                                 TextSettings text,
                                 CutsceneBackground.Data background,
                                 Optional<SoundSettings> sound,
                                 Optional<MusicSettings> music,
                                 List<ResourceLocation> onFinish) {

    public static final Codec<CutsceneDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            CutscenePage.CODEC.listOf().fieldOf("pages").forGetter(CutsceneDefinition::pages),
            Codec.FLOAT.optionalFieldOf("black_duration", 2f).forGetter(CutsceneDefinition::blackDuration),
            Codec.FLOAT.optionalFieldOf("fade_in", 3f).forGetter(CutsceneDefinition::fadeIn),
            Codec.FLOAT.optionalFieldOf("text_delay").forGetter(CutsceneDefinition::textDelay),
            Codec.FLOAT.optionalFieldOf("fade_out", 2f).forGetter(CutsceneDefinition::fadeOut),
            Codec.FLOAT.optionalFieldOf("chars_per_second", 22f).forGetter(CutsceneDefinition::charsPerSecond),
            Codec.FLOAT.optionalFieldOf("punctuation_pause", 0.35f).forGetter(CutsceneDefinition::punctuationPause),
            Codec.FLOAT.optionalFieldOf("auto_advance", -1f).forGetter(CutsceneDefinition::autoAdvance),
            Codec.BOOL.optionalFieldOf("skippable", true).forGetter(CutsceneDefinition::skippable),
            Codec.BOOL.optionalFieldOf("pause_game", true).forGetter(CutsceneDefinition::pauseGame),
            TextSettings.CODEC.optionalFieldOf("text", TextSettings.DEFAULT).forGetter(CutsceneDefinition::text),
            CutsceneBackgrounds.CODEC
                    .optionalFieldOf("background", CutsceneBackgrounds.preset(CutsceneBackgrounds.PRISMATIC))
                    .forGetter(CutsceneDefinition::background),
            SoundSettings.CODEC.optionalFieldOf("sound").forGetter(CutsceneDefinition::sound),
            MusicSettings.CODEC.optionalFieldOf("music").forGetter(CutsceneDefinition::music),
            ResourceLocation.CODEC.listOf().optionalFieldOf("on_finish", List.of())
                    .forGetter(CutsceneDefinition::onFinish))
            .apply(i, CutsceneDefinition::new));

    public float textStart() {
        return textDelay.orElse(blackDuration + fadeIn * 0.5f);
    }

    public record TextSettings(TextColor color, float scale, float maxWidth, int lineSpacing, boolean shadow,
                               float waveAmplitude, float waveSpeed, float waveFrequency) {

        public static final TextSettings DEFAULT = new TextSettings(
                TextColor.fromRgb(0xFFFFFF), 1.5f, 0.6f, 4, true, 1.2f, 3f, 0.45f);

        public static final Codec<TextSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
                TextColor.CODEC.optionalFieldOf("color", DEFAULT.color).forGetter(TextSettings::color),
                Codec.FLOAT.optionalFieldOf("scale", DEFAULT.scale).forGetter(TextSettings::scale),
                Codec.FLOAT.optionalFieldOf("max_width", DEFAULT.maxWidth).forGetter(TextSettings::maxWidth),
                Codec.INT.optionalFieldOf("line_spacing", DEFAULT.lineSpacing).forGetter(TextSettings::lineSpacing),
                Codec.BOOL.optionalFieldOf("shadow", DEFAULT.shadow).forGetter(TextSettings::shadow),
                Codec.FLOAT.optionalFieldOf("wave_amplitude", DEFAULT.waveAmplitude)
                        .forGetter(TextSettings::waveAmplitude),
                Codec.FLOAT.optionalFieldOf("wave_speed", DEFAULT.waveSpeed).forGetter(TextSettings::waveSpeed),
                Codec.FLOAT.optionalFieldOf("wave_frequency", DEFAULT.waveFrequency)
                        .forGetter(TextSettings::waveFrequency))
                .apply(i, TextSettings::new));
    }

    public record MusicSettings(ResourceLocation sound, float volume, float pitch, float start, float fadeIn,
                                boolean loop, SoundSource source, boolean stopGameMusic) {

        private static final Codec<SoundSource> SOURCE_CODEC = Codec.STRING.comapFlatMap(
                name -> Arrays.stream(SoundSource.values()).filter(source -> source.getName().equals(name)).findFirst()
                        .map(DataResult::success)
                        .orElseGet(() -> DataResult.error(() -> "Unknown sound source '" + name + "'")),
                SoundSource::getName);

        public static final Codec<MusicSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("sound").forGetter(MusicSettings::sound),
                Codec.FLOAT.optionalFieldOf("volume", 1f).forGetter(MusicSettings::volume),
                Codec.FLOAT.optionalFieldOf("pitch", 1f).forGetter(MusicSettings::pitch),
                Codec.FLOAT.optionalFieldOf("start", 0f).forGetter(MusicSettings::start),
                Codec.FLOAT.optionalFieldOf("fade_in", 4f).forGetter(MusicSettings::fadeIn),
                Codec.BOOL.optionalFieldOf("loop", true).forGetter(MusicSettings::loop),
                SOURCE_CODEC.optionalFieldOf("source", SoundSource.MUSIC).forGetter(MusicSettings::source),
                Codec.BOOL.optionalFieldOf("stop_game_music", true).forGetter(MusicSettings::stopGameMusic))
                .apply(i, MusicSettings::new));
    }

    public record SoundSettings(ResourceLocation id, float volume, float pitch, float pitchVariance,
                                int everyNLetters) {

        public static final Codec<SoundSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("id").forGetter(SoundSettings::id),
                Codec.FLOAT.optionalFieldOf("volume", 0.25f).forGetter(SoundSettings::volume),
                Codec.FLOAT.optionalFieldOf("pitch", 1f).forGetter(SoundSettings::pitch),
                Codec.FLOAT.optionalFieldOf("pitch_variance", 0.1f).forGetter(SoundSettings::pitchVariance),
                Codec.INT.optionalFieldOf("every_n_letters", 2).forGetter(SoundSettings::everyNLetters))
                .apply(i, SoundSettings::new));
    }
}
