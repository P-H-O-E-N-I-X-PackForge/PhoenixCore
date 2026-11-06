package net.phoenix.core.integration.gregpacks.network;

import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@SuppressWarnings("all")
public class GregPacksNetwork {

    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(CPacketOpenOmniPack.TYPE, CPacketOpenOmniPack.STREAM_CODEC, CPacketOpenOmniPack::handle);

        registrar.playToServer(CPacketFluidInteract.TYPE, CPacketFluidInteract.STREAM_CODEC,
                CPacketFluidInteract::handle);

        registrar.playToClient(SPacketFluidSync.TYPE, SPacketFluidSync.STREAM_CODEC, SPacketFluidSync::handle);

        registrar.playToClient(SPacketJetpackThrust.TYPE, SPacketJetpackThrust.STREAM_CODEC,
                SPacketJetpackThrust::handle);

        registrar.playToClient(net.phoenix.core.integration.drone.network.S2CDroneOverlayPacket.TYPE,
                net.phoenix.core.integration.drone.network.S2CDroneOverlayPacket.STREAM_CODEC,
                net.phoenix.core.integration.drone.network.S2CDroneOverlayPacket::handle);

        registrar.playToClient(net.phoenix.core.integration.drone.network.S2CDroneFleetPacket.TYPE,
                net.phoenix.core.integration.drone.network.S2CDroneFleetPacket.STREAM_CODEC,
                net.phoenix.core.integration.drone.network.S2CDroneFleetPacket::handle);

        registrar.playToClient(net.phoenix.core.integration.drone.network.S2CDroneAlertPacket.TYPE,
                net.phoenix.core.integration.drone.network.S2CDroneAlertPacket.STREAM_CODEC,
                net.phoenix.core.integration.drone.network.S2CDroneAlertPacket::handle);

        registrar.playToClient(net.phoenix.core.integration.drone.network.S2CDroneSearchPacket.TYPE,
                net.phoenix.core.integration.drone.network.S2CDroneSearchPacket.STREAM_CODEC,
                net.phoenix.core.integration.drone.network.S2CDroneSearchPacket::handle);

        registrar.playToServer(CPacketKeyState.TYPE, CPacketKeyState.STREAM_CODEC, CPacketKeyState::handle);
    }
}
