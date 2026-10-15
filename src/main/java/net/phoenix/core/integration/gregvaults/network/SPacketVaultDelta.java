package net.phoenix.core.integration.gregvaults.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.phoenix.core.integration.gregvaults.client.ClientVaultPacketHandlers;

import java.util.ArrayList;
import java.util.List;

public class SPacketVaultDelta implements CustomPacketPayload {

    public static final Type<SPacketVaultDelta> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "s_packet_vault_delta"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SPacketVaultDelta> STREAM_CODEC = StreamCodec
            .of((buf, msg) -> SPacketVaultDelta.encode(msg, buf), SPacketVaultDelta::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static final byte TYPE_COUNT = 0;
    public static final byte TYPE_FULL = 1;
    public static final byte TYPE_REMOVED = 2;

    public record Entry(byte type, int slot, ItemStack stack, int count) {}

    private final int containerId;
    private final List<Entry> entries;

    private SPacketVaultDelta(int containerId, List<Entry> entries) {
        this.containerId = containerId;
        this.entries = entries;
    }

    public int getContainerId() {
        return containerId;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public static class Builder {

        private final int containerId;
        private final List<Entry> entries = new ArrayList<>();

        public Builder(int containerId) {
            this.containerId = containerId;
        }

        public void addCountOnly(int slot, int count) {
            entries.add(new Entry(TYPE_COUNT, slot, ItemStack.EMPTY, count));
        }

        public void addFull(int slot, ItemStack stack) {
            entries.add(new Entry(TYPE_FULL, slot, stack.copy(), stack.getCount()));
        }

        public void addRemoved(int slot) {
            entries.add(new Entry(TYPE_REMOVED, slot, ItemStack.EMPTY, 0));
        }

        public boolean isEmpty() {
            return entries.isEmpty();
        }

        public SPacketVaultDelta build() {
            return new SPacketVaultDelta(containerId, entries);
        }
    }

    public static void encode(SPacketVaultDelta packet, RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(packet.containerId);
        buf.writeShort(packet.entries.size());
        for (Entry e : packet.entries) {
            buf.writeByte(e.type());
            buf.writeVarInt(e.slot());
            if (e.type() == TYPE_COUNT) {
                buf.writeVarInt(e.count());
            } else if (e.type() == TYPE_FULL) {
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, e.stack());
            }
        }
    }

    public static SPacketVaultDelta decode(RegistryFriendlyByteBuf buf) {
        int containerId = buf.readVarInt();
        int size = buf.readShort();
        List<Entry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            byte type = buf.readByte();
            int slot = buf.readVarInt();
            if (type == TYPE_COUNT) {
                entries.add(new Entry(TYPE_COUNT, slot, ItemStack.EMPTY, buf.readVarInt()));
            } else if (type == TYPE_FULL) {
                ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                entries.add(new Entry(TYPE_FULL, slot, stack, stack.getCount()));
            } else {
                entries.add(new Entry(TYPE_REMOVED, slot, ItemStack.EMPTY, 0));
            }
        }
        return new SPacketVaultDelta(containerId, entries);
    }

    public static void handle(SPacketVaultDelta packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) ClientVaultPacketHandlers.handleDelta(packet);
        });
    }
}
