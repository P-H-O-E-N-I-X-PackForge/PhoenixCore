package net.phoenix.core.client.renderer.cinema.cutscene;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.phoenix.core.client.renderer.cinema.cutscene.background.BackgroundEffects;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Optional;

/**
 * One page of a cutscene. In JSON a page is either plain text (any text component) or an object with a
 * {@code "content"} field plus any of the extras below:
 *
 * <pre>
 * {
 *   "id": "ask",                         // label other pages / choices can jump to
 *   "content": "Keep it, or let it burn?",
 *   "text_y": 0.4,                       // vertical centre of the text, 0..1 of the screen (default 0.5)
 *   "images": [ ... ],                   // see {@link CutsceneImage}
 *   "actions": ["phoenixcore:lore/saw_notebook"],   // server actions run when this page appears
 *   "choices": [                         // shown once the page is typed; the player must pick one
 *     {"text": "Keep it.", "actions": ["phoenixcore:lore/keep_notebook"], "goto": "kept"},
 *     {"text": "Let it burn.", "goto": "burned"},
 *     {"text": "Walk away.", "cutscene": "phoenixcore:lore/04_archive"},
 *     {"text": "...", "end": true}
 *   ],
 *   "goto": "after",                     // where to go after this page (default: the next page)
 *   "end": true                          // end the cutscene after this page
 * }
 * </pre>
 *
 * Actions are ids of server-side definitions in {@code data/<namespace>/cutscene_actions/}, see
 * {@code CutsceneActions}.
 */
public record CutscenePage(Optional<String> id, Component content, Optional<Float> textY, List<CutsceneImage> images,
                           List<ResourceLocation> actions, List<Choice> choices, Optional<String> gotoLabel,
                           boolean end) {

    private static final Codec<CutscenePage> OBJECT_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("id").forGetter(CutscenePage::id),
            ExtraCodecs.COMPONENT.fieldOf("content").forGetter(CutscenePage::content),
            Codec.FLOAT.optionalFieldOf("text_y").forGetter(CutscenePage::textY),
            CutsceneImage.CODEC.listOf().optionalFieldOf("images", List.of()).forGetter(CutscenePage::images),
            ResourceLocation.CODEC.listOf().optionalFieldOf("actions", List.of()).forGetter(CutscenePage::actions),
            Choice.CODEC.listOf().optionalFieldOf("choices", List.of()).forGetter(CutscenePage::choices),
            Codec.STRING.optionalFieldOf("goto").forGetter(CutscenePage::gotoLabel),
            Codec.BOOL.optionalFieldOf("end", false).forGetter(CutscenePage::end))
            .apply(i, CutscenePage::new));

    /** An object with "content" is a full page; anything else is read as plain page text. */
    public static final Codec<CutscenePage> CODEC = Codec.either(OBJECT_CODEC, ExtraCodecs.COMPONENT).xmap(
            either -> either.map(page -> page, CutscenePage::text),
            page -> page.isPlain() ? Either.right(page.content()) : Either.left(page));

    public static CutscenePage text(Component content) {
        return new CutscenePage(Optional.empty(), content, Optional.empty(), List.of(), List.of(), List.of(),
                Optional.empty(), false);
    }

    private boolean isPlain() {
        return id.isEmpty() && textY.isEmpty() && images.isEmpty() && actions.isEmpty() && choices.isEmpty() &&
                gotoLabel.isEmpty() && !end;
    }

    /**
     * A choice button. After its actions are sent, it jumps to {@code goto}, plays another {@code cutscene}, or
     * {@code end}s the cutscene; with none of those it continues to the next page.
     */
    public record Choice(Component text, List<ResourceLocation> actions, Optional<String> gotoLabel,
                         Optional<ResourceLocation> cutscene, boolean end) {

        public static final Codec<Choice> CODEC = RecordCodecBuilder.create(i -> i.group(
                ExtraCodecs.COMPONENT.fieldOf("text").forGetter(Choice::text),
                ResourceLocation.CODEC.listOf().optionalFieldOf("actions", List.of()).forGetter(Choice::actions),
                Codec.STRING.optionalFieldOf("goto").forGetter(Choice::gotoLabel),
                ResourceLocation.CODEC.optionalFieldOf("cutscene").forGetter(Choice::cutscene),
                Codec.BOOL.optionalFieldOf("end", false).forGetter(Choice::end))
                .apply(i, Choice::new));
    }

    /**
     * A picture on a page: exactly one of {@code texture}, {@code item} or {@code entity}.
     *
     * <pre>
     * {"texture": "phoenixcore:textures/gui/notebook.png", "x": 0.5, "y": 0.3, "width": 96, "height": 64,
     *  "region": {"u": 0, "v": 0, "width": 32, "height": 32, "texture_width": 64, "texture_height": 64},
     *  "tint": "#FFFFFFFF"}
     * {"item": "minecraft:writable_book", "x": 0.5, "y": 0.3, "scale": 4}
     * {"entity": "minecraft:player", "x": 0.5, "y": 0.8, "scale": 50, "rotation": 20}
     * </pre>
     *
     * {@code x, y} are screen fractions: the centre of textures and items, the feet of entities.
     * {@code "minecraft:player"} shows the player themselves. Sizes are in GUI pixels; {@code scale} multiplies
     * items (16 px) and entities. {@code rotation} spins entities in degrees per second, starting from {@code yaw}.
     * Textures fade in; items and entities fade up out of darkness.
     */
    public record CutsceneImage(Optional<ResourceLocation> texture, Optional<ResourceLocation> item,
                                Optional<ResourceLocation> entity, float x, float y, int width, int height,
                                float scale, float yaw, float rotation, int tint, Optional<Region> region,
                                Animation animation) {

        public static final Codec<CutsceneImage> CODEC = ExtraCodecs.validate(RecordCodecBuilder.<CutsceneImage>create(i -> i.group(
                ResourceLocation.CODEC.optionalFieldOf("texture").forGetter(CutsceneImage::texture),
                ResourceLocation.CODEC.optionalFieldOf("item").forGetter(CutsceneImage::item),
                ResourceLocation.CODEC.optionalFieldOf("entity").forGetter(CutsceneImage::entity),
                Codec.FLOAT.optionalFieldOf("x", 0.5f).forGetter(CutsceneImage::x),
                Codec.FLOAT.optionalFieldOf("y", 0.3f).forGetter(CutsceneImage::y),
                Codec.INT.optionalFieldOf("width", 64).forGetter(CutsceneImage::width),
                Codec.INT.optionalFieldOf("height", 64).forGetter(CutsceneImage::height),
                Codec.FLOAT.optionalFieldOf("scale", 0f).forGetter(CutsceneImage::scale),
                Codec.FLOAT.optionalFieldOf("yaw", 0f).forGetter(CutsceneImage::yaw),
                Codec.FLOAT.optionalFieldOf("rotation", 0f).forGetter(CutsceneImage::rotation),
                BackgroundEffects.COLOR.optionalFieldOf("tint", 0xFFFFFFFF).forGetter(CutsceneImage::tint),
                Region.CODEC.optionalFieldOf("region").forGetter(CutsceneImage::region),
                Animation.CODEC.optionalFieldOf("animation", Animation.DEFAULT).forGetter(CutsceneImage::animation))
                .apply(i, CutsceneImage::new)),
                image -> (image.texture.isPresent() ? 1 : 0) + (image.item.isPresent() ? 1 : 0) +
                        (image.entity.isPresent() ? 1 : 0) == 1 ? DataResult.success(image) :
                                DataResult.error(() -> "A cutscene image needs exactly one of texture, item or entity"));

        /** Item scale, or entity scale in pixels per block, with the type's default when unset. */
        public float scaleOr(float fallback) {
            return scale > 0 ? scale : fallback;
        }
    }

    /** Part of a texture sheet to draw; without it the whole texture is stretched over width x height. */
    public record Region(int u, int v, int width, int height, int textureWidth, int textureHeight) {

        public static final Codec<Region> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("u", 0).forGetter(Region::u),
                Codec.INT.optionalFieldOf("v", 0).forGetter(Region::v),
                Codec.INT.fieldOf("width").forGetter(Region::width),
                Codec.INT.fieldOf("height").forGetter(Region::height),
                Codec.INT.optionalFieldOf("texture_width", 256).forGetter(Region::textureWidth),
                Codec.INT.optionalFieldOf("texture_height", 256).forGetter(Region::textureHeight))
                .apply(i, Region::new));
    }

    /**
     * How an image appears: after {@code delay} seconds it fades in over {@code fade_in} while sliding from
     * ({@code slide_x}, {@code slide_y}) pixels away; {@code bob} makes it float up and down by that many pixels.
     */
    public record Animation(float delay, float fadeIn, float slideX, float slideY, float bob) {

        public static final Animation DEFAULT = new Animation(0f, 0.8f, 0f, 8f, 0f);

        public static final Codec<Animation> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.FLOAT.optionalFieldOf("delay", DEFAULT.delay).forGetter(Animation::delay),
                Codec.FLOAT.optionalFieldOf("fade_in", DEFAULT.fadeIn).forGetter(Animation::fadeIn),
                Codec.FLOAT.optionalFieldOf("slide_x", DEFAULT.slideX).forGetter(Animation::slideX),
                Codec.FLOAT.optionalFieldOf("slide_y", DEFAULT.slideY).forGetter(Animation::slideY),
                Codec.FLOAT.optionalFieldOf("bob", DEFAULT.bob).forGetter(Animation::bob))
                .apply(i, Animation::new));
    }
}
