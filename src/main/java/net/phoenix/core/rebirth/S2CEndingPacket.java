package net.phoenix.core.rebirth;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.conflux.common.ending.Ending;
import net.phoenix.conflux.common.ending.Endings;
import net.phoenix.conflux.common.research.ResearchTeamHelper;
import net.phoenix.core.client.gui.forge.ClientEnding;

import java.util.List;

public record S2CEndingPacket(String id, String title, List<String> lines, int accent, int accent2)
        implements CustomPacketPayload {

    public static final Type<S2CEndingPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "s2c_ending"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CEndingPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, S2CEndingPacket::id,
            ByteBufCodecs.STRING_UTF8, S2CEndingPacket::title,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(8)), S2CEndingPacket::lines,
            ByteBufCodecs.VAR_INT, S2CEndingPacket::accent,
            ByteBufCodecs.VAR_INT, S2CEndingPacket::accent2,
            S2CEndingPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static S2CEndingPacket of(ServerPlayer player) {
        var level = player.serverLevel();
        return of(level.getServer(), Endings.resolve(level, ResearchTeamHelper.getTeamId(player)));
    }

    public static S2CEndingPacket of(net.minecraft.server.MinecraftServer server, Ending ending) {
        Endings.Text text = Endings.text(server, ending);
        int accent = Endings.accent(ending.to());
        int accent2 = Endings.accent(ending.from() != null ? ending.from() : ending.to());
        return new S2CEndingPacket(ending.id(), text.title(), text.animatic(), accent, accent2);
    }

    public static void send(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, of(player));
    }

    public static void handle(S2CEndingPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientEnding.set(msg));
    }
}
