package net.phoenix.core.integration.gregpacks.common.inventory;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.phoenix.core.integration.gregpacks.network.CPacketKeyState;
import net.phoenix.core.integration.gregpacks.network.CPacketOpenOmniPack;
import net.phoenix.core.integration.gregpacks.network.GregPacksNetwork;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;

@OnlyIn(Dist.CLIENT)
public class OmniPackKeybind {

    private static boolean[] lastKeys = new boolean[6];

    public static final KeyMapping OPEN_PACK = new KeyMapping(
            "key.gregpacks.open_omnipack",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "key.categories.gregpacks");

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_PACK);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            Options o = mc.options;
            boolean[] keys = {
                    o.keyJump.isDown(),
                    o.keyUp.isDown(),
                    o.keyDown.isDown(),
                    o.keyLeft.isDown(),
                    o.keyRight.isDown(),
                    o.keyShift.isDown()
            };
            if (!Arrays.equals(keys, lastKeys)) {
                lastKeys = keys.clone();
                GregPacksNetwork.CHANNEL.sendToServer(new CPacketKeyState(keys));
            }

            while (OPEN_PACK.consumeClick()) {
                GregPacksNetwork.CHANNEL.sendToServer(new CPacketOpenOmniPack());
            }
        }
    }
}
