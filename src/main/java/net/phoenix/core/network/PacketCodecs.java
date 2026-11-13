package net.phoenix.core.network;

import net.minecraft.network.codec.StreamCodec;

import io.netty.buffer.ByteBuf;

public final class PacketCodecs {

    private PacketCodecs() {}

    public static final StreamCodec<ByteBuf, Character> CHAR = StreamCodec.of(
            (buf, c) -> buf.writeChar(c), buf -> buf.readChar());

    public static final StreamCodec<ByteBuf, Integer> BYTE_INT = StreamCodec.of(
            (buf, i) -> buf.writeByte(i), buf -> (int) buf.readByte());

    public static final StreamCodec<ByteBuf, net.minecraft.world.phys.Vec3> VEC3 = StreamCodec.of(
            (buf, v) -> {
                buf.writeDouble(v.x);
                buf.writeDouble(v.y);
                buf.writeDouble(v.z);
            }, buf -> new net.minecraft.world.phys.Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));

    public static <E extends Enum<E>> StreamCodec<ByteBuf, E> enumOrdinal(Class<E> type) {
        E[] values = type.getEnumConstants();
        return StreamCodec.of(
                (buf, e) -> net.minecraft.network.VarInt.write(buf, e.ordinal()),
                buf -> values[net.minecraft.network.VarInt.read(buf)]);
    }
}
