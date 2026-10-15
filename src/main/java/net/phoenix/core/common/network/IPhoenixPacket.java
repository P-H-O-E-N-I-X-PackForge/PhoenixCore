package net.phoenix.core.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.NetworkEvent;

public interface IPhoenixPacket {

    void encode(FriendlyByteBuf buf);

    void handle(NetworkEvent.Context context);
}
