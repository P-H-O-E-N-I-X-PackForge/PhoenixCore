package net.phoenix.core.integration.emi;

import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.data.materials.AstralMaterials;
import net.phoenix.core.integration.astral.ritual.AstralRitualPedestalBlockEntity.RitualResult;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

import java.util.List;

public class AstralRitualEmiRecipe extends BasicEmiRecipe {

    private final RitualResult result;

    public AstralRitualEmiRecipe(Item catalyst, RitualResult result) {
        super(PhoenixEmiPlugin.ASTRAL_RITUAL, new ResourceLocation(PhoenixCore.MOD_ID,
                "astral_ritual/" + catalyst.builtInRegistryHolder().key().location().getPath()), 130, 40);
        this.result = result;
        this.inputs = List.of(EmiStack.of(new ItemStack(catalyst)));
        this.outputs = List
                .of(EmiStack.of(
                        ChemicalHelper.get(TagPrefix.dust, AstralMaterials.ASTRAL_FILAMENT, result.filamentCount())));
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(inputs.get(0), 4, 12).drawBack(true);
        widgets.addText(Component.literal("→"), 38, 18, 0xFFFFFF, false);
        widgets.addSlot(outputs.get(0), 54, 12).drawBack(true).recipeContext(this);
        widgets.addText(
                Component.literal("or +" + result.threadAmount() + " Thread").withStyle(ChatFormatting.LIGHT_PURPLE),
                4, 30, 0x9C6CE0, false);

        widgets.addTooltipText(
                List.of(Component.literal("Ritual Pedestal, at night, ring formed, Wand in hand"),
                        Component.literal("Fills a carried Astral Thread Cell if you have one,"),
                        Component.literal("otherwise drops Astral Filament.")),
                54, 12, 16, 16);
    }
}
