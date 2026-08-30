package net.phoenix.core.client.keybind;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public class PhoenixKeybinds {

    public static final KeyMapping OPEN_WING_GUI = new KeyMapping(
            "key.phoenixcore.wing_flight_gui",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_9,
            "key.categories.phoenixcore");

    public static final KeyMapping TESLA_MODE = new KeyMapping(
            "key.phoenixcore.tesla_mode",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            "key.categories.phoenixcore");

    public static final KeyMapping TESLA_DISCHARGE = new KeyMapping(
            "key.phoenixcore.tesla_discharge",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_7,
            "key.categories.phoenixcore");

    public static final KeyMapping MANIPULATOR_MENU = new KeyMapping(
            "key.phoenixcore.manipulator_menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.phoenixcore");

    public static final KeyMapping OPEN_RECIPE_BUILDER = new KeyMapping(
            "key.phoenixcore.recipe_builder",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_8,
            "key.categories.phoenixcore");

    public static final KeyMapping SPRAY_CAN_MENU = new KeyMapping(
            "key.phoenixcore.spray_can_menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.phoenixcore");

    public static final KeyMapping OPEN_EMI_FAVORITE_PAGES = new KeyMapping(
            "key.phoenixcore.open_emi_favorite_pages",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.categories.phoenixcore");

    public static final KeyMapping OPEN_ASTRAL_CODEX = new KeyMapping(
            "key.phoenixcore.open_astral_codex",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_6,
            "key.categories.phoenixcore");

    // Quick A/B diagnostic for "is it the shaders?" - toggles every discipline sky shader
    // (nebula/black hole/sunflare/veins/etc.) off entirely without needing to leave the dimension
    // or rebuild anything, so FPS with vs. without can be compared directly. See
    // ClientTickHandler for the toggle handler and SkyManager#toggleSkyRendering.
    public static final KeyMapping TOGGLE_DISCIPLINE_SKY = new KeyMapping(
            "key.phoenixcore.toggle_discipline_sky",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_5,
            "key.categories.phoenixcore");

    // Shows ShaderProfiler's live per-shader ms readout in the corner - the finer-grained
    // "which one specifically" follow-up to TOGGLE_DISCIPLINE_SKY's blunter on/off A-B test.
    public static final KeyMapping SHOW_SHADER_PROFILER = new KeyMapping(
            "key.phoenixcore.show_shader_profiler",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_4,
            "key.categories.phoenixcore");

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_WING_GUI);
        event.register(TESLA_MODE);
        event.register(TESLA_DISCHARGE);
        event.register(MANIPULATOR_MENU);
        event.register(OPEN_RECIPE_BUILDER);
        event.register(SPRAY_CAN_MENU);
        event.register(OPEN_EMI_FAVORITE_PAGES);
        event.register(OPEN_ASTRAL_CODEX);
        event.register(TOGGLE_DISCIPLINE_SKY);
        event.register(SHOW_SHADER_PROFILER);
    }
}
