package net.phoenix.core.client.renderer.cinema.cutscene.background;

import net.phoenix.core.PhoenixCore;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraftforge.fml.loading.FMLPaths;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Compiles fragment shaders for cutscene backgrounds at runtime (ported from Phoenix Chronicles'
 * DynamicShaderManager). Shaders come from either:
 * <ul>
 * <li>a resource pack: {@code assets/<namespace>/cutscene_shaders/<path>.frag}, reloaded with F3+T</li>
 * <li>the config folder: {@code config/phoenixcore/shaders/<name>.frag}, hot-reloaded when the file changes</li>
 * </ul>
 * Shadertoy-style sources (defining {@code mainImage(out vec4, in vec2)}) are wrapped automatically and get
 * {@code iTime}, {@code iResolution}, {@code iMouse}; the cutscene fade is applied for you. Any other source is used
 * as-is and receives {@code in vec2 texCoord} plus the same uniforms, and {@code iAlpha} for the fade.
 */
public final class CutsceneShaders {

    private CutsceneShaders() {}

    public static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve(PhoenixCore.MOD_ID).resolve("shaders");
    private static final long HOT_RELOAD_COOLDOWN_MS = 1000L;

    private static final String VERTEX_SOURCE = """
            #version 150
            in vec3 Position;
            in vec2 UV0;
            uniform mat4 ModelViewMat;
            uniform mat4 ProjMat;
            out vec2 texCoord;
            void main() {
                gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
                texCoord = UV0;
            }
            """;

    /** An extra float uniform (1-4 components) declared by a background's {@code "uniforms"}. */
    public record Uniform(String name, int count) {}

    private record Entry(long sourceMtime, long lastChecked, @Nullable ShaderInstance instance) {}

    private static final Map<String, Entry> CACHE = new HashMap<>();

    /** Closes every compiled shader. Called on resource reload. */
    public static void clear() {
        CACHE.values().forEach(entry -> {
            if (entry.instance() != null) entry.instance().close();
        });
        CACHE.clear();
    }

    public static @Nullable ShaderInstance fromResource(ResourceLocation id, List<Uniform> uniforms) {
        String key = "res:" + id + signature(uniforms);
        Entry cached = CACHE.get(key);
        if (cached != null) return cached.instance();

        ResourceLocation file = id.withPath(path -> "cutscene_shaders/" + path + ".frag");
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(file);
        ShaderInstance instance = null;
        if (resource.isEmpty()) {
            PhoenixCore.LOGGER.error("Cutscene shader {} not found (expected {})", id, file);
        } else {
            try (InputStream stream = resource.get().open()) {
                String source = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                instance = compile(id.toString(), source, resource.get().source(), uniforms);
            } catch (IOException e) {
                PhoenixCore.LOGGER.error("Failed to read cutscene shader {}", file, e);
            }
        }
        CACHE.put(key, new Entry(0, 0, instance));
        return instance;
    }

    public static @Nullable ShaderInstance fromConfig(String name, List<Uniform> uniforms) {
        Path file = CONFIG_DIR.resolve(name + ".frag").normalize();
        if (!file.startsWith(CONFIG_DIR)) return null;

        String key = "cfg:" + name + signature(uniforms);
        Entry cached = CACHE.get(key);
        long now = System.currentTimeMillis();
        if (cached != null && now - cached.lastChecked() < HOT_RELOAD_COOLDOWN_MS) return cached.instance();

        long mtime;
        try {
            mtime = Files.getLastModifiedTime(file).toMillis();
        } catch (IOException e) {
            if (cached == null) PhoenixCore.LOGGER.error("Cutscene shader file {} not found", file);
            CACHE.put(key, new Entry(-1, now, cached != null ? cached.instance() : null));
            return cached != null ? cached.instance() : null;
        }
        if (cached != null && cached.sourceMtime() == mtime) {
            CACHE.put(key, new Entry(mtime, now, cached.instance()));
            return cached.instance();
        }

        ShaderInstance instance = null;
        try {
            String source = Files.readString(file, StandardCharsets.UTF_8);
            instance = compile(name, source, Minecraft.getInstance().getVanillaPackResources(), uniforms);
        } catch (IOException e) {
            PhoenixCore.LOGGER.error("Failed to read cutscene shader {}", file, e);
        }
        if (instance == null && cached != null && cached.instance() != null) {
            // Keep showing the last good version while the file is being edited.
            CACHE.put(key, new Entry(mtime, now, cached.instance()));
            return cached.instance();
        }
        if (cached != null && cached.instance() != null) cached.instance().close();
        CACHE.put(key, new Entry(mtime, now, instance));
        return instance;
    }

    /** Draws a full-screen quad with the shader, filling in the standard uniforms. */
    public static void draw(GuiGraphics graphics, ShaderInstance shader, CutsceneBackground.BackgroundContext context,
                            float timeScale, boolean opaque, Map<String, List<Float>> uniforms) {
        graphics.flush();
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        float pixelWidth = (float) (context.width() * guiScale), pixelHeight = (float) (context.height() * guiScale);

        shader.safeGetUniform("iTime").set(context.time() * timeScale);
        shader.safeGetUniform("iResolution").set(pixelWidth, pixelHeight, 1f);
        shader.safeGetUniform("iMouse").set((float) (context.mouseX() * guiScale),
                pixelHeight - (float) (context.mouseY() * guiScale), 0f, 0f);
        shader.safeGetUniform("iAlpha").set(context.alpha());
        shader.safeGetUniform("iOpaque").set(opaque ? 1f : 0f);
        uniforms.forEach((name, values) -> {
            var uniform = shader.safeGetUniform(name);
            switch (values.size()) {
                case 1 -> uniform.set(values.get(0));
                case 2 -> uniform.set(values.get(0), values.get(1));
                case 3 -> uniform.set(values.get(0), values.get(1), values.get(2));
                case 4 -> uniform.set(values.get(0), values.get(1), values.get(2), values.get(3));
                default -> {}
            }
        });

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(() -> shader);
        Matrix4f matrix = graphics.pose().last().pose();
        int w = context.width(), h = context.height();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(matrix, 0, h, 0).uv(0, 1).endVertex();
        buffer.vertex(matrix, w, h, 0).uv(1, 1).endVertex();
        buffer.vertex(matrix, w, 0, 0).uv(1, 0).endVertex();
        buffer.vertex(matrix, 0, 0, 0).uv(0, 0).endVertex();
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.disableBlend();
    }

    // ---------------------------------------------------------------- compilation

    private static @Nullable ShaderInstance compile(String id, String userSource, PackResources pack,
                                                    List<Uniform> uniforms) {
        String fragmentSource = wrapShadertoy(userSource);
        String name = "cutscene_" + id.replaceAll("[^a-zA-Z0-9_]", "_") + "_" +
                Integer.toHexString((fragmentSource + signature(uniforms)).hashCode());

        Map<ResourceLocation, Resource> files = Map.of(
                coreFile(name, ".json"), memory(pack, programJson(name, uniforms)),
                coreFile(name, ".vsh"), memory(pack, VERTEX_SOURCE),
                coreFile(name, ".fsh"), memory(pack, fragmentSource));
        try {
            return new ShaderInstance(ResourceProvider.fromMap(files), PhoenixCore.id(name),
                    DefaultVertexFormat.POSITION_TEX);
        } catch (Exception e) {
            PhoenixCore.LOGGER.error("Failed to compile cutscene shader '{}'", id, e);
            return null;
        }
    }

    private static String wrapShadertoy(String userSource) {
        if (!userSource.contains("mainImage")) return userSource;
        return """
                #version 150
                uniform float iTime;
                uniform vec3 iResolution;
                uniform vec4 iMouse;
                uniform float iAlpha;
                uniform float iOpaque;
                in vec2 texCoord;
                out vec4 forgeborn_outColor;

                %s

                void main() {
                    vec4 fragColor = vec4(0.0);
                    vec2 fragCoord = vec2(texCoord.x, 1.0 - texCoord.y) * iResolution.xy;
                    mainImage(fragColor, fragCoord);
                    float alpha = iOpaque > 0.5 ? 1.0 : fragColor.a;
                    forgeborn_outColor = vec4(fragColor.rgb, alpha * iAlpha);
                }
                """.formatted(userSource);
    }

    private static String programJson(String name, List<Uniform> extra) {
        String extraUniforms = extra.stream()
                .map(u -> ",\n    { \"name\": \"%s\", \"type\": \"float\", \"count\": %d, \"values\": [%s] }"
                        .formatted(u.name(), u.count(), String.join(", ", java.util.Collections.nCopies(u.count(), "0.0"))))
                .collect(Collectors.joining());
        return """
                {
                  "blend": { "func": "add", "srcrgb": "srcalpha", "dstrgb": "1-srcalpha" },
                  "vertex": "%1$s:%2$s",
                  "fragment": "%1$s:%2$s",
                  "attributes": ["Position", "UV0"],
                  "samplers": [],
                  "uniforms": [
                    { "name": "ModelViewMat", "type": "matrix4x4", "count": 16, "values": [1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0] },
                    { "name": "ProjMat", "type": "matrix4x4", "count": 16, "values": [1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0] },
                    { "name": "iTime", "type": "float", "count": 1, "values": [0.0] },
                    { "name": "iResolution", "type": "float", "count": 3, "values": [1.0, 1.0, 1.0] },
                    { "name": "iMouse", "type": "float", "count": 4, "values": [0.0, 0.0, 0.0, 0.0] },
                    { "name": "iAlpha", "type": "float", "count": 1, "values": [1.0] },
                    { "name": "iOpaque", "type": "float", "count": 1, "values": [1.0] }%3$s
                  ]
                }
                """.formatted(PhoenixCore.MOD_ID, name, extraUniforms);
    }

    private static String signature(List<Uniform> uniforms) {
        return uniforms.isEmpty() ? "" :
                uniforms.stream().map(u -> u.name() + u.count()).collect(Collectors.joining(",", "[", "]"));
    }

    private static ResourceLocation coreFile(String name, String extension) {
        return PhoenixCore.id("shaders/core/" + name + extension);
    }

    private static Resource memory(PackResources pack, String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        return new Resource(pack, () -> new ByteArrayInputStream(bytes));
    }
}
