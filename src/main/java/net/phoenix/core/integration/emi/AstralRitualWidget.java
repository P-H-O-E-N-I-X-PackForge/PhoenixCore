package net.phoenix.core.integration.emi;

import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.common.data.materials.AstralMaterials;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.integration.recipeviewer.RecipeSlotRole;
import brachy.modularui.integration.recipeviewer.RecipeViewerSlotWidget;
import brachy.modularui.widget.ParentWidget;

public class AstralRitualWidget extends ParentWidget<AstralRitualWidget> {

    public AstralRitualWidget(PhoenixViewer.AstralRitualEntry entry) {
        size(130, 40);
        var result = entry.result();
        ItemStack filament = ChemicalHelper.get(TagPrefix.dust, AstralMaterials.ASTRAL_FILAMENT,
                result.filamentCount());

        child(RecipeViewerSlotWidget.create(ItemStack.class).recipeSlotRole(RecipeSlotRole.INPUT)
                .value(new ItemStack(entry.catalyst())).left(4).top(12));
        child(Text.str("→").asWidget().left(38).top(18));
        child(RecipeViewerSlotWidget.create(ItemStack.class).recipeSlotRole(RecipeSlotRole.OUTPUT)
                .value(filament).left(54).top(12)
                .tooltip(t -> {
                    t.addLine(Text.str("Ritual Pedestal, at night, ring formed, Wand in hand"));
                    t.addLine(Text.str("Fills a carried Astral Thread Cell if you have one,"));
                    t.addLine(Text.str("otherwise drops Astral Filament."));
                }));
        child(Text.of(Component.literal("or +" + result.threadAmount() + " Thread")
                .withStyle(ChatFormatting.LIGHT_PURPLE)).asWidget().left(4).top(30));
    }
}
