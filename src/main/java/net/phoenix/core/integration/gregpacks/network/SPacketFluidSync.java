package net.phoenix.core.integration.gregpacks.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.integration.gregpacks.common.inventory.OmniPackMenu;

import java.util.function.Supplier;

public class SPacketFluidSync {

    private final String fluidKey;

    public SPacketFluidSync(String fluidKey) {
        this.fluidKey = fluidKey;
    }

    public static void encode(SPacketFluidSync msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.fluidKey);
    }

    public static SPacketFluidSync decode(FriendlyByteBuf buf) {
        return new SPacketFluidSync(buf.readUtf());
    }

    public static void handle(SPacketFluidSync msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> applySync(msg));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void applySync(SPacketFluidSync msg) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null && mc.player.containerMenu instanceof OmniPackMenu menu) {
            menu.setSyncedFluidKey(msg.fluidKey);
        }
    }
}
