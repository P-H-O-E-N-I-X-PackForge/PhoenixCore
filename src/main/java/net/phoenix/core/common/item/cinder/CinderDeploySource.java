package net.phoenix.core.common.item.cinder;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import net.phoenix.core.integration.ae2.CinderAtlasWirelessLink;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import org.jetbrains.annotations.Nullable;

/**
 * The one place that knows "given whatever's actually held, which {@link CompoundTag} has the target
 * config to preview/build, and where its materials come from" - a {@link CinderCoreItem} uses its own
 * root tag and its own stocked-materials capability; a {@link CinderAtlasItem} uses its active
 * loadout's active slot (see {@link CinderAtlasData}) and a bound AE2 network (see
 * {@link CinderAtlasWirelessLink}). Everything downstream (the ghost-preview pipeline, the commit
 * packet) goes through this instead of checking item types itself, so a third deployable item type
 * would only need a branch added here, not scattered {@code instanceof} checks through preview/commit
 * code.
 * <p>
 * The materials methods here are server-only (they need a real {@link ServerLevel} to resolve an AE2
 * grid, which has no client-side meaning at all) - client-side code (the preview ghost's green/red
 * tint) still just calls {@code CinderSchemaData.hasSufficientMaterials} directly against the held
 * stack's own capability, which naturally and correctly reads as "insufficient" for an Atlas until the
 * server confirms otherwise on an actual commit attempt. Not a bug to route around - genuinely unknown
 * client-side, same as it would be for any other AE2-network-backed check.
 */
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

    /** Server-only real materials check - a Cinder Core against its own stocked inventory, an Atlas
     *  against its linked AE2 network's live stock (also gated by real wireless range - see
     *  {@code CinderAtlasWirelessLink#isInRange}). */
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

    /** Caller must already have confirmed {@link #hasSufficientMaterials} - this doesn't re-check. */
    public static void consumeMaterials(ItemStack stack, ServerLevel level, Reference2IntMap<Block> required,
                                        Player player) {
        if (stack.getItem() instanceof CinderCoreItem) {
            CinderSchemaData.consumeMaterials(stack, required);
        } else if (stack.getItem() instanceof CinderAtlasItem) {
            CinderAtlasWirelessLink.extractMaterials(stack, level, required, player);
        }
    }
}
