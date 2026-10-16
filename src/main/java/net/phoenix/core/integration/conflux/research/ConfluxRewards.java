package net.phoenix.core.integration.conflux.research;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.PhoenixCore;

/**
 * Hands out the {@code item} unlocks of a research node. The value is {@code namespace:item} or
 * {@code namespace:item*count}; anything that does not fit in the inventory is dropped at the player's feet.
 */
public final class ConfluxRewards {

    private ConfluxRewards() {}

    public static void give(ServerPlayer player, ResearchNode node) {
        for (ResearchUnlock unlock : node.unlocks) {
            if (!"item".equals(unlock.type())) continue;

            String value = unlock.value();
            int count = 1;
            int star = value.indexOf('*');
            if (star >= 0) {
                try {
                    count = Math.max(1, Integer.parseInt(value.substring(star + 1).trim()));
                } catch (NumberFormatException e) {
                    PhoenixCore.LOGGER.warn("[Conflux] Bad reward count in '{}' on node {}", value, node.id);
                }
                value = value.substring(0, star);
            }
            ResourceLocation id = ResourceLocation.tryParse(value.trim());
            Item item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
            if (item == null) {
                PhoenixCore.LOGGER.warn("[Conflux] Unknown reward item '{}' on node {}", value, node.id);
                continue;
            }

            while (count > 0) {
                int take = Math.min(count, item.getDefaultMaxStackSize());
                ItemStack stack = new ItemStack(item, take);
                String name = stack.getHoverName().getString();
                if (!player.getInventory().add(stack)) player.drop(stack, false);
                player.displayClientMessage(Component.literal("§6[Axiom] §7Reward: §f" + take + "x " + name), false);
                count -= take;
            }
        }
    }
}
