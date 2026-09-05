package net.phoenix.core.client.renderer.cinema;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.phoenix.core.common.block.cinema.CinemaScreenBlockEntity;
import net.phoenix.core.network.PhoenixNetwork;
import net.phoenix.core.network.packet.C2SCinemaScreenConfigPacket;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = "phoenixcore", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class CinemaScrollHandler {

    private static final float SCALE_STEP = 0.002f;
    private static final float MIN_SCALE = 0.005f;
    private static final float MAX_SCALE = 0.05f;

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !mc.player.isShiftKeyDown()) return;
        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;

        BlockPos pos = hit.getBlockPos();
        if (!(mc.level.getBlockEntity(pos) instanceof CinemaScreenBlockEntity screen)) return;

        event.setCanceled(true);
        boolean up = event.getScrollDelta() > 0;
        boolean editBackground = Screen.hasAltDown();

        if (editBackground) {
            CinemaGroupUtil.GroupLayout layout = CinemaGroupUtil.getLayout(mc.level, pos);
            BlockPos anchorPos = layout.anchor() != null ? layout.anchor() : pos;
            if (mc.level.getBlockEntity(anchorPos) instanceof CinemaScreenBlockEntity anchorScreen) {
                pos = anchorPos;
                screen = anchorScreen;
            }
        }

        List<String> lines = new ArrayList<>();
        for (Component line : screen.getLines()) lines.add(line.getString());

        int color = screen.getTextColor();
        float scale = screen.getTextScale();
        int alignOrdinal = screen.getTextAlign().ordinal();
        int backgroundOrdinal = screen.getBackground().ordinal();

        if (editBackground) {
            CinemaScreenBlockEntity.Background[] values = CinemaScreenBlockEntity.Background.values();
            backgroundOrdinal = (backgroundOrdinal + (up ? 1 : -1) + values.length) % values.length;
        } else if (Screen.hasControlDown()) {
            scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale + (up ? SCALE_STEP : -SCALE_STEP)));
        } else {
            CinemaScreenBlockEntity.TextAlign[] values = CinemaScreenBlockEntity.TextAlign.values();
            alignOrdinal = (alignOrdinal + (up ? 1 : -1) + values.length) % values.length;
        }

        PhoenixNetwork.CHANNEL.sendToServer(
                new C2SCinemaScreenConfigPacket(pos, lines, color, scale, alignOrdinal, backgroundOrdinal));
    }
}
