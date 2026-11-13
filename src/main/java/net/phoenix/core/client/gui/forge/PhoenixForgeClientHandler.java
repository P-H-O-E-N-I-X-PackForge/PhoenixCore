package net.phoenix.core.client.gui.forge;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.rebirth.S2CRebirthStatePacket;

@OnlyIn(Dist.CLIENT)
public final class PhoenixForgeClientHandler {

    private PhoenixForgeClientHandler() {}

    public static void accept(S2CRebirthStatePacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PhoenixForgeScreen open && open.pos().equals(msg.forge())) {
            open.update(msg);
        } else if (msg.open()) {
            PhoenixForgeScreen screen = new PhoenixForgeScreen(msg);
            mc.setScreen(screen);
            if (msg.ignited()) screen.update(msg);
        }
    }
}
