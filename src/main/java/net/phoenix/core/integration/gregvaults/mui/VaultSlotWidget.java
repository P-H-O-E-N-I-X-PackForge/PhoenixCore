package net.phoenix.core.integration.gregvaults.mui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import brachy.modularui.api.widget.Interactable;
import brachy.modularui.screen.RichTooltip;
import brachy.modularui.screen.viewport.ModularGuiContext;
import brachy.modularui.theme.WidgetThemeEntry;
import brachy.modularui.widgets.slot.ItemSlot;
import brachy.modularui.widgets.slot.ModularSlot;

public final class VaultSlotWidget extends ItemSlot {

    private final VaultSession session;
    private final int windowIndex;

    public VaultSlotWidget(VaultSession session, int windowIndex) {
        this.session = session;
        this.windowIndex = windowIndex;
        setItemHook(stack -> session.isStacked() ? stack.copyWithCount(1) : stack);
    }

    @Override
    public Interactable.Result onMousePressed(int mouseButton) {
        ModularSlot slot = getSlot();
        if (session.isStacked() && slot != null && !slot.getItem().isEmpty() && session.sync != null &&
                !net.minecraft.client.gui.screens.Screen.hasShiftDown()) {

            ItemStack carried = session.player.containerMenu.getCarried();
            if (carried.isEmpty() || ItemStack.isSameItemSameComponents(carried, slot.getItem())) {
                session.sync.requestPickup(windowIndex, mouseButton == 1);
                return Interactable.Result.SUCCESS;
            }
        }
        return super.onMousePressed(mouseButton);
    }

    @Override
    public void drawOverlay(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        super.drawOverlay(context, widgetTheme);
        ModularSlot slot = getSlot();
        if (slot == null) return;
        var g = context.getGraphics();

        if (!slot.isActive()) {
            g.fill(1, 1, 17, 17, 0x99111111);
            return;
        }

        ItemStack stack = slot.getItem();
        if (!session.isStacked() || stack.isEmpty()) return;

        String text = shortCount(stack.getCount());
        var font = Minecraft.getInstance().font;
        g.pose().pushPose();
        g.pose().translate(1, 1, 200);
        g.pose().scale(0.5f, 0.5f, 0.5f);
        int tx = (int) ((16f - font.width(text) * 0.5f) / 0.5f);
        int ty = (int) ((16f - 7f * 0.5f) / 0.5f);
        g.drawString(font, text, tx, ty, 0xFFFFFF, true);
        g.pose().popPose();
    }

    @Override
    public void buildTooltip(ItemStack stack, RichTooltip tooltip) {
        super.buildTooltip(stack, tooltip);
        if (session.isStacked() && !stack.isEmpty()) {
            tooltip.addLine(Component.literal(String.format("%,d in the vault", stack.getCount()))
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    public static String shortCount(long count) {
        if (count < 10_000L) return Long.toString(count);
        if (count < 1_000_000L) return (count / 1_000L) + "k";
        if (count < 1_000_000_000L) return (count / 1_000_000L) + "m";
        return (count / 1_000_000_000L) + "b";
    }
}
