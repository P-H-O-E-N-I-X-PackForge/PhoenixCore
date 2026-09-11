package net.phoenix.core.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> client: the commit failed validation - show the reason and clear the client's preview.
 * When the rejection was specifically "not enough materials" (as opposed to too-far-away or
 * nothing-configured), this arms a short window on the client's {@code CinderPreviewState} letting a
 * second right-click at the same spot force a partial build instead of hard-failing again - see
 * {@code CinderPreviewState#armForceBuild}.
 */
public class S2CCinderCommitRejectedPacket {

    private final String reason;
    private final boolean insufficientMaterials;

    public S2CCinderCommitRejectedPacket(String reason, boolean insufficientMaterials) {
        this.reason = reason;
        this.insufficientMaterials = insufficientMaterials;
    }

    public S2CCinderCommitRejectedPacket(FriendlyByteBuf buf) {
        this.reason = buf.readUtf(256);
        this.insufficientMaterials = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(reason, 256);
        buf.writeBoolean(insufficientMaterials);
    }

    public static void handle(S2CCinderCommitRejectedPacket msg, Supplier<NetworkEvent.Context> ctxGetter) {
        NetworkEvent.Context ctx = ctxGetter.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            var state = net.phoenix.core.client.cinder.CinderPreviewState.INSTANCE;
            if (msg.insufficientMaterials) {
                state.armForceBuild();
            }
            state.cancel();
            var player = Minecraft.getInstance().player;
            if (player != null) {
                player.displayClientMessage(Component.literal(msg.reason), true);
            }
        }));
        ctx.setPacketHandled(true);
    }
}
