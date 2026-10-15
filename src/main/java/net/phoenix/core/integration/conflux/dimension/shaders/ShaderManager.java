package net.phoenix.core.integration.conflux.dimension.shaders;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL20;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ShaderManager {

    private static final ShaderManager INSTANCE = new ShaderManager();
    private final Map<String, CompiledShaderProgram> shaders = new HashMap<>();
    private final FullscreenQuadRenderer quadRenderer = new FullscreenQuadRenderer();
    private ResourceManager resourceManager;

    public static ShaderManager getInstance() {
        return INSTANCE;
    }

    public void init(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
        quadRenderer.init();
        loadAllShaders();
    }

    private void loadAllShaders() {
        try {

            loadShader("phoenix_glow", "core/phoenix_glow");

            loadShader("sculk_biolum", "core/sculk_biolum");
            loadShader("sound_ripples", "core/sound_ripples");

            loadShader("void_space", "core/void_space");
            loadShader("gravity_bridge", "core/gravity_bridge");

            loadShader("neon_glow", "core/neon_glow");
            loadShader("industrial_sky", "core/industrial_sky");

            loadShader("color_inversion", "core/color_inversion");
            loadShader("reality_distortion", "core/reality_distortion");

            System.out.println("[PhoenixCore] Loaded " + shaders.size() + " shaders");
        } catch (Exception e) {
            System.err.println("[PhoenixCore] Error loading shaders: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadShader(String name, String path) throws IOException {
        String vertexSource = loadShaderSource(
                ResourceLocation.fromNamespaceAndPath("phoenixcore", "shaders/core/basic.vsh"));

        String fragmentSource = loadShaderSource(new ResourceLocation("phoenixcore", "shaders/" + path + ".fsh"));

        if (vertexSource == null || fragmentSource == null) {
            System.err.println("[PhoenixCore] Failed to load shader: " + name);
            return;
        }

        int program = compileShaderProgram(name, vertexSource, fragmentSource);
        if (program > 0) {
            CompiledShaderProgram shader = new CompiledShaderProgram(program, name);
            shaders.put(name, shader);
            System.out.println("[PhoenixCore] Compiled shader: " + name);
        }
    }

    @Nullable
    private String loadShaderSource(ResourceLocation location) {
        try {
            Resource resource = resourceManager.getResource(location).orElse(null);
            if (resource == null) {
                System.err.println("[PhoenixCore] Shader not found: " + location);
                return null;
            }

            byte[] bytes = resource.open().readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[PhoenixCore] Error loading shader: " + location + " - " + e.getMessage());
            return null;
        }
    }

    private int compileShaderProgram(String name, String vertexSource, String fragmentSource) {
        try {
            return compileGLSL(vertexSource, fragmentSource);
        } catch (Exception e) {
            System.err.println("[PhoenixCore] Failed to compile shader " + name + ": " + e.getMessage());
            return 0;
        }
    }

    private int compileGLSL(String vertexSource, String fragmentSource) {
        int vertexShader = compileShader(GL20.GL_VERTEX_SHADER, vertexSource);
        int fragmentShader = compileShader(GL20.GL_FRAGMENT_SHADER, fragmentSource);

        if (vertexShader == 0 || fragmentShader == 0) {
            return 0;
        }

        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertexShader);
        GL20.glAttachShader(program, fragmentShader);

        GL20.glBindAttribLocation(program, 0, "Position");
        GL20.glBindAttribLocation(program, 1, "UV0");
        GL20.glLinkProgram(program);

        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == 0) {
            System.err.println("[PhoenixCore] Shader link error: " + GL20.glGetProgramInfoLog(program));
            GL20.glDeleteProgram(program);
            return 0;
        }

        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);

        return program;
    }

    private int compileShader(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);

        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == 0) {
            String typeName = type == GL20.GL_VERTEX_SHADER ? "vertex" : "fragment";
            System.err
                    .println("[PhoenixCore] " + typeName + " shader compile error: " + GL20.glGetShaderInfoLog(shader));
            GL20.glDeleteShader(shader);
            return 0;
        }

        return shader;
    }

    @Nullable
    public CompiledShaderProgram getShader(String name) {
        return shaders.get(name);
    }

    public void useShader(String name) {
        CompiledShaderProgram shader = getShader(name);
        if (shader != null) {
            shader.use();
        }
    }

    public void stopUsingShader() {
        GL20.glUseProgram(0);
    }

    public FullscreenQuadRenderer getQuadRenderer() {
        return quadRenderer;
    }

    public void cleanup() {
        for (CompiledShaderProgram shader : shaders.values()) {
            shader.delete();
        }
        shaders.clear();
        quadRenderer.cleanup();
    }

    public void setUniform(String shaderName, String uniformName, float value) {
        CompiledShaderProgram shader = getShader(shaderName);
        if (shader != null) {
            shader.setUniform1f(uniformName, value);
        }
    }

    public void setUniform2f(String shaderName, String uniformName, float x, float y) {
        CompiledShaderProgram shader = getShader(shaderName);
        if (shader != null) {
            shader.setUniform2f(uniformName, x, y);
        }
    }

    public void setUniform3f(String shaderName, String uniformName, float x, float y, float z) {
        CompiledShaderProgram shader = getShader(shaderName);
        if (shader != null) {
            shader.setUniform3f(uniformName, x, y, z);
        }
    }

    public void setUniform4f(String shaderName, String uniformName, float x, float y, float z, float w) {
        CompiledShaderProgram shader = getShader(shaderName);
        if (shader != null) {
            shader.setUniform4f(uniformName, x, y, z, w);
        }
    }
}
