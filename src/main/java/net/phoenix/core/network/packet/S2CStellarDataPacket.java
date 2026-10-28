package net.phoenix.core.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.common.machine.multiblock.stellar.StellarForgeState;
import net.phoenix.core.common.machine.multiblock.stellar.StellarPlasma;
import net.phoenix.core.common.machine.multiblock.stellar.StellarTint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** The Stellar Forge's plasma recipes and dyes, sent to a client so the recipe viewer can show them. */
public class S2CStellarDataPacket implements CustomPacketPayload {

    public static final Type<S2CStellarDataPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "s2c_stellar_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CStellarDataPacket> STREAM_CODEC = StreamCodec
            .ofMember(S2CStellarDataPacket::encode, S2CStellarDataPacket::new);

    private final List<StellarPlasma.Recipe> plasmas;
    private final List<Map.Entry<ResourceLocation, StellarTint.Tint>> tints;

    public S2CStellarDataPacket(List<StellarPlasma.Recipe> plasmas,
                                List<Map.Entry<ResourceLocation, StellarTint.Tint>> tints) {
        this.plasmas = plasmas;
        this.tints = tints;
    }

    public S2CStellarDataPacket(RegistryFriendlyByteBuf buf) {
        this.plasmas = new ArrayList<>();
        int n = buf.readVarInt();
        for (int i = 0; i < n; i++) {
            plasmas.add(new StellarPlasma.Recipe(buf.readResourceLocation(), buf.readResourceLocation(), buf.readUtf(),
                    StellarForgeState.Stage.values()[buf.readVarInt()], buf.readFloat(), buf.readFloat(), buf.readDouble(),
                    buf.readDouble(), buf.readFloat(), buf.readDouble(), buf.readFloat()));
        }
        this.tints = new ArrayList<>();
        int m = buf.readVarInt();
        for (int i = 0; i < m; i++) {
            ResourceLocation id = buf.readResourceLocation();
            ResourceLocation fluid = buf.readBoolean() ? buf.readResourceLocation() : null;
            ResourceLocation tag = buf.readBoolean() ? buf.readResourceLocation() : null;
            tints.add(Map.entry(id, new StellarTint.Tint(fluid, tag, buf.readVarInt(), buf.readInt(), buf.readBoolean(),
                    buf.readBoolean())));
        }
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(plasmas.size());
        for (var r : plasmas) {
            buf.writeResourceLocation(r.id());
            buf.writeResourceLocation(r.fluid());
            buf.writeUtf(r.name());
            buf.writeVarInt(r.body().ordinal());
            buf.writeFloat(r.heatMin());
            buf.writeFloat(r.heatMax());
            buf.writeDouble(r.minMass());
            buf.writeDouble(r.rate());
            buf.writeFloat(r.damageStability());
            buf.writeDouble(r.damageMass());
            buf.writeFloat(r.damageHeat());
        }
        buf.writeVarInt(tints.size());
        for (var entry : tints) {
            var t = entry.getValue();
            buf.writeResourceLocation(entry.getKey());
            buf.writeBoolean(t.fluid() != null);
            if (t.fluid() != null) buf.writeResourceLocation(t.fluid());
            buf.writeBoolean(t.tag() != null);
            if (t.tag() != null) buf.writeResourceLocation(t.tag());
            buf.writeVarInt(t.amount());
            buf.writeInt(t.color());
            buf.writeBoolean(t.star());
            buf.writeBoolean(t.blackHole());
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CStellarDataPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            StellarPlasma.setClientCopy(msg.plasmas);
            StellarTint.setClientCopy(msg.tints);
            if (net.neoforged.fml.ModList.get().isLoaded("emi")) {
                try {
                    dev.emi.emi.runtime.EmiReloadManager.reload();
                } catch (Throwable ignored) {
                    // the recipe viewer reads the new lists when it next reloads
                }
            }
        });
    }
}
