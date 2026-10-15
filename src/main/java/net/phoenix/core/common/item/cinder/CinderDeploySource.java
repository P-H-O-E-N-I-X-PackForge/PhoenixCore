package net.phoenix.core.common.item.cinder;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.phoenix.core.integration.ae2.CinderAtlasWirelessLink;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import org.jetbrains.annotations.Nullable;

public final class CinderDeploySource {

    private CinderDeploySource() {}

    public static @Nullable CompoundTag resolveDeployTag(ItemStack stack) {
        if (stack.getItem() instanceof CinderCoreItem) {
            return stack.getOrCreateTag();
        }
        if (stack.getItem() instanceof CinderAtlasItem) {
            int loadout = CinderAtlasData.getActiveLoadout(stack);
            int slot = CinderAtlasData.getActiveSlot(stack);
            return CinderAtlasData.getOrCreateSlotTag(stack, loadout, slot);
        }
        return null;
    }

    public static boolean hasConfiguredTarget(ItemStack stack) {
        CompoundTag tag = resolveDeployTag(stack);
        return tag != null && CinderSchemaData.getTargetId(tag) != null;
    }

    public static boolean hasSufficientMaterials(ItemStack stack, ServerLevel level, Player player,
                                                 Reference2IntMap<Block> required) {
        if (stack.getItem() instanceof CinderCoreItem) {
            return CinderSchemaData.hasSufficientMaterials(stack, required);
        }
        if (stack.getItem() instanceof CinderAtlasItem) {
            return CinderAtlasWirelessLink.hasSufficientMaterials(stack, level, player, required);
        }
        return false;
    }

    public static void consumeMaterials(ItemStack stack, ServerLevel level, Reference2IntMap<Block> required,
                                        Player player) {
        if (stack.getItem() instanceof CinderCoreItem) {
            CinderSchemaData.consumeMaterials(stack, required);
        } else if (stack.getItem() instanceof CinderAtlasItem) {
            CinderAtlasWirelessLink.extractMaterials(stack, level, required, player);
        }
    }
}
