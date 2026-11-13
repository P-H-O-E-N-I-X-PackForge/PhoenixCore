package net.phoenix.core.client.collider;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ColliderClientHandler {

    private ColliderClientHandler() {}

    public static void open(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ColliderScreen open && open.pos().equals(pos)) return;
        mc.setScreen(new ColliderScreen(pos));
    }
}
