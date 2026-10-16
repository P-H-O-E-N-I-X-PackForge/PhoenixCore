package net.phoenix.core.integration.continuum.common;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.configs.PhoenixConfigs;
import net.phoenix.core.integration.continuum.ContinuumRegistry;
import net.phoenix.core.integration.continuum.item.ContinuumRocketItem;

import org.jetbrains.annotations.Nullable;

public final class RocketRefit {

    private RocketRefit() {}

    public enum Action {
        TIER_UP,
        FIT,
        REPAIR
    }

    public static ItemStack heldRocket(net.minecraft.world.entity.player.Player player) {
        ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (main.getItem() instanceof ContinuumRocketItem) return main;
        ItemStack off = player.getItemInHand(InteractionHand.OFF_HAND);
        return off.getItem() instanceof ContinuumRocketItem ? off : ItemStack.EMPTY;
    }

    public static @Nullable Item refitKit(int toTier) {
        return switch (toTier) {
            case 2 -> ContinuumRegistry.REFIT_MK2.get();
            case 3 -> ContinuumRegistry.REFIT_MK3.get();
            case 4 -> ContinuumRegistry.REFIT_MK4.get();
            default -> null;
        };
    }

    public static @Nullable Item module(String upgrade) {
        return switch (upgrade) {
            case RocketStats.OVERDRIVE -> ContinuumRegistry.MODULE_OVERDRIVE.get();
            case RocketStats.REINFORCED_HULL -> ContinuumRegistry.MODULE_HULL.get();
            case RocketStats.HULL_INTERLOCK -> ContinuumRegistry.MODULE_INTERLOCK.get();
            case RocketStats.STELLAR_SHIELD -> ContinuumRegistry.MODULE_STELLAR_SHIELD.get();
            case RocketStats.SINGULARITY_SHIELD -> ContinuumRegistry.MODULE_SINGULARITY_SHIELD.get();
            default -> null;
        };
    }

    public static int count(net.minecraft.world.entity.player.Player player, Item item) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) if (stack.is(item)) total += stack.getCount();
        for (ItemStack stack : player.getInventory().offhand) if (stack.is(item)) total += stack.getCount();
        return total;
    }

    private static boolean take(ServerPlayer player, Item item) {
        if (player.getAbilities().instabuild) return true;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                stack.shrink(1);
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(item)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    public static @Nullable String apply(ServerPlayer player, Action action, String key) {
        InteractionHand hand = player.getItemInHand(InteractionHand.MAIN_HAND)
                .getItem() instanceof ContinuumRocketItem ?
                        InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        ItemStack rocket = heldRocket(player);
        if (rocket.isEmpty()) return "Hold a Continuum Rocket to refit it.";

        ItemStack original = rocket;
        rocket = original.copy();
        String error = applyTo(player, action, key, rocket);
        if (error == null) {
            player.setItemInHand(hand, rocket);
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();
            player.containerMenu.broadcastChanges();
        }
        return error;
    }

    private static @Nullable String applyTo(ServerPlayer player, Action action, String key, ItemStack rocket) {
        switch (action) {
            case TIER_UP -> {
                int next = RocketStats.tier(rocket) + 1;
                Item kit = refitKit(next);
                if (kit == null) return "That hull cannot be refitted any further.";
                if (!player.getAbilities().instabuild && count(player, kit) <= 0) {
                    return "You need a " + kit.getDescription().getString() + ".";
                }
                take(player, kit);
                RocketStats.setTier(rocket, next);
                done(player, "Hull refitted to " + RocketStats.tierName(next) + ".");
            }
            case FIT -> {
                Item module = module(key);
                if (module == null) return "Unknown module.";
                int tier = RocketStats.tier(rocket);
                int cap = RocketStats.upgradeCap(tier, key);
                int current = RocketStats.level(rocket, key);
                if (cap <= 0) return "This hull is too light to carry that module. Refit it to a higher tier.";
                if (current >= cap) return "This hull cannot carry any more of that module at " +
                        RocketStats.tierName(tier) + ".";
                if (!player.getAbilities().instabuild && count(player, module) <= 0) {
                    return "You need a " + module.getDescription().getString() + ".";
                }
                take(player, module);
                RocketStats.setLevel(rocket, key, current + 1);
                done(player, module.getDescription().getString() + " fitted (level " + (current + 1) + ").");
            }
            case REPAIR -> {
                float wear = RocketStats.wear(rocket);
                if (wear <= 0.0f) return "The rocket is in perfect condition.";
                Item kit = ContinuumRegistry.REPAIR_KIT.get();
                if (!player.getAbilities().instabuild && count(player, kit) <= 0) return "You need a repair kit.";
                take(player, kit);
                RocketStats.setWear(rocket, wear - (float) PhoenixConfigs.INSTANCE.continuum.repairKitWear);
                done(player, "Rocket wear is now " + Math.round(RocketStats.wear(rocket) * 100.0f) + "%.");
            }
        }
        return null;
    }

    private static void done(ServerPlayer player, String message) {
        player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.6f, 1.4f);
        player.displayClientMessage(Component.literal(message).withStyle(ChatFormatting.GREEN), true);
    }
}
