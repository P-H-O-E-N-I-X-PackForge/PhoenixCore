package net.phoenix.core.integration.emi;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.phoenix.core.integration.gregvaults.client.screen.AbstractVaultMenu;
import net.phoenix.core.integration.gregvaults.network.CPacketFillCraftingGrid;
import net.phoenix.core.integration.gregvaults.network.VaultNetwork;

import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IUniversalRecipeTransferHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("all")
public class VaultJeiTransferHandler<T extends AbstractVaultMenu>
                                    implements IUniversalRecipeTransferHandler<T> {

    private final Class<T> menuClass;
    private final MenuType<T> menuType;

    public VaultJeiTransferHandler(Class<T> menuClass, MenuType<T> menuType) {
        this.menuClass = menuClass;
        this.menuType = menuType;
    }

    @Override
    public Class<? extends T> getContainerClass() {
        return menuClass;
    }

    @Override
    public Optional<MenuType<T>> getMenuType() {
        return Optional.of(menuType);
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(T menu, Object recipe,
                                                         IRecipeSlotsView recipeSlots, Player player,
                                                         boolean maxTransfer, boolean doTransfer) {
        if (!doTransfer) return null;

        ItemStack[] template = new ItemStack[9];
        Arrays.fill(template, ItemStack.EMPTY);

        List<? extends mezz.jei.api.gui.ingredient.IRecipeSlotView> inputs = recipeSlots
                .getSlotViews(RecipeIngredientRole.INPUT);

        for (int i = 0; i < Math.min(inputs.size(), 9); i++) {
            final int idx = i;
            inputs.get(i).getItemStacks()
                    .findFirst()
                    .ifPresent(stack -> template[idx] = stack.copy());
        }

        ResourceLocation recipeId = recipe instanceof Recipe<?> r ? r.getId() : null;
        VaultNetwork.CHANNEL.sendToServer(new CPacketFillCraftingGrid(recipeId, template));
        return null;
    }
}
