package net.phoenix.core.integration.emi;

import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.gui.ViewerEntry;
import com.gregtechceu.gtceu.api.recipe.gui.viewer.GTViewerCategories;
import com.gregtechceu.gtceu.api.recipe.gui.viewer.GTViewerCategory;
import com.gregtechceu.gtceu.api.recipe.gui.viewer.GTViewerVisibility;
import com.gregtechceu.gtceu.integration.recipeviewer.widgets.MultiblockPreviewExtensions;
import com.gregtechceu.gtceu.integration.recipeviewer.widgets.MultiblockPreviewExtensions.Slot;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.data.materials.AstralMaterials;
import net.phoenix.core.common.machine.multiblock.electric.AetherCrucibleMachines;
import net.phoenix.core.integration.astral.AstralBlocks;
import net.phoenix.core.integration.astral.ritual.AstralRitualPedestalBlockEntity;
import net.phoenix.core.integration.astral.ritual.AstralRitualPedestalBlockEntity.RitualResult;

import brachy.modularui.api.drawable.Text;

import java.util.List;

public final class PhoenixViewer {

    public record AstralRitualEntry(Item catalyst, RitualResult result) {}

    private PhoenixViewer() {}

    public static void register() {
        GTViewerCategories.register(GTViewerCategory
                .builder(ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, "astral_ritual"),
                        AstralRitualEntry.class)
                .icon(() -> new ItemStack(AstralBlocks.ASTRAL_RITUAL_PEDESTAL.get()))
                .title(Component.literal("Astral Ritual"))
                .entries(() -> AstralRitualPedestalBlockEntity.RITUAL_TABLE.entrySet().stream()
                        .map(e -> new AstralRitualEntry(e.getKey(), e.getValue())).toList())
                .id(e -> ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID,
                        "astral_ritual/" + e.catalyst().builtInRegistryHolder().key().location().getPath()))
                .ui(AstralRitualWidget::new)
                .inputs(e -> List.of(ViewerEntry.of(e.catalyst())))
                .outputs(e -> List.of(ViewerEntry.of(ChemicalHelper.get(TagPrefix.dust,
                        AstralMaterials.ASTRAL_FILAMENT, e.result().filamentCount()))))
                .workstation(() -> new ItemStack(AstralBlocks.ASTRAL_RITUAL_PEDESTAL.get()))
                .maxSize(130, 60)
                .build());

        MultiblockPreviewExtensions.register(Slot.BELOW,
                def -> AetherCrucibleMachines.AETHER_CRUCIBLE != null &&
                        def == AetherCrucibleMachines.AETHER_CRUCIBLE.get(),
                preview -> Text.str("Makes ores for the discipline you committed to").asWidget());

        GTViewerVisibility.addRule((category, id, entry) -> !(entry instanceof GTRecipe recipe) ||
                !DisciplineRecipeVisibility.hidden(recipe));
    }
}
