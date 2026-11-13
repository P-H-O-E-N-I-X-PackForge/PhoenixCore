package net.phoenix.core.client.infuser;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class InfuserClientHandler {

    private InfuserClientHandler() {}

    public static void open(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof InfuserScreen open && open.pos().equals(pos)) return;
        mc.setScreen(new InfuserScreen(pos));
    }
}
