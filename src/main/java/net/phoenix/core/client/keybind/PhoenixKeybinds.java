package net.phoenix.core.client.keybind;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public class PhoenixKeybinds {

    public static final KeyMapping OPEN_WING_GUI = new KeyMapping(
            "key.phoenixcore.wing_flight_gui",   // translation key
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_9,                  // Numpad 9
            "key.categories.phoenixcore"          // category
    );

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_WING_GUI);
    }
}
