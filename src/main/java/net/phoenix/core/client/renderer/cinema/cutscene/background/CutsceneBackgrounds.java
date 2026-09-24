package net.phoenix.core.client.renderer.cinema.cutscene.background;

import net.phoenix.core.PhoenixCore;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Registry and JSON format for cutscene backgrounds. Anywhere a background is expected, JSON accepts:
 * <ul>
 * <li>a string: a preset id, e.g. {@code "phoenixcore:prismatic"}. Presets come from
 * {@code assets/<namespace>/cutscene_backgrounds/<path>.json} or {@link #registerPreset} in code</li>
 * <li>a list: backgrounds drawn on top of each other (same as {@code "stack"})</li>
 * <li>an object with a {@code "type"}:
 * <ul>
 * <li>{@code layered}: builder-style effect layers, see {@link CutsceneBackgroundBuilder.Layered}</li>
 * <li>{@code shader}: {@code {"shader": "phoenixcore:dream_nebula"}} (resource pack) or {@code {"file": "name"}}
 * (config/phoenixcore/shaders), plus optional {@code time_scale}, {@code opaque}, {@code uniforms} and a
 * {@code fallback} background used if it fails to compile. See {@link CutsceneShaders}</li>
 * <li>{@code texture}: {@code {"texture": "ns:textures/...png", "tile_size": 32, "scroll_x": 4, "scroll_y": 0,
 * "tint": "#FFFFFFFF"}}. {@code tile_size} 0 stretches the image over the screen</li>
 * <li>{@code stack}: {@code {"backgrounds": [...]}}</li>
 * <li>{@code preset}: {@code {"id": "ns:path"}}</li>
 * </ul>
 * </li>
 * </ul>
 * Mods can add types with {@link #register(String, MapCodec)}.
 */
public final class CutsceneBackgrounds {

    private CutsceneBackgrounds() {}

    private static final Map<String, MapCodec<? extends CutsceneBackground.Data>> TYPES = new LinkedHashMap<>();
    private static final Map<ResourceLocation, CutsceneBackground> CODE_PRESETS = new LinkedHashMap<>();

    private static final Codec<CutsceneBackground.Data> TYPED_CODEC = ExtraCodecs.validate(Codec.STRING,
            type -> TYPES.containsKey(type) ? DataResult.success(type) :
                    DataResult.error(() -> "Unknown cutscene background type '" + type + "', known: " + TYPES.keySet()))
            .dispatch("type", CutsceneBackground.Data::type, type -> TYPES.get(type).codec());

    public static final Codec<CutsceneBackground.Data> CODEC = ExtraCodecs.lazyInitializedCodec(() -> Codec.either(
            ResourceLocation.CODEC,
            Codec.either(CutsceneBackgrounds.CODEC.listOf(), TYPED_CODEC))
            .xmap(CutsceneBackgrounds::fromEither, CutsceneBackgrounds::toEither));

    public static final ResourceLocation PRISMATIC = PhoenixCore.id("prismatic");

    static {
        register("layered", CutsceneBackgroundBuilder.Layered.CODEC);
        register("shader", Shader.CODEC);
        register("texture", Texture.CODEC);
        register("stack", Stack.CODEC);
        register("preset", Preset.CODEC);

        // Every hue, slowly drifting, with darker edges so text stays readable.
        registerPreset(PRISMATIC, CutsceneBackgroundBuilder.create()
                .layer(BackgroundEffects.hueCycle(0.03f, 0.6f, 0.3f, 0.12f))
                .layer(BackgroundEffects.vignette(0.6f))
                .build());
        registerPreset(PhoenixCore.id("aurora"), CutsceneBackgroundBuilder.create()
                .layer(BackgroundEffects.solid(0xFF06121A))
                .layer(BackgroundEffects.noise(0xFF33FFAA, 1.5f, 0.05f), 0.45f, BlendMode.ADD)
                .layer(BackgroundEffects.colorCycle(0.05f, 0xFF33FFAA, 0xFF33AAFF, 0xFFAA55FF), 0.35f, BlendMode.SCREEN)
                .layer(BackgroundEffects.sparkle(0xFFFFFFFF, 0.5f, 1.5f), 0.5f, BlendMode.ADD)
                .layer(BackgroundEffects.vignette(0.7f))
                .build());
        registerPreset(PhoenixCore.id("void"), CutsceneBackgroundBuilder.create()
                .layer(BackgroundEffects.solid(0xFF000000))
                .layer(BackgroundEffects.noise(0xFF3A1E5C, 1.2f, 0.03f), 0.6f)
                .layer(BackgroundEffects.sparkle(0xFFB9A8FF, 0.3f, 0.8f), 0.4f, BlendMode.ADD)
                .build());
        registerPreset(PhoenixCore.id("dream_nebula"), shader(PhoenixCore.id("dream_nebula"), preset(PRISMATIC)));
    }

    public static void register(String type, MapCodec<? extends CutsceneBackground.Data> codec) {
        TYPES.put(type, codec);
    }

    public static Set<String> types() {
        return TYPES.keySet();
    }

    /** Registers a background that JSON can refer to by id. JSON presets with the same id take priority. */
    public static void registerPreset(ResourceLocation id, CutsceneBackground background) {
        CODE_PRESETS.put(id, background);
    }

    public static Optional<CutsceneBackground> getPreset(ResourceLocation id) {
        CutsceneBackground fromJson = PresetLoader.INSTANCE.presets.get(id);
        return Optional.ofNullable(fromJson != null ? fromJson : CODE_PRESETS.get(id));
    }

    // ---------------------------------------------------------------- factories

    public static Preset preset(ResourceLocation id) {
        return new Preset(id);
    }

    public static Stack stack(CutsceneBackground.Data... backgrounds) {
        return new Stack(List.of(backgrounds));
    }

    public static Shader shader(ResourceLocation resourceShader, CutsceneBackground.Data fallback) {
        return new Shader(Optional.of(resourceShader), Optional.empty(), 1f, true, Map.of(), Optional.of(fallback));
    }

    public static Shader configShader(String fileName, CutsceneBackground.Data fallback) {
        return new Shader(Optional.empty(), Optional.of(fileName), 1f, true, Map.of(), Optional.of(fallback));
    }

    public static Texture texture(ResourceLocation texture, int tileSize, float scrollX, float scrollY) {
        return new Texture(texture, tileSize, scrollX, scrollY, 0xFFFFFFFF);
    }

    // ---------------------------------------------------------------- types

    public record Stack(List<CutsceneBackground.Data> backgrounds) implements CutsceneBackground.Data {

        public static final MapCodec<Stack> CODEC = ExtraCodecs.lazyInitializedCodec(() -> CutsceneBackgrounds.CODEC.listOf())
                .fieldOf("backgrounds").xmap(Stack::new, Stack::backgrounds);

        @Override
        public void render(GuiGraphics graphics, BackgroundContext context) {
            backgrounds.forEach(background -> background.render(graphics, context));
        }

        @Override
        public String type() {
            return "stack";
        }
    }

    public record Preset(ResourceLocation id) implements CutsceneBackground.Data {

        public static final MapCodec<Preset> CODEC = ResourceLocation.CODEC.fieldOf("id").xmap(Preset::new, Preset::id);
        private static final int MAX_DEPTH = 8;
        private static final Set<ResourceLocation> WARNED = new java.util.HashSet<>();
        private static int depth;

        @Override
        public void render(GuiGraphics graphics, BackgroundContext context) {
            Optional<CutsceneBackground> background = getPreset(id);
            if (background.isEmpty()) {
                if (WARNED.add(id)) PhoenixCore.LOGGER.warn("Unknown cutscene background preset {}", id);
                return;
            }
            if (depth >= MAX_DEPTH) {
                if (WARNED.add(id)) PhoenixCore.LOGGER.warn("Cutscene background preset {} refers to itself", id);
                return;
            }
            depth++;
            try {
                background.get().render(graphics, context);
            } finally {
                depth--;
            }
        }

        @Override
        public String type() {
            return "preset";
        }
    }

    public record Shader(Optional<ResourceLocation> shader, Optional<String> file, float timeScale, boolean opaque,
                         Map<String, List<Float>> uniforms, Optional<CutsceneBackground.Data> fallback)
            implements CutsceneBackground.Data {

        private static final Codec<List<Float>> UNIFORM_VALUES = Codec.either(Codec.FLOAT, Codec.FLOAT.listOf())
                .comapFlatMap(either -> either.map(v -> DataResult.success(List.of(v)),
                        list -> list.isEmpty() || list.size() > 4 ?
                                DataResult.error(() -> "Uniforms take 1 to 4 values") : DataResult.success(list)),
                        list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list));

        public static final MapCodec<Shader> CODEC = RecordCodecBuilder.<Shader>mapCodec(i -> i.group(
                ResourceLocation.CODEC.optionalFieldOf("shader").forGetter(Shader::shader),
                Codec.STRING.optionalFieldOf("file").forGetter(Shader::file),
                Codec.FLOAT.optionalFieldOf("time_scale", 1f).forGetter(Shader::timeScale),
                Codec.BOOL.optionalFieldOf("opaque", true).forGetter(Shader::opaque),
                Codec.unboundedMap(Codec.STRING, UNIFORM_VALUES).optionalFieldOf("uniforms", Map.of())
                        .forGetter(Shader::uniforms),
                ExtraCodecs.lazyInitializedCodec(() -> CutsceneBackgrounds.CODEC).optionalFieldOf("fallback")
                        .forGetter(Shader::fallback))
                .apply(i, Shader::new))
                .flatXmap(shader -> shader.shader().isPresent() != shader.file().isPresent() ?
                        DataResult.success(shader) :
                        DataResult.error(() -> "Shader background needs exactly one of 'shader' or 'file'"),
                        DataResult::success);

        @Override
        public void render(GuiGraphics graphics, BackgroundContext context) {
            List<CutsceneShaders.Uniform> declared = uniforms.entrySet().stream()
                    .map(e -> new CutsceneShaders.Uniform(e.getKey(), e.getValue().size()))
                    .sorted(Comparator.comparing(CutsceneShaders.Uniform::name))
                    .toList();
            ShaderInstance instance = shader.isPresent() ? CutsceneShaders.fromResource(shader.get(), declared) :
                    CutsceneShaders.fromConfig(file.orElseThrow(), declared);
            if (instance == null) {
                fallback.ifPresent(background -> background.render(graphics, context));
                return;
            }
            CutsceneShaders.draw(graphics, instance, context, timeScale, opaque, uniforms);
        }

        @Override
        public String type() {
            return "shader";
        }
    }

    public record Texture(ResourceLocation texture, int tileSize, float scrollX, float scrollY, int tint)
            implements CutsceneBackground.Data {

        public static final MapCodec<Texture> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                ResourceLocation.CODEC.fieldOf("texture").forGetter(Texture::texture),
                Codec.INT.optionalFieldOf("tile_size", 0).forGetter(Texture::tileSize),
                Codec.FLOAT.optionalFieldOf("scroll_x", 0f).forGetter(Texture::scrollX),
                Codec.FLOAT.optionalFieldOf("scroll_y", 0f).forGetter(Texture::scrollY),
                BackgroundEffects.COLOR.optionalFieldOf("tint", 0xFFFFFFFF).forGetter(Texture::tint))
                .apply(i, Texture::new));

        @Override
        public void render(GuiGraphics graphics, BackgroundContext context) {
            int w = context.width(), h = context.height();
            RenderSystem.enableBlend();
            graphics.setColor(((tint >> 16) & 0xFF) / 255f, ((tint >> 8) & 0xFF) / 255f, (tint & 0xFF) / 255f,
                    ((tint >>> 24) / 255f) * context.alpha());
            if (tileSize <= 0) {
                graphics.blit(texture, 0, 0, w, h, 0f, 0f, 1, 1, 1, 1);
            } else {
                float u = (context.time() * -scrollX) % tileSize, v = (context.time() * -scrollY) % tileSize;
                graphics.blit(texture, 0, 0, w, h, u < 0 ? u + tileSize : u, v < 0 ? v + tileSize : v, w, h,
                        tileSize, tileSize);
            }
            graphics.setColor(1f, 1f, 1f, 1f);
            RenderSystem.disableBlend();
        }

        @Override
        public String type() {
            return "texture";
        }
    }

    // ---------------------------------------------------------------- codec helpers

    private static CutsceneBackground.Data fromEither(
                                                      Either<ResourceLocation, Either<List<CutsceneBackground.Data>, CutsceneBackground.Data>> either) {
        return either.map(Preset::new, inner -> inner.map(Stack::new, typed -> typed));
    }

    private static Either<ResourceLocation, Either<List<CutsceneBackground.Data>, CutsceneBackground.Data>> toEither(
                                                                                                                     CutsceneBackground.Data background) {
        return background instanceof Preset preset ? Either.left(preset.id()) : Either.right(Either.right(background));
    }

    // ---------------------------------------------------------------- JSON presets

    /** Loads {@code assets/<namespace>/cutscene_backgrounds/*.json} and clears compiled shaders on reload. */
    public static final class PresetLoader extends SimpleJsonResourceReloadListener {

        public static final PresetLoader INSTANCE = new PresetLoader();

        private Map<ResourceLocation, CutsceneBackground> presets = Map.of();

        private PresetLoader() {
            super(new Gson(), "cutscene_backgrounds");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> jsons, ResourceManager resourceManager,
                             ProfilerFiller profiler) {
            CutsceneShaders.clear();
            Map<ResourceLocation, CutsceneBackground> loaded = new HashMap<>();
            jsons.forEach((id, json) -> CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error -> PhoenixCore.LOGGER.error("Failed to load cutscene background {}: {}",
                            id, error))
                    .ifPresent(background -> loaded.put(id, background)));
            presets = Map.copyOf(loaded);
        }
    }
}
