package net.phoenix.core.common.data;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.data.recipe.GTCraftingComponents;
import com.gregtechceu.gtceu.data.recipe.event.CraftingComponentModificationEvent;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.phoenix.core.PhoenixCore;

@EventBusSubscriber(modid = PhoenixCore.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class PftCraftingComponents {

    private PftCraftingComponents() {}

    private static final String[][] METALS = {
            { "aurum_steel", String.valueOf(GTValues.LV) },
            { "aluminfrost", String.valueOf(GTValues.MV) },
            { "frost_reinforced_stained_steel", String.valueOf(GTValues.HV) },
            { "source_imbued_titanium", String.valueOf(GTValues.EV) },
            { "void_touched_tungsten_steel", String.valueOf(GTValues.IV) },
            { "resonant_rhodium_alloy", String.valueOf(GTValues.LuV) },
            { "advanced_quin_naquadian_alloy", String.valueOf(GTValues.ZPM) },
    };

    @SubscribeEvent
    public static void onModify(CraftingComponentModificationEvent event) {
        for (String[] metal : METALS) {
            int tier = Integer.parseInt(metal[1]);
            ItemStack plate = item(metal[0] + "_plate");
            ItemStack rotor = item(metal[0] + "_rotor");
            if (!plate.isEmpty()) GTCraftingComponents.PLATE.add(tier, plate);
            if (!rotor.isEmpty()) GTCraftingComponents.ROTOR.add(tier, rotor);
        }
    }

    private static ItemStack item(String path) {
        ResourceLocation id = PhoenixCore.id(path);
        if (!BuiltInRegistries.ITEM.containsKey(id)) {
            PhoenixCore.LOGGER.warn("[PFT] crafting component: no item {}", id);
            return ItemStack.EMPTY;
        }
        return new ItemStack(BuiltInRegistries.ITEM.get(id));
    }
}
