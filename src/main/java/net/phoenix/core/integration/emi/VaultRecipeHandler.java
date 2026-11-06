package net.phoenix.core.integration.emi;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.integration.gregvaults.mui.VaultMenu;
import net.phoenix.core.integration.gregvaults.mui.VaultSession;

import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.api.stack.EmiStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("all")
public class VaultRecipeHandler<T extends AbstractContainerMenu>
                               implements StandardRecipeHandler<T> {

    private static VaultSession session(AbstractContainerMenu menu) {
        return menu instanceof VaultMenu vault ? vault.session() : null;
    }

    @Override
    public List<Slot> getInputSources(T menu) {
        VaultSession session = session(menu);
        return session == null ? List.of() : new ArrayList<>(session.playerSlots);
    }

    @Override
    public List<Slot> getCraftingSlots(T menu) {
        VaultSession session = session(menu);
        return session == null ? List.of() : new ArrayList<>(session.gridSlots);
    }

    @Override
    public EmiPlayerInventory getInventory(AbstractContainerScreen<T> screen) {
        T menu = screen.getMenu();
        List<EmiStack> stacks = new ArrayList<>();
        VaultSession session = session(menu);
        if (session != null) {
            for (Slot slot : session.playerSlots) {
                if (slot.hasItem()) stacks.add(EmiStack.of(slot.getItem()));
            }
        }
        return new EmiPlayerInventory(stacks);
    }

    @Override
    public boolean canCraft(EmiRecipe recipe, EmiCraftContext<T> context) {
        return session(context.getScreen().getMenu()) != null && supportsRecipe(recipe);
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        return recipe.getCategory().equals(VanillaEmiRecipeCategories.CRAFTING) && recipe.getInputs().size() <= 9;
    }

    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<T> context) {
        if (session(context.getScreen().getMenu()) == null) return false;

        ItemStack[] template = new ItemStack[9];
        Arrays.fill(template, ItemStack.EMPTY);
        var inputs = recipe.getInputs();
        for (int i = 0; i < Math.min(inputs.size(), 9); i++) {
            var stacks = inputs.get(i).getEmiStacks();
            if (!stacks.isEmpty()) {
                ItemStack s = stacks.get(0).getItemStack();
                template[i] = s != null ? s.copy() : ItemStack.EMPTY;
            }
        }

        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new net.phoenix.core.integration.gregvaults.network.CPacketFillCraftingGrid(recipe.getId(), template));
        return true;
    }
}
