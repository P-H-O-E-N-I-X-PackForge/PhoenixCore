package net.phoenix.core.client.nullwell;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class NullWellClientHandler {

    private NullWellClientHandler() {}

    public static void open(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof NullWellScreen open && open.pos().equals(pos)) return;
        mc.setScreen(new NullWellScreen(pos));
    }
}
