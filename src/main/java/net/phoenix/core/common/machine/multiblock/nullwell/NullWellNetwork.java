package net.phoenix.core.common.machine.multiblock.nullwell;

import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NullWellNetwork {

    private NullWellNetwork() {}

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(S2CNullWellOpenPacket.TYPE, S2CNullWellOpenPacket.STREAM_CODEC,
                S2CNullWellOpenPacket::handle);
        registrar.playToServer(C2SNullWellPacket.TYPE, C2SNullWellPacket.STREAM_CODEC, C2SNullWellPacket::handle);
    }
}
