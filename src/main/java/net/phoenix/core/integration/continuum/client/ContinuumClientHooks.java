package net.phoenix.core.integration.continuum.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.core.integration.continuum.client.screen.ContinuumArrivalScreen;
import net.phoenix.core.integration.continuum.client.screen.ContinuumAscentScreen;
import net.phoenix.core.integration.continuum.client.screen.ContinuumMapScreen;
import net.phoenix.core.integration.continuum.client.screen.ContinuumTransitScreen;
import net.phoenix.core.integration.continuum.common.ContinuumStateSnapshot;
import net.phoenix.core.integration.continuum.common.Mission;
import net.phoenix.core.integration.continuum.data.ContinuumBody;
import net.phoenix.core.integration.continuum.data.ContinuumData;
import net.phoenix.core.integration.continuum.data.DiscoveryStage;

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

    public static void missionLanded(ContinuumStateSnapshot.MissionView mission) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ContinuumTransitScreen || mc.screen instanceof ContinuumArrivalScreen) return;

        boolean crash = mission.state() == Mission.State.FAILED;
        if (crash) ContinuumSounds.warning();
        else ContinuumSounds.chime();

        ContinuumBody body = ContinuumData.body(mission.destination());
        String name = body != null ? body.name() : mission.destination().getPath();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal(crash ?
                    "Mission to " + name + " failed. Open the map to collect the rocket." :
                    "Mission to " + name + " has landed. Open the map to collect.")
                    .withStyle(crash ? ChatFormatting.RED : ChatFormatting.AQUA), true);
        }
    }

    public static void outpostDamaged(ResourceLocation body) {
        ContinuumBody target = ContinuumData.body(body);
        String name = target != null ? target.name() : body.getPath();
        String message = "The outpost on " + name + " is damaged. Fly a repair run.";
        ContinuumSounds.warning();

        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ContinuumMapScreen map) {
            map.flashMessage(message);
        } else if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal(message).withStyle(ChatFormatting.RED), true);
        }
    }

    public static void loreUnlocked(ResourceLocation id, DiscoveryStage stage) {
        ContinuumBody body = ContinuumData.body(id);
        if (body == null || body.lore(stage).isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        String message = "Archive updated: " + body.name() + " (" + stage.label().toLowerCase(java.util.Locale.ROOT) +
                ")";
        ContinuumSounds.entry();
        if (mc.screen instanceof ContinuumMapScreen map) {
            map.flashMessage(message);
        } else if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal(message).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
    }

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
