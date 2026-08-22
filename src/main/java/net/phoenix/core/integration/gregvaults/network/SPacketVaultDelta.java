package net.phoenix.core.integration.gregvaults.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.phoenix.core.integration.gregvaults.client.ClientVaultPacketHandlers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SPacketVaultDelta {

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

    public static void encode(SPacketVaultDelta packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.containerId);
        buf.writeShort(packet.entries.size());
        for (Entry e : packet.entries) {
            buf.writeByte(e.type());
            buf.writeVarInt(e.slot());
            if (e.type() == TYPE_COUNT) {
                buf.writeVarInt(e.count());
            } else if (e.type() == TYPE_FULL) {
                buf.writeItem(e.stack());
            }
        }
    }

    public static SPacketVaultDelta decode(FriendlyByteBuf buf) {
        int containerId = buf.readVarInt();
        int size = buf.readShort();
        List<Entry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            byte type = buf.readByte();
            int slot = buf.readVarInt();
            if (type == TYPE_COUNT) {
                entries.add(new Entry(TYPE_COUNT, slot, ItemStack.EMPTY, buf.readVarInt()));
            } else if (type == TYPE_FULL) {
                ItemStack stack = buf.readItem();
                entries.add(new Entry(TYPE_FULL, slot, stack, stack.getCount()));
            } else {
                entries.add(new Entry(TYPE_REMOVED, slot, ItemStack.EMPTY, 0));
            }
        }
        return new SPacketVaultDelta(containerId, entries);
    }

    public static void handle(SPacketVaultDelta packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientVaultPacketHandlers.handleDelta(packet)));
        ctx.get().setPacketHandled(true);
    }
}
