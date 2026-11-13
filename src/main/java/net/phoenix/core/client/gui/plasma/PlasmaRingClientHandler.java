package net.phoenix.core.client.gui.plasma;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.common.machine.multiblock.electric.plasma.S2CPlasmaRingPacket;

@OnlyIn(Dist.CLIENT)
public final class PlasmaRingClientHandler {

    private PlasmaRingClientHandler() {}

    public static void accept(S2CPlasmaRingPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PlasmaRingScreen open && open.pos().equals(msg.pos())) {
            open.update(msg);
        } else if (msg.open()) {
            mc.setScreen(new PlasmaRingScreen(msg));
        }
    }
}
