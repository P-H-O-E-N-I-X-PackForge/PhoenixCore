package net.phoenix.core.integration.continuum.pdim;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.continuum.data.ContinuumBody;

public final class PdimCosts {

    private PdimCosts() {}

    public record Cost(ResourceLocation item, int count) {

        public boolean free() {
            return count <= 0 || BuiltInRegistries.ITEM.get(item) == Items.AIR;
        }

        public Component describe() {
            Item resolved = BuiltInRegistries.ITEM.get(item);
            return Component.literal(count + " x ").append(resolved.getDescription());
        }
    }

    public static Cost costFor(ContinuumBody body) {
        ContinuumBody.PdimCost override = body.pdimCost();
        if (override != null) return new Cost(override.item(), override.count());

        var cfg = PhoenixConfigs.INSTANCE.continuum;
        ResourceLocation id = ResourceLocation.tryParse(cfg.pdimCostItem);
        return new Cost(id != null ? id : new ResourceLocation("minecraft", "air"), cfg.pdimCostCount);
    }

    public static boolean charges(boolean alreadyHasDimension) {
        var mode = PhoenixConfigs.INSTANCE.continuum.pdimChargeMode;
        return mode == PhoenixConfigs.ContinuumConfigs.PdimChargeMode.EVERY_VISIT || !alreadyHasDimension;
    }

    public static int count(Player player, Cost cost) {
        Item item = BuiltInRegistries.ITEM.get(cost.item());
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) total += stack.getCount();
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    public static void take(Player player, Cost cost) {
        Item item = BuiltInRegistries.ITEM.get(cost.item());
        int remaining = cost.count();
        for (var list : java.util.List.of(player.getInventory().items, player.getInventory().offhand)) {
            for (ItemStack stack : list) {
                if (remaining <= 0) return;
                if (!stack.is(item)) continue;
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
    }
}
