package net.phoenix.core.client.gui.honey;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.common.machine.multiblock.electric.honey.S2CHoneyLatticePacket;

@OnlyIn(Dist.CLIENT)
public final class HoneyLatticeClientHandler {

    private HoneyLatticeClientHandler() {}

    public static void accept(S2CHoneyLatticePacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof HoneyLatticeScreen open && open.pos().equals(msg.pos())) {
            open.update(msg);
        } else if (msg.open()) {
            mc.setScreen(new HoneyLatticeScreen(msg));
        }
    }
}
