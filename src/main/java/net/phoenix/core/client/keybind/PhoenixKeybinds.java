package net.phoenix.core.client.keybind;

import net.minecraft.client.KeyMapping;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public class PhoenixKeybinds {

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

    public static final KeyMapping TOGGLE_DISCIPLINE_SKY = new KeyMapping(
            "key.phoenixcore.toggle_discipline_sky",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_5,
            "key.categories.phoenixcore");

    public static final KeyMapping SHOW_SHADER_PROFILER = new KeyMapping(
            "key.phoenixcore.show_shader_profiler",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_4,
            "key.categories.phoenixcore");

    public static final KeyMapping CINDER_ATLAS_RADIAL = new KeyMapping(
            "key.phoenixcore.cinder_atlas_radial",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_1,
            "key.categories.phoenixcore");

    /** Put the held item or block on the face you are looking at, scaled down. Rebind it in Controls. */
    public static final KeyMapping PLACE_ITEM = new KeyMapping(
            "key.phoenixcore.place_item",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.phoenixcore");

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(MANIPULATOR_MENU);
        event.register(OPEN_RECIPE_BUILDER);
        event.register(SPRAY_CAN_MENU);
        event.register(OPEN_EMI_FAVORITE_PAGES);
        event.register(OPEN_ASTRAL_CODEX);
        event.register(TOGGLE_DISCIPLINE_SKY);
        event.register(SHOW_SHADER_PROFILER);
        event.register(CINDER_ATLAS_RADIAL);
        event.register(PLACE_ITEM);
    }
}
