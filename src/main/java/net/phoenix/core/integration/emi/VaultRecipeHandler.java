package net.phoenix.core.integration.emi;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.integration.gregvaults.client.screen.AbstractVaultMenu;

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

    @Override
    public List<Slot> getInputSources(T menu) {
        if (!(menu instanceof AbstractVaultMenu m)) return List.of();
        return menu.slots.subList(m.playerSlotsStart, m.craftingSlotsStart);
    }

    @Override
    public List<Slot> getCraftingSlots(T menu) {
        if (!(menu instanceof AbstractVaultMenu m)) return List.of();
        return menu.slots.subList(m.craftingSlotsStart, m.craftingOutputStart);
    }

    @Override
    public EmiPlayerInventory getInventory(AbstractContainerScreen<T> screen) {
        T menu = screen.getMenu();
        List<EmiStack> stacks = new ArrayList<>();
        if (menu instanceof AbstractVaultMenu m && m.clientCache != null) {
            for (ItemStack s : m.clientCache) {
                if (s != null && !s.isEmpty()) stacks.add(EmiStack.of(s));
            }
            for (int i = m.playerSlotsStart; i < m.craftingSlotsStart; i++) {
                Slot slot = menu.slots.get(i);
                if (slot.hasItem()) stacks.add(EmiStack.of(slot.getItem()));
            }
        }
        return new EmiPlayerInventory(stacks);
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        return recipe.getCategory().equals(VanillaEmiRecipeCategories.CRAFTING) && recipe.getInputs().size() <= 9;
    }

    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<T> context) {
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

        net.phoenix.core.integration.gregvaults.network.VaultNetwork.CHANNEL.sendToServer(
                new net.phoenix.core.integration.gregvaults.network.CPacketFillCraftingGrid(recipe.getId(), template));
        return true;
    }
}
