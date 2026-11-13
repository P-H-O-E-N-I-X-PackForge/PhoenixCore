package net.phoenix.core.common.machine.multiblock.electric.honey;

import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class HoneyNetwork {

    private HoneyNetwork() {}

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(S2CHoneyLatticePacket.TYPE, S2CHoneyLatticePacket.STREAM_CODEC,
                S2CHoneyLatticePacket::handle);
        registrar.playToServer(C2SHoneyLatticePacket.TYPE, C2SHoneyLatticePacket.STREAM_CODEC,
                C2SHoneyLatticePacket::handle);
    }
}
