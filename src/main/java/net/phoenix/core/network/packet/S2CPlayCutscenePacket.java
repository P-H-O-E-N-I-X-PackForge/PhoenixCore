package net.phoenix.core.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Tells a client to play a cutscene from its {@code assets/<namespace>/cutscenes/} folder. */
public class S2CPlayCutscenePacket {

    private final ResourceLocation id;

    public S2CPlayCutscenePacket(ResourceLocation id) {
        this.id = id;
    }

    public S2CPlayCutscenePacket(FriendlyByteBuf buf) {
        this.id = buf.readResourceLocation();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(id);
    }

    public static void handle(S2CPlayCutscenePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (!FMLEnvironment.dist.isClient()) return;

            net.phoenix.core.client.renderer.cinema.cutscene.CutsceneManager.play(msg.id);
        });
        ctx.get().setPacketHandled(true);
    }
}
