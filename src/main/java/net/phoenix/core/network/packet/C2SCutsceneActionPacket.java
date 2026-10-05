package net.phoenix.core.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.common.block.cinema.CutsceneActions;

import java.util.function.Supplier;

/** A cutscene asks the server to run one of its actions (validated by {@link CutsceneActions#trigger}). */
public class C2SCutsceneActionPacket {

    private final ResourceLocation cutscene;
    private final ResourceLocation action;

    public C2SCutsceneActionPacket(ResourceLocation cutscene, ResourceLocation action) {
        this.cutscene = cutscene;
        this.action = action;
    }

    public C2SCutsceneActionPacket(FriendlyByteBuf buf) {
        this.cutscene = buf.readResourceLocation();
        this.action = buf.readResourceLocation();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(cutscene);
        buf.writeResourceLocation(action);
    }

    public static void handle(C2SCutsceneActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player != null) CutsceneActions.trigger(player, msg.cutscene, msg.action);
        });
        ctx.get().setPacketHandled(true);
    }
}
