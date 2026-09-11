package net.phoenix.core.common.item.cinder;

import com.gregtechceu.gtceu.api.mui.MultiblockSchemaInfo;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import net.minecraftforge.common.capabilities.ForgeCapabilities;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;

/**
 * A normal 3x3 crafting recipe (works in a plain crafting table, no multiblock needed) that fills a
 * Cinder Core by hand - an alternative to the Cinder Forge multiblock
 * ({@link net.phoenix.core.integration.ae2.CinderForgeHatchPartMachine}) for small structures. Same
 * "special"/dynamic-matching shape as vanilla's own {@code ArmorDyeRecipe}/{@code RepairItemRecipe}:
 * no fixed ingredient list in JSON, {@link #matches} does the real work by reading whichever Core is
 * in the grid and checking the rest of the grid against its specific schema requirement.
 * <p>
 * Hard ceiling, not a design choice: a vanilla crafting grid is physically 3x3 = 9 slots, one of
 * which holds the Core - so this can never satisfy a schema needing more than <b>8 distinct block
 * types</b>, no matter how the matching logic is written (AE2 Crafting Patterns are tied to that same
 * grid). The Cinder Forge multiblock has no such ceiling (its hatch pulls straight from the ME
 * network, not a 9-slot grid) - this recipe is only ever the option for small multiblocks, or players
 * without an AE2 network at all.
 */
public class CinderCoreForgeRecipe extends CustomRecipe {

    public CinderCoreForgeRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        ItemStack template = ItemStack.EMPTY;
        Reference2IntMap<Block> available = new Reference2IntOpenHashMap<>();

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof CinderCoreItem) {
                if (!template.isEmpty()) return false;
                if (CinderSchemaData.getTargetId(stack) == null) return false;
                // Same one-shot-fill rule as the Forge - only an empty Core is a valid input, never a
                // partially- or already-stocked one.
                if (!CinderSchemaData.tallyStocked(stack).isEmpty()) return false;
                template = stack;
            } else if (stack.getItem() instanceof BlockItem blockItem) {
                available.merge(blockItem.getBlock(), stack.getCount(), Integer::sum);
            } else {
                return false;
            }
        }
        if (template.isEmpty()) return false;

        MultiblockSchemaInfo schemaInfo = CinderSchemaData.resolveSchema(template);
        if (schemaInfo == null) return false;
        var required = schemaInfo.getBlockCounts();
        if (required.isEmpty()) return false;

        for (var entry : required.reference2IntEntrySet()) {
            if (available.getOrDefault(entry.getKey(), 0) < entry.getIntValue()) return false;
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        ItemStack template = ItemStack.EMPTY;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.getItem() instanceof CinderCoreItem) {
                template = stack;
                break;
            }
        }
        if (template.isEmpty()) return ItemStack.EMPTY;

        MultiblockSchemaInfo schemaInfo = CinderSchemaData.resolveSchema(template);
        if (schemaInfo == null) return ItemStack.EMPTY;
        var required = schemaInfo.getBlockCounts();

        ItemStack output = template.copy();
        output.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            int slot = 0;
            for (var entry : required.reference2IntEntrySet()) {
                handler.insertItem(slot++, new ItemStack(entry.getKey().asItem(), entry.getIntValue()), false);
            }
        });
        return output;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return new ItemStack(CinderItems.CINDER_CORE.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CinderRecipes.CINDER_FORGE_CRAFTING.get();
    }
}
