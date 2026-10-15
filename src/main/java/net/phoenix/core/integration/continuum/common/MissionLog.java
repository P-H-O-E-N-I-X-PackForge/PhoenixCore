package net.phoenix.core.integration.continuum.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public final class MissionLog {

    private MissionLog() {}

    public static final int LIMIT = 120;

    public enum Kind {

        LAUNCH,
        LANDED,
        FAILED,
        STRANDED,
        RESCUED,
        LOST,
        COLLECTED,
        STATION,
        COMET;

        public boolean problem() {
            return this == FAILED || this == STRANDED || this == LOST;
        }
    }

    public record Entry(long time, Kind kind, ResourceLocation body, String text) {

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("Time", time);
            tag.putByte("Kind", (byte) kind.ordinal());
            tag.putString("Body", body.toString());
            tag.putString("Text", text);
            return tag;
        }

        public static Entry load(CompoundTag tag) {
            int ordinal = Math.max(0, Math.min(tag.getByte("Kind"), Kind.values().length - 1));
            return new Entry(tag.getLong("Time"), Kind.values()[ordinal], ResourceLocation.parse(tag.getString("Body")),
                    tag.getString("Text"));
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeLong(time);
            buf.writeByte(kind.ordinal());
            buf.writeResourceLocation(body);
            buf.writeUtf(text, 300);
        }

        public static Entry read(FriendlyByteBuf buf) {
            long time = buf.readLong();
            int ordinal = Math.max(0, Math.min(buf.readByte(), Kind.values().length - 1));
            return new Entry(time, Kind.values()[ordinal], buf.readResourceLocation(), buf.readUtf(300));
        }
    }
}
