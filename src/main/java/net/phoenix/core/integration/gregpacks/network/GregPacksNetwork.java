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

        registrar.playToServer(CPacketKeyState.TYPE, CPacketKeyState.STREAM_CODEC, CPacketKeyState::handle);
    }
}
