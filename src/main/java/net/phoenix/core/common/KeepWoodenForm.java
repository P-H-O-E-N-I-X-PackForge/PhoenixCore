package net.phoenix.core.common;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.phoenix.core.PhoenixCore;

@EventBusSubscriber(modid = PhoenixCore.MOD_ID)
public final class KeepWoodenForm {

    private static final ResourceLocation FORM = ResourceLocation.parse("gtceu:brick_wooden_form");
    private static final ResourceLocation CLAY = ResourceLocation.parse("gtceu:compressed_coke_clay");

    private KeepWoodenForm() {}

    @SubscribeEvent
    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!BuiltInRegistries.ITEM.getKey(event.getCrafting().getItem()).equals(CLAY)) return;
        Item form = BuiltInRegistries.ITEM.get(FORM);
        if (form == null || form == net.minecraft.world.item.Items.AIR) return;

        ItemStack back = new ItemStack(form);
        if (!event.getEntity().getInventory().add(back)) event.getEntity().drop(back, false);
    }
}
