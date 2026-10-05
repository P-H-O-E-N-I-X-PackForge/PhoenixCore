package net.phoenix.core.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.common.block.cinema.Cutscenes;

import java.util.function.Supplier;

/**
 * A cutscene choice asks to continue into another cutscene. Same as the player running {@code /cutscene open},
 * but it goes through the server so the new cutscene gets a session for its actions.
 */
public class C2SCutsceneRequestPacket {

    private final ResourceLocation cutscene;

    public C2SCutsceneRequestPacket(ResourceLocation cutscene) {
        this.cutscene = cutscene;
    }

    public C2SCutsceneRequestPacket(FriendlyByteBuf buf) {
        this.cutscene = buf.readResourceLocation();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(cutscene);
    }

    public static void handle(C2SCutsceneRequestPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player != null) Cutscenes.play(player, msg.cutscene);
        });
        ctx.get().setPacketHandled(true);
    }
}
