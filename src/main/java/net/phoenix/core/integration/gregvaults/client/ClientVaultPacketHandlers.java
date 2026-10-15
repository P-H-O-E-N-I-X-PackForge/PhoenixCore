package net.phoenix.core.integration.gregvaults.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.phoenix.core.integration.gregvaults.client.screen.AbstractVaultMenu;
import net.phoenix.core.integration.gregvaults.network.SPacketVaultContents;
import net.phoenix.core.integration.gregvaults.network.SPacketVaultDelta;

@OnlyIn(Dist.CLIENT)
public final class ClientVaultPacketHandlers {

    private ClientVaultPacketHandlers() {}

    public static void handleContents(SPacketVaultContents packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        var menu = mc.player.containerMenu;
        if (menu.containerId != packet.getContainerId()) return;
        if (menu instanceof AbstractVaultMenu m) m.setClientCache(packet.getStacks());
    }

    public static void handleDelta(SPacketVaultDelta packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        var menu = mc.player.containerMenu;
        if (menu.containerId != packet.getContainerId()) return;
        if (menu instanceof AbstractVaultMenu m) {
            applyDelta(packet, m.clientCache);
            m.applyDeltaUpdate();
        }
    }

    private static void applyDelta(SPacketVaultDelta packet, ItemStack[] cache) {
        if (cache == null) return;
        for (var e : packet.getEntries()) {
            int slot = e.slot();
            if (slot < 0 || slot >= cache.length) continue;
            switch (e.type()) {
                case SPacketVaultDelta.TYPE_FULL -> cache[slot] = e.stack();
                case SPacketVaultDelta.TYPE_REMOVED -> cache[slot] = ItemStack.EMPTY;
                case SPacketVaultDelta.TYPE_COUNT -> {
                    ItemStack existing = cache[slot];
                    if (existing != null && !existing.isEmpty()) {
                        cache[slot] = existing.copyWithCount(e.count());
                    }
                }
            }
        }
    }
}
