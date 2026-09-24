package net.phoenix.core.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.phoenix.core.network.packet.*;
import net.phoenix.core.shop.network.C2SAddShopEntryPacket;
import net.phoenix.core.shop.network.C2SBuyShopEntryPacket;
import net.phoenix.core.shop.network.C2SRemoveShopEntryPacket;
import net.phoenix.core.shop.network.S2CShopSyncPacket;

import java.util.Optional;

@SuppressWarnings("removal")
public class PhoenixNetwork {

    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath("phoenixcore", "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private static int id = 0;

    public static void init() {
        CHANNEL.registerMessage(id++,
                SelectColorPacket.class,
                SelectColorPacket::encode,
                SelectColorPacket::decode,
                SelectColorPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++,
                C2STeslaDischargePacket.class,
                C2STeslaDischargePacket::encode,
                C2STeslaDischargePacket::new,
                C2STeslaDischargePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++,
                UpdateWingSettingsPacket.class,
                UpdateWingSettingsPacket::encode,
                UpdateWingSettingsPacket::new,
                UpdateWingSettingsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++,
                PacketPhoenixModeSync.class,
                PacketPhoenixModeSync::encode,
                PacketPhoenixModeSync::decode,
                PacketPhoenixModeSync::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++,
                CPacketChangeManipulatorMode.class,
                CPacketChangeManipulatorMode::encode,
                CPacketChangeManipulatorMode::new,
                CPacketChangeManipulatorMode::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++,
                CPacketManipulatorAction.class,
                CPacketManipulatorAction::encode,
                CPacketManipulatorAction::new,
                CPacketManipulatorAction::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++,
                PacketRecipeBuilderGenerate.class,
                PacketRecipeBuilderGenerate::encode,
                PacketRecipeBuilderGenerate::decode,
                PacketRecipeBuilderGenerate::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++,
                SelectChromaticCodePacket.class,
                SelectChromaticCodePacket::encode,
                SelectChromaticCodePacket::decode,
                SelectChromaticCodePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++,
                C2SSelectSoundPacket.class,
                C2SSelectSoundPacket::encode,
                C2SSelectSoundPacket::new,
                C2SSelectSoundPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++,
                C2SSoundMetadataPacket.class,
                C2SSoundMetadataPacket::encode,
                C2SSoundMetadataPacket::new,
                C2SSoundMetadataPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++,
                S2CPlaySoundPacket.class,
                S2CPlaySoundPacket::encode,
                S2CPlaySoundPacket::new,
                S2CPlaySoundPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(id++,
                S2CPlayStreamPacket.class,
                S2CPlayStreamPacket::encode,
                S2CPlayStreamPacket::new,
                S2CPlayStreamPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(id++,
                C2SToggleTeslaModePacket.class,
                C2SToggleTeslaModePacket::encode,
                C2SToggleTeslaModePacket::new,
                C2SToggleTeslaModePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++,
                S2CShopSyncPacket.class,
                S2CShopSyncPacket::encode,
                S2CShopSyncPacket::decode,
                S2CShopSyncPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(id++,
                C2SBuyShopEntryPacket.class,
                C2SBuyShopEntryPacket::encode,
                C2SBuyShopEntryPacket::decode,
                C2SBuyShopEntryPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++,
                C2SAddShopEntryPacket.class,
                C2SAddShopEntryPacket::encode,
                C2SAddShopEntryPacket::decode,
                C2SAddShopEntryPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++,
                C2SRemoveShopEntryPacket.class,
                C2SRemoveShopEntryPacket::encode,
                C2SRemoveShopEntryPacket::decode,
                C2SRemoveShopEntryPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++,
                C2SCinemaScreenConfigPacket.class,
                C2SCinemaScreenConfigPacket::encode,
                C2SCinemaScreenConfigPacket::new,
                C2SCinemaScreenConfigPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++,
                C2SCinemaActionPacket.class,
                C2SCinemaActionPacket::encode,
                C2SCinemaActionPacket::new,
                C2SCinemaActionPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++,
                C2SCinderConfigPacket.class,
                C2SCinderConfigPacket::encode,
                C2SCinderConfigPacket::new,
                C2SCinderConfigPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++,
                C2SCinderSetTargetPacket.class,
                C2SCinderSetTargetPacket::encode,
                C2SCinderSetTargetPacket::new,
                C2SCinderSetTargetPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++,
                C2SCinderCommitPacket.class,
                C2SCinderCommitPacket::encode,
                C2SCinderCommitPacket::new,
                C2SCinderCommitPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++,
                S2CCinderCommitRejectedPacket.class,
                S2CCinderCommitRejectedPacket::encode,
                S2CCinderCommitRejectedPacket::new,
                S2CCinderCommitRejectedPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(id++,
                C2SCinderClearTargetPacket.class,
                C2SCinderClearTargetPacket::encode,
                C2SCinderClearTargetPacket::new,
                C2SCinderClearTargetPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++,
                C2SCinderAtlasSetSlotTargetPacket.class,
                C2SCinderAtlasSetSlotTargetPacket::encode,
                C2SCinderAtlasSetSlotTargetPacket::new,
                C2SCinderAtlasSetSlotTargetPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++,
                C2SCinderAtlasClearSlotPacket.class,
                C2SCinderAtlasClearSlotPacket::encode,
                C2SCinderAtlasClearSlotPacket::new,
                C2SCinderAtlasClearSlotPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++,
                C2SCinderAtlasRenameLoadoutPacket.class,
                C2SCinderAtlasRenameLoadoutPacket::encode,
                C2SCinderAtlasRenameLoadoutPacket::new,
                C2SCinderAtlasRenameLoadoutPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++,
                C2SCinderAtlasSetActiveLoadoutPacket.class,
                C2SCinderAtlasSetActiveLoadoutPacket::encode,
                C2SCinderAtlasSetActiveLoadoutPacket::new,
                C2SCinderAtlasSetActiveLoadoutPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++,
                C2SCinderAtlasSetActiveSlotPacket.class,
                C2SCinderAtlasSetActiveSlotPacket::encode,
                C2SCinderAtlasSetActiveSlotPacket::new,
                C2SCinderAtlasSetActiveSlotPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++,
                C2SCinderAtlasOpenCraftingTerminalPacket.class,
                C2SCinderAtlasOpenCraftingTerminalPacket::encode,
                C2SCinderAtlasOpenCraftingTerminalPacket::new,
                C2SCinderAtlasOpenCraftingTerminalPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++,
                C2SCinderAtlasRequestMaterialsPacket.class,
                C2SCinderAtlasRequestMaterialsPacket::encode,
                C2SCinderAtlasRequestMaterialsPacket::new,
                C2SCinderAtlasRequestMaterialsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++,
                S2CPlayCutscenePacket.class,
                S2CPlayCutscenePacket::encode,
                S2CPlayCutscenePacket::new,
                S2CPlayCutscenePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
}
