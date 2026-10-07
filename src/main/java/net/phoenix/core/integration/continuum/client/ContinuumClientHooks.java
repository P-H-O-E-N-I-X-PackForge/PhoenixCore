package net.phoenix.core.integration.continuum.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.integration.continuum.client.screen.ContinuumAscentScreen;
import net.phoenix.core.integration.continuum.client.screen.ContinuumMapScreen;
import net.phoenix.core.integration.continuum.common.ContinuumStateSnapshot;
import net.phoenix.core.integration.continuum.data.ContinuumData;

/** Entry points the network packets call on the client. Kept separate so the packets never name client screens. */
public final class ContinuumClientHooks {

    private ContinuumClientHooks() {}

    public static void openMap(BlockPos pad) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new ContinuumMapScreen(pad));
    }

    public static void acceptData(ContinuumData.Snapshot snapshot) {
        ContinuumData.acceptSync(snapshot);
    }

    public static void acceptState(ContinuumStateSnapshot snapshot) {
        ContinuumClientState.accept(snapshot);
    }

    /** The server's answer to a launch request. */
    public static void missionResult(boolean accepted, ResourceLocation destination, BlockPos pad, String message) {
        Minecraft mc = Minecraft.getInstance();
        if (accepted) {
            mc.setScreen(new ContinuumAscentScreen(destination, pad));
            return;
        }

        if (mc.screen instanceof ContinuumMapScreen map) {
            map.flashMessage(message);
        } else if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal(message).withStyle(ChatFormatting.RED), true);
        }
    }
}
