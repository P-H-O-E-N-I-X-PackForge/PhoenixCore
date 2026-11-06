package net.phoenix.core.integration.emi;

import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.phoenix.core.PhoenixCore;
import net.phoenix.core.common.data.recipe.pft.PftLapidaryRecipes;
import net.phoenix.core.common.machine.PftMachines;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

import java.util.ArrayList;
import java.util.List;

public final class LapidaryEmi {

    private LapidaryEmi() {}

    private static EmiStack lapidary() {
        for (var machine : PftMachines.LAPIDARY) {
            if (machine != null) return EmiStack.of(machine.asStack());
        }
        return EmiStack.EMPTY;
    }

    public static final EmiRecipeCategory CUTTING = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID, "lapidary_cuts"), lapidary()) {

        @Override
        public Component getName() {
            return Component.literal("Lapidary: gem cuts");
        }
    };

    public static void register(EmiRegistry registry) {
        registry.addCategory(CUTTING);
        for (var machine : PftMachines.LAPIDARY) {
            if (machine != null) registry.addWorkstation(CUTTING, EmiStack.of(machine.asStack()));
        }
        TagPrefix[] prefixes = PftLapidaryRecipes.cutPrefixes();
        for (Material gem : PftLapidaryRecipes.gems()) {
            ItemStack input = ChemicalHelper.get(TagPrefix.gem.get(), gem, 1);
            if (input.isEmpty()) continue;
            List<EmiStack> cuts = new ArrayList<>();
            for (TagPrefix prefix : prefixes) {
                ItemStack out = ChemicalHelper.get(prefix, gem, 1);
                cuts.add(out.isEmpty() ? EmiStack.EMPTY : EmiStack.of(out));
            }
            if (cuts.stream().allMatch(EmiStack::isEmpty)) continue;
            registry.addRecipe(new GemRecipe(gem, input, cuts));
        }
    }

    private static final class GemRecipe extends BasicEmiRecipe {

        private final EmiStack gem;
        private final List<EmiStack> cuts;
        private final String name;
        private final int accent;

        GemRecipe(Material material, ItemStack gem, List<EmiStack> cuts) {
            super(CUTTING, ResourceLocation.fromNamespaceAndPath(PhoenixCore.MOD_ID,
                    "/lapidary_cuts/" + material.getName()), 176, 78);
            this.gem = EmiStack.of(gem);
            this.cuts = cuts;
            this.name = material.getLocalizedName().getString();

            int rgb = material.getMaterialRGB();
            int r = Math.min(255, ((rgb >> 16) & 255) + 40), g = Math.min(255, ((rgb >> 8) & 255) + 40),
                    b = Math.min(255, (rgb & 255) + 40);
            this.accent = 0xFF000000 | (r << 16) | (g << 8) | b;
            this.inputs = List.of(this.gem, EmiStack.of(Fluids.WATER, 100));
            List<EmiStack> real = new ArrayList<>();
            for (EmiStack cut : cuts) if (!cut.isEmpty()) real.add(cut);
            this.outputs = real;
            this.catalysts = List.of(lapidary());
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            EmiPage.frame(widgets, 176, height, accent);
            EmiPage.title(widgets, name + " cuts", 0, 1, 176, 0xFFFFFFFF);

            widgets.addSlot(gem, 0, 16).recipeContext(this);
            widgets.addSlot(EmiStack.of(Fluids.WATER, 100), 0, 38).recipeContext(this);
            widgets.addTooltipText(List.of(Component.literal("100 mB of water cools the saw")), 0, 38, 18, 18);
            EmiPage.row(widgets, "water", 0, 60, 26, EmiPage.DIM, null);

            EmiPage.section(widgets, "Circuit picks the cut", 28, 14, 148, accent);
            for (int i = 0; i < cuts.size(); i++) {
                int x = 28 + (i % 4) * 36;
                int y = 28 + (i / 4) * 24;
                String cut = PftLapidaryRecipes.CUTS[i];
                String cutName = Character.toUpperCase(cut.charAt(0)) + cut.substring(1) + " cut";
                EmiPage.row(widgets, String.valueOf(i + 1), x + 1, y + 5, 8, EmiPage.WARM, null);
                if (!cuts.get(i).isEmpty()) widgets.addSlot(cuts.get(i), x + 9, y).recipeContext(this);
                widgets.addTooltipText(List.of(Component.literal("Circuit " + (i + 1)), Component.literal(cutName)), x,
                        y, 30, 18);
            }
        }
    }
}
