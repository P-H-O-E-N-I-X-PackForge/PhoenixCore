package net.phoenix.core.client.event;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.client.gui.screen.ColorRadialMenuScreen;
import net.phoenix.core.client.keybind.PhoenixKeybinds;
import net.phoenix.core.common.item.ChameleonSprayCanBehaviour;
import net.phoenix.core.common.item.ChameleonSprayCanItem;
import net.phoenix.core.integration.recipe_helper.RecipeBuilderMenu;
import net.phoenix.core.integration.recipe_helper.RecipeBuilderScreen;
import net.phoenix.core.network.client.ClientSoundHandler;
import net.phoenix.core.network.packet.SelectColorPacket;

@EventBusSubscriber(modid = PhoenixCore.MOD_ID, value = Dist.CLIENT)
public class ClientTickHandler {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        ClientSoundHandler.tickActiveStreams(mc.player);

        if (PhoenixKeybinds.SPRAY_CAN_MENU.consumeClick()) {
            ItemStack stack = mc.player.getMainHandItem();
            if (stack.getItem() instanceof ChameleonSprayCanItem) {
                mc.setScreen(new ColorRadialMenuScreen(InteractionHand.MAIN_HAND));
            }
        }

        while (PhoenixKeybinds.CINDER_ATLAS_RADIAL.consumeClick()) {
            if (mc.screen != null) continue;
            InteractionHand hand = mc.player.getMainHandItem()
                    .getItem() instanceof net.phoenix.core.common.item.cinder.CinderAtlasItem ?
                            InteractionHand.MAIN_HAND :
                            mc.player.getOffhandItem()
                                    .getItem() instanceof net.phoenix.core.common.item.cinder.CinderAtlasItem ?
                                            InteractionHand.OFF_HAND : null;
            if (hand != null) {
                mc.setScreen(new net.phoenix.core.client.gui.cinder.CinderAtlasRadialScreen(hand));
            }
        }

        while (PhoenixKeybinds.PLACE_ITEM.consumeClick()) {
            if (mc.screen != null || mc.player.getMainHandItem().isEmpty()) continue;
            if (mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit &&
                    hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                net.neoforged.neoforge.network.PacketDistributor
                        .sendToServer(new net.phoenix.core.network.packet.C2SPlaceItemPacket(
                                hit.getBlockPos(), hit.getDirection(), hit.getLocation()));
            }
        }

        while (PhoenixKeybinds.TOGGLE_DISCIPLINE_SKY.consumeClick()) {
            net.phoenix.core.integration.conflux.dimension.sky.SkyManager manager = net.phoenix.core.integration.conflux.dimension.sky.SkyManager
                    .getInstance();
            manager.toggleSkyRendering();
            mc.player.displayClientMessage(Component.literal(
                    "[PhoenixCore] Discipline sky shaders: " + (manager.isSkyRenderingEnabled() ? "ON" : "OFF")), true);
        }

        while (PhoenixKeybinds.SHOW_SHADER_PROFILER.consumeClick()) {
            net.phoenix.core.client.worldfx.ShaderProfiler.enabled = !net.phoenix.core.client.worldfx.ShaderProfiler.enabled;
            if (!net.phoenix.core.client.worldfx.ShaderProfiler.enabled) {
                net.phoenix.core.client.worldfx.ShaderProfiler.clear();
            }
            mc.player.displayClientMessage(Component.literal("[PhoenixCore] Shader profiler HUD: " +
                    (net.phoenix.core.client.worldfx.ShaderProfiler.enabled ? "ON" : "OFF")), true);
        }

        while (PhoenixKeybinds.OPEN_RECIPE_BUILDER.consumeClick()) {
            if (mc.screen == null) {
                RecipeBuilderMenu menu = new RecipeBuilderMenu(0, mc.player.getInventory());
                mc.setScreen(
                        new RecipeBuilderScreen(menu, mc.player.getInventory(), Component.literal("Recipe Builder")));
            }
        }

        while (PhoenixKeybinds.OPEN_EMI_FAVORITE_PAGES.consumeClick()) {
            mc.setScreen(new net.phoenix.core.client.emi.EmiFavoritePagesScreen(mc.screen));
        }

        while (PhoenixKeybinds.OPEN_ASTRAL_CODEX.consumeClick()) {
            if (mc.screen == null) {
                net.phoenix.core.integration.astral.client.AstralCodexScreen.open();
            }
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        if (mc.options.keyShift.isDown()) {
            ItemStack stack = mc.player.getMainHandItem();
            if (stack.getItem() instanceof ChameleonSprayCanItem) {
                double scrollDelta = event.getScrollDeltaY();
                event.setCanceled(true);

                var order = ChameleonSprayCanBehaviour.CREATIVE_ORDER;
                var current = ChameleonSprayCanBehaviour.getColor(stack);
                int position = -1;
                for (int i = 0; current != null && i < order.length; i++) {
                    if (order[i] == current) position = i;
                }
                int next = position + (scrollDelta > 0 ? 1 : -1);
                if (next < -1) next = order.length - 1;
                if (next >= order.length) next = -1;
                int nextColor = next == -1 ? -1 : order[next].ordinal();

                PacketDistributor.sendToServer(new SelectColorPacket(InteractionHand.MAIN_HAND, nextColor));
                mc.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.1f, 1.5f + (next * 0.05f));
            }
        }
    }

    @SubscribeEvent
    public static void onRightClick(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (!event.isUseItem()) return;
        ItemStack stack = mc.player.getMainHandItem();
        if (!(stack.getItem() instanceof ChameleonSprayCanItem)) return;

        if (!mc.player.isShiftKeyDown()) return;
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.MISS) return;

        event.setCanceled(true);
        mc.setScreen(new ColorRadialMenuScreen(InteractionHand.MAIN_HAND));
    }

    @SubscribeEvent
    public static void onPickBlock(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isPickBlock()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        InteractionHand hand;
        if (mc.player.getMainHandItem().getItem() instanceof ChameleonSprayCanItem) hand = InteractionHand.MAIN_HAND;
        else if (mc.player.getOffhandItem().getItem() instanceof ChameleonSprayCanItem) hand = InteractionHand.OFF_HAND;
        else return;
        event.setCanceled(true);

        if (!(mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit) ||
                hit.getType() != HitResult.Type.BLOCK)
            return;
        var level = mc.player.level();
        var pos = hit.getBlockPos();
        var be = level.getBlockEntity(pos);
        Integer index = null;

        if (be instanceof appeng.api.implementations.blockentities.IColorableBlockEntity colorable) {
            var aeColor = colorable.getColor();
            index = aeColor == appeng.api.util.AEColor.TRANSPARENT ? Integer.valueOf(-1) : aeColor.dye.ordinal();
        } else if (be instanceof com.gregtechceu.gtceu.api.pipenet.IPipeNode<?, ?> pipe) {
            index = pipe.isPainted() ? colorFromRgb(pipe.getPaintingColor()) : Integer.valueOf(-1);
        } else if (be instanceof com.gregtechceu.gtceu.api.blockentity.IPaintable paintable) {
            index = paintable.isPainted() ? colorFromRgb(paintable.getRealColor()) : Integer.valueOf(-1);
        } else if (be instanceof net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity shulker) {
            index = shulker.getColor() == null ? Integer.valueOf(-1) : shulker.getColor().ordinal();
        } else {
            index = ChameleonSprayCanBehaviour.pickedColorIndex(level.getBlockState(pos));
        }
        if (index != null) PacketDistributor.sendToServer(new SelectColorPacket(hand, index));
    }

    private static Integer colorFromRgb(int rgb) {
        for (net.minecraft.world.item.DyeColor color : net.minecraft.world.item.DyeColor.values()) {
            if (color.getMapColor().col == rgb) return color.ordinal();
        }
        return null;
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        ItemStack stack = event.getItemStack();
        if (stack.getItem() instanceof ChameleonSprayCanItem) {

        }
    }
}
