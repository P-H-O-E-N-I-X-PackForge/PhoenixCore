package net.phoenix.core.network;

import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.phoenix.core.network.packet.*;
import net.phoenix.core.shop.network.C2SAddShopEntryPacket;
import net.phoenix.core.shop.network.C2SBuyShopEntryPacket;
import net.phoenix.core.shop.network.C2SRemoveShopEntryPacket;
import net.phoenix.core.shop.network.S2CShopSyncPacket;

@SuppressWarnings("removal")
public class PhoenixNetwork {

    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(SelectColorPacket.TYPE, SelectColorPacket.STREAM_CODEC, SelectColorPacket::handle);

        registrar.playToServer(C2STeslaDischargePacket.TYPE, C2STeslaDischargePacket.STREAM_CODEC,
                C2STeslaDischargePacket::handle);

        registrar.playToServer(UpdateWingSettingsPacket.TYPE, UpdateWingSettingsPacket.STREAM_CODEC,
                (msg, ctx) -> msg.handle(ctx));

        registrar.playToServer(PacketPhoenixModeSync.TYPE, PacketPhoenixModeSync.STREAM_CODEC,
                PacketPhoenixModeSync::handle);

        registrar.playToServer(CPacketChangeManipulatorMode.TYPE, CPacketChangeManipulatorMode.STREAM_CODEC,
                CPacketChangeManipulatorMode::handle);

        registrar.playToServer(CPacketManipulatorAction.TYPE, CPacketManipulatorAction.STREAM_CODEC,
                CPacketManipulatorAction::handle);

        registrar.playToServer(PacketRecipeBuilderGenerate.TYPE, PacketRecipeBuilderGenerate.STREAM_CODEC,
                PacketRecipeBuilderGenerate::handle);

        registrar.playToServer(SelectChromaticCodePacket.TYPE, SelectChromaticCodePacket.STREAM_CODEC,
                SelectChromaticCodePacket::handle);

        registrar.playToServer(C2SSelectSoundPacket.TYPE, C2SSelectSoundPacket.STREAM_CODEC,
                C2SSelectSoundPacket::handle);

        registrar.playToServer(C2SSoundMetadataPacket.TYPE, C2SSoundMetadataPacket.STREAM_CODEC,
                C2SSoundMetadataPacket::handle);

        registrar.playToClient(S2CPlaySoundPacket.TYPE, S2CPlaySoundPacket.STREAM_CODEC, S2CPlaySoundPacket::handle);

        registrar.playToClient(S2CPlayStreamPacket.TYPE, S2CPlayStreamPacket.STREAM_CODEC, S2CPlayStreamPacket::handle);

        registrar.playToServer(C2SToggleTeslaModePacket.TYPE, C2SToggleTeslaModePacket.STREAM_CODEC,
                C2SToggleTeslaModePacket::handle);

        registrar.playToClient(S2CShopSyncPacket.TYPE, S2CShopSyncPacket.STREAM_CODEC, S2CShopSyncPacket::handle);

        registrar.playToServer(C2SBuyShopEntryPacket.TYPE, C2SBuyShopEntryPacket.STREAM_CODEC,
                C2SBuyShopEntryPacket::handle);

        registrar.playToServer(C2SAddShopEntryPacket.TYPE, C2SAddShopEntryPacket.STREAM_CODEC,
                C2SAddShopEntryPacket::handle);

        registrar.playToServer(C2SRemoveShopEntryPacket.TYPE, C2SRemoveShopEntryPacket.STREAM_CODEC,
                C2SRemoveShopEntryPacket::handle);

        registrar.playToServer(C2SCinemaScreenConfigPacket.TYPE, C2SCinemaScreenConfigPacket.STREAM_CODEC,
                C2SCinemaScreenConfigPacket::handle);

        registrar.playToServer(C2SCinemaActionPacket.TYPE, C2SCinemaActionPacket.STREAM_CODEC,
                C2SCinemaActionPacket::handle);

        registrar.playToServer(C2SCinderConfigPacket.TYPE, C2SCinderConfigPacket.STREAM_CODEC,
                C2SCinderConfigPacket::handle);

        registrar.playToServer(C2SCinderSetTargetPacket.TYPE, C2SCinderSetTargetPacket.STREAM_CODEC,
                C2SCinderSetTargetPacket::handle);

        registrar.playToServer(C2SCinderCommitPacket.TYPE, C2SCinderCommitPacket.STREAM_CODEC,
                C2SCinderCommitPacket::handle);

        registrar.playToClient(S2CCinderCommitRejectedPacket.TYPE, S2CCinderCommitRejectedPacket.STREAM_CODEC,
                S2CCinderCommitRejectedPacket::handle);

        registrar.playToServer(C2SCinderClearTargetPacket.TYPE, C2SCinderClearTargetPacket.STREAM_CODEC,
                C2SCinderClearTargetPacket::handle);

        registrar.playToServer(C2SCinderAtlasSetSlotTargetPacket.TYPE, C2SCinderAtlasSetSlotTargetPacket.STREAM_CODEC,
                C2SCinderAtlasSetSlotTargetPacket::handle);

        registrar.playToServer(C2SCinderAtlasClearSlotPacket.TYPE, C2SCinderAtlasClearSlotPacket.STREAM_CODEC,
                C2SCinderAtlasClearSlotPacket::handle);

        registrar.playToServer(C2SCinderAtlasRenameLoadoutPacket.TYPE, C2SCinderAtlasRenameLoadoutPacket.STREAM_CODEC,
                C2SCinderAtlasRenameLoadoutPacket::handle);

        registrar.playToServer(C2SCinderAtlasSetActiveLoadoutPacket.TYPE,
                C2SCinderAtlasSetActiveLoadoutPacket.STREAM_CODEC, C2SCinderAtlasSetActiveLoadoutPacket::handle);

        registrar.playToServer(C2SCinderAtlasSetActiveSlotPacket.TYPE, C2SCinderAtlasSetActiveSlotPacket.STREAM_CODEC,
                C2SCinderAtlasSetActiveSlotPacket::handle);

        registrar.playToServer(C2SCinderAtlasOpenCraftingTerminalPacket.TYPE,
                C2SCinderAtlasOpenCraftingTerminalPacket.STREAM_CODEC,
                C2SCinderAtlasOpenCraftingTerminalPacket::handle);

        registrar.playToServer(C2SCinderAtlasRequestMaterialsPacket.TYPE,
                C2SCinderAtlasRequestMaterialsPacket.STREAM_CODEC, C2SCinderAtlasRequestMaterialsPacket::handle);

        registrar.playToClient(S2CPlayCutscenePacket.TYPE, S2CPlayCutscenePacket.STREAM_CODEC,
                S2CPlayCutscenePacket::handle);

        registrar.playToServer(C2SCutsceneActionPacket.TYPE, C2SCutsceneActionPacket.STREAM_CODEC,
                C2SCutsceneActionPacket::handle);

        registrar.playToServer(C2SCutsceneRequestPacket.TYPE, C2SCutsceneRequestPacket.STREAM_CODEC,
                C2SCutsceneRequestPacket::handle);
        registerResonanceAndPhysics(registrar);
    }

    private static void registerResonanceAndPhysics(PayloadRegistrar registrar) {
        registrar.playToServer(C2SAttuneStartPacket.TYPE, C2SAttuneStartPacket.STREAM_CODEC,
                C2SAttuneStartPacket::handle);
        registrar.playToServer(C2SAttunePressPacket.TYPE, C2SAttunePressPacket.STREAM_CODEC,
                C2SAttunePressPacket::handle);
        registrar.playToClient(S2CAttuneStartPacket.TYPE, S2CAttuneStartPacket.STREAM_CODEC,
                S2CAttuneStartPacket::handle);
        registrar.playToClient(S2CAttuneResultPacket.TYPE, S2CAttuneResultPacket.STREAM_CODEC,
                S2CAttuneResultPacket::handle);
        registrar.playToClient(S2CPhysicsImpulsePacket.TYPE, S2CPhysicsImpulsePacket.STREAM_CODEC,
                S2CPhysicsImpulsePacket::handle);
        registrar.playToServer(C2SOpenCalibrationPacket.TYPE, C2SOpenCalibrationPacket.STREAM_CODEC,
                C2SOpenCalibrationPacket::handle);
        registrar.playToServer(C2SCalibratePacket.TYPE, C2SCalibratePacket.STREAM_CODEC, C2SCalibratePacket::handle);
        registrar.playToClient(S2COpenCalibrationPacket.TYPE, S2COpenCalibrationPacket.STREAM_CODEC,
                S2COpenCalibrationPacket::handle);
        registrar.playToClient(S2CCalibrationResultPacket.TYPE, S2CCalibrationResultPacket.STREAM_CODEC,
                S2CCalibrationResultPacket::handle);
    }
}
