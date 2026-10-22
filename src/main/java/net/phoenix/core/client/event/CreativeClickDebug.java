package net.phoenix.core.client.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.phoenix.core.PhoenixCore;

/**
 * TEMPORARY: logs what happens to mouse presses and releases in any container screen (inventory, chests, creative) (is the event cancelled before or after
 * vanilla sees it, what is on the cursor, and vanilla's skipNextRelease flag), to find what makes the carried item vanish on
 * release. Remove once the bug is found.
 */
@EventBusSubscriber(modid = PhoenixCore.MOD_ID, value = Dist.CLIENT)
public final class CreativeClickDebug {

    private CreativeClickDebug() {}

    private static String state(Screen screen) {
        if (!(screen instanceof AbstractContainerScreen<?> c)) return "";
        String skip = "?";
        try {
            var f = AbstractContainerScreen.class.getDeclaredField("skipNextRelease");
            f.setAccessible(true);
            skip = String.valueOf(f.getBoolean(c));
        } catch (ReflectiveOperationException ignored) {}
        String quick = "?";
        try {
            var f = AbstractContainerScreen.class.getDeclaredField("isQuickCrafting");
            f.setAccessible(true);
            quick = String.valueOf(f.getBoolean(c));
        } catch (ReflectiveOperationException ignored) {}
        var player = Minecraft.getInstance().player;
        String menus = player == null ? "" : " playerContainerMenu.carried=" + player.containerMenu.getCarried() +
                " inventoryMenu.carried=" + player.inventoryMenu.getCarried() + " menuIsScreenMenu=" +
                (player.containerMenu == c.getMenu()) + " isQuickCrafting=" + quick;
        var slot = c.getSlotUnderMouse();
        return " screen=" + c.getClass().getSimpleName() + " gui=(" + c.getGuiLeft() + "," + c.getGuiTop() + ")" +
                " slotUnderMouse=" + (slot == null ? "none" : slot.index + ":" + slot.getItem()) +
                " carried=" + c.getMenu().getCarried() + menus + " skipNextRelease=" + skip;
    }

    private static void log(String what, ScreenEvent event, boolean cancelable) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) return;
        PhoenixCore.LOGGER.info("[CreativeClickDebug] {}{}{}", what,
                cancelable ? " cancelled=" + ((net.neoforged.bus.api.ICancellableEvent) event).isCanceled() : "",
                state(event.getScreen()));
    }

    /** Which widgets of the screen claim the point under the mouse: one of them may be swallowing the click. */
    private static String widgetsAt(Screen screen, double x, double y) {
        StringBuilder sb = new StringBuilder();
        for (var child : screen.children()) {
            if (child.isMouseOver(x, y)) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(child.getClass().getName());
            }
        }
        return sb.length() == 0 ? "none" : sb.toString();
    }

    /** What vanilla's own hit test says at this point (it can differ from the slot the screen shows as hovered). */
    private static String hitTest(Screen screen, double x, double y) {
        if (!(screen instanceof AbstractContainerScreen<?> c)) return "";
        StringBuilder sb = new StringBuilder();
        try {
            var find = AbstractContainerScreen.class.getDeclaredMethod("findSlot", double.class, double.class);
            find.setAccessible(true);
            Object slot = find.invoke(c, x, y);
            sb.append(" findSlot=").append(slot == null ? "null" : ((net.minecraft.world.inventory.Slot) slot).index);
        } catch (ReflectiveOperationException e) {
            sb.append(" findSlot=?").append(e.getClass().getSimpleName());
        }
        try {
            var outside = AbstractContainerScreen.class.getDeclaredMethod("hasClickedOutside", double.class, double.class,
                    int.class, int.class, int.class);
            outside.setAccessible(true);
            sb.append(" clickedOutside=").append(outside.invoke(c, x, y, c.getGuiLeft(), c.getGuiTop(), 0));
        } catch (ReflectiveOperationException e) {
            sb.append(" clickedOutside=?").append(e.getClass().getSimpleName());
        }
        var hovered = c.getSlotUnderMouse();
        if (hovered != null) sb.append(" hoveredActive=").append(hovered.isActive());
        sb.append(" slots=").append(c.getMenu().slots.size());
        return sb.toString();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void pressedPreFirst(ScreenEvent.MouseButtonPressed.Pre e) {
        log("press PRE first btn=" + e.getButton() + " at=" + (int) e.getMouseX() + "," + (int) e.getMouseY() +
                " widgetsAtMouse=[" + widgetsAt(e.getScreen(), e.getMouseX(), e.getMouseY()) + "]" +
                hitTest(e.getScreen(), e.getMouseX(), e.getMouseY()), e, true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void pressedPreLast(ScreenEvent.MouseButtonPressed.Pre e) {
        log("press PRE last btn=" + e.getButton(), e, true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void pressedPost(ScreenEvent.MouseButtonPressed.Post e) {
        log("press POST btn=" + e.getButton() + " handled=" + e.getClickResult(), e, false);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void releasedPreFirst(ScreenEvent.MouseButtonReleased.Pre e) {
        log("release PRE first btn=" + e.getButton(), e, true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void releasedPreLast(ScreenEvent.MouseButtonReleased.Pre e) {
        log("release PRE last btn=" + e.getButton(), e, true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void releasedPost(ScreenEvent.MouseButtonReleased.Post e) {
        log("release POST btn=" + e.getButton() + " handled=" + e.getReleaseResult(), e, false);
    }
}
