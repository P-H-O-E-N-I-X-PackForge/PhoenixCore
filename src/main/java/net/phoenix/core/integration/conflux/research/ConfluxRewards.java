package net.phoenix.core.integration.conflux.research;

import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.PhoenixCore;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.jetbrains.annotations.Nullable;

/**
 * Hands out the {@code item} unlocks of a research node. The value is an item the way a command writes one, with an
 * optional count: {@code namespace:item}, {@code namespace:item*4}, or with data components,
 * {@code minecraft:diamond_sword[minecraft:enchantments={levels:{"minecraft:sharpness":5}}]*1}. Anything that does not
 * fit in the inventory is dropped at the player's feet.
 */
public final class ConfluxRewards {

    private ConfluxRewards() {}

    /** Plain {@code id*count} only; components need a registry lookup, see {@link #parse(String, HolderLookup.Provider)}. */
    public static ItemStack parse(String value) {
        return parse(value, null);
    }

    /** Parses an {@code item} unlock value into a stack, or EMPTY if it does not name a real item. */
    public static ItemStack parse(String value, @Nullable HolderLookup.Provider registries) {
        String v = value.trim();
        int count = 1;
        int star = v.lastIndexOf('*');
        if (star > v.lastIndexOf(']')) {
            try {
                count = Math.max(1, Integer.parseInt(v.substring(star + 1).trim()));
                v = v.substring(0, star).trim();
            } catch (NumberFormatException ignored) {}
        }

        if (registries != null) {
            try {
                ItemParser.ItemResult result = new ItemParser(registries).parse(new StringReader(v));
                return new ItemStack(result.item(), count, result.components());
            } catch (CommandSyntaxException e) {
                PhoenixCore.LOGGER.warn("[Conflux] Bad item reward '{}': {}", value, e.getMessage());
                return ItemStack.EMPTY;
            }
        }
        ResourceLocation id = ResourceLocation.tryParse(v);
        Item item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        return item == null ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    public static void give(ServerPlayer player, ResearchNode node) {
        for (ResearchUnlock unlock : node.unlocks) {
            if ("archive".equals(unlock.type())) {
                var unlocker = net.phoenix.core.api.ConfluxApi.archiveUnlocker;
                if (unlocker != null) unlocker.accept(player, unlock.value());
                continue;
            }
            if (!"item".equals(unlock.type())) continue;

            ItemStack template = parse(unlock.value(), player.registryAccess());
            if (template.isEmpty()) {
                PhoenixCore.LOGGER.warn("[Conflux] Unknown reward item '{}' on node {}", unlock.value(), node.id);
                continue;
            }

            int remaining = template.getCount();
            while (remaining > 0) {
                int take = Math.min(remaining, template.getMaxStackSize());
                ItemStack stack = template.copyWithCount(take);
                String name = stack.getHoverName().getString();
                if (!player.getInventory().add(stack)) player.drop(stack, false);
                player.displayClientMessage(Component.literal("§6[Axiom] §7Reward: §f" + take + "x " + name),
                        false);
                remaining -= take;
            }
        }
    }
}
