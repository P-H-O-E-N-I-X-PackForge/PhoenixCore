package net.phoenix.core.integration.conflux.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.phoenix.core.integration.conflux.panoptic.S2CPanopticStatePacket;

@OnlyIn(Dist.CLIENT)
public final class PanopticClientHandler {

    private PanopticClientHandler() {}

    public static void solution(net.phoenix.core.integration.conflux.panoptic.S2CPanopticSolutionPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PanopticScreen open && open.controller().equals(msg.controller())) {
            open.showSolution(msg);
        }
    }

    public static void accept(S2CPanopticStatePacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PanopticScreen open && open.controller().equals(msg.controller())) {
            open.update(msg);
        } else if (msg.open()) {
            mc.setScreen(new PanopticScreen(msg));
        }
    }
}
